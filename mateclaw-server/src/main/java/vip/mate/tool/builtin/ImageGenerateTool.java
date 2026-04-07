package vip.mate.tool.builtin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import vip.mate.system.model.SystemSettingsDTO;
import vip.mate.system.service.SystemSettingService;
import vip.mate.task.AsyncTaskService;
import vip.mate.task.model.AsyncTaskInfo;
import vip.mate.tool.image.*;

import java.util.List;
import java.util.StringJoiner;

/**
 * 图片生成工具 — Agent 可调用的 @Tool，提交图片生成任务
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageGenerateTool {

    private final ImageGenerationService imageGenerationService;
    private final ImageProviderRegistry providerRegistry;
    private final SystemSettingService systemSettingService;
    private final AsyncTaskService asyncTaskService;

    @Tool(description = "图片生成工具，支持以下 action：\n"
            + "- generate（默认）：生成图片。提供 prompt 描述图片内容，可选 size/aspectRatio/model/count\n"
            + "- list：列出所有可用的图片 Provider 及其支持的模型和能力\n"
            + "- status：查看当前会话中正在进行的图片生成任务状态\n"
            + "部分 Provider 是异步生成（30秒-2分钟），完成后自动显示在对话中。")
    public String image_generate(
            @ToolParam(description = "操作类型: generate（生成图片）、list（列出可用 Provider）、status（查看任务状态），默认 generate", required = false) String action,
            @ToolParam(description = "图片内容描述，尽量详细（generate 时必填）", required = false) String prompt,
            @ToolParam(description = "图片尺寸: 1024x1024 / 1024x1792 / 1792x1024", required = false) String size,
            @ToolParam(description = "画面比例: 1:1 / 16:9 / 9:16，默认 1:1", required = false) String aspectRatio,
            @ToolParam(description = "生成数量（1-4），默认 1", required = false) Integer count,
            @ToolParam(description = "指定模型名称（可选）", required = false) String model,
            @ToolParam(description = "查询指定任务 ID 的状态（status 模式时使用）", required = false) String taskId
    ) {
        String normalizedAction = (action == null || action.isBlank()) ? "generate" : action.trim().toLowerCase();

        return switch (normalizedAction) {
            case "list" -> handleListAction();
            case "status" -> handleStatusAction(taskId);
            default -> handleGenerateAction(prompt, size, aspectRatio, count, model);
        };
    }

    // ==================== action=list ====================

    private String handleListAction() {
        SystemSettingsDTO config = systemSettingService.getAllSettings();
        List<ImageGenerationProvider> providers = providerRegistry.allSorted();

        if (providers.isEmpty()) {
            return "当前没有注册的图片生成 Provider。";
        }

        StringJoiner sb = new StringJoiner("\n\n");
        sb.add("## 可用的图片生成 Provider\n");

        for (ImageGenerationProvider p : providers) {
            boolean available = p.isAvailable(config);
            ImageProviderCapabilities caps = p.detailedCapabilities();

            StringJoiner entry = new StringJoiner("\n");
            entry.add("### " + p.label() + " (" + p.id() + ") " + (available ? "[已配置]" : "[未配置]"));

            if (caps != null) {
                if (caps.getModels() != null && !caps.getModels().isEmpty()) {
                    entry.add("- 模型: " + String.join(", ", caps.getModels()));
                }
                entry.add("- 支持尺寸: " + String.join(", ", caps.getSupportedSizes()));
                entry.add("- 最大数量: " + caps.getMaxCount());
            }
            sb.add(entry.toString());
        }
        return sb.toString();
    }

    // ==================== action=status ====================

    private String handleStatusAction(String taskId) {
        String conversationId = ToolExecutionContext.conversationId();

        if (taskId != null && !taskId.isBlank()) {
            AsyncTaskInfo info = imageGenerationService.checkTaskStatus(taskId);
            if (info == null) {
                return "未找到任务 ID: " + taskId;
            }
            return formatTaskStatus(info);
        }

        if (conversationId == null) {
            return "无法获取当前会话信息";
        }
        List<AsyncTaskInfo> activeTasks = asyncTaskService.listActiveTasks(conversationId);
        List<AsyncTaskInfo> imageTasks = activeTasks.stream()
                .filter(t -> "image_generation".equals(t.getTaskType()))
                .toList();
        if (imageTasks.isEmpty()) {
            return "当前会话没有进行中的图片生成任务。";
        }

        StringJoiner sb = new StringJoiner("\n");
        sb.add("当前会话有 " + imageTasks.size() + " 个进行中的任务：");
        for (AsyncTaskInfo task : imageTasks) {
            sb.add("- 任务 " + task.getTaskId() + ": " + formatTaskStatus(task));
        }
        return sb.toString();
    }

    // ==================== action=generate ====================

    private String handleGenerateAction(String prompt, String size, String aspectRatio,
                                         Integer count, String model) {
        String conversationId = ToolExecutionContext.conversationId();
        String username = ToolExecutionContext.username();

        if (conversationId == null || conversationId.isBlank()) {
            return "错误：无法获取当前会话信息，请重试";
        }

        if (prompt == null || prompt.isBlank()) {
            return "错误：prompt 为必填参数，请描述你想要生成的图片内容";
        }

        ImageGenerationRequest request = ImageGenerationRequest.builder()
                .prompt(prompt)
                .size(size)
                .aspectRatio(aspectRatio != null ? aspectRatio : "1:1")
                .count(count != null ? count : 1)
                .model(model)
                .build();

        ImageGenerationResult result = imageGenerationService.submitGeneration(
                request, conversationId, username != null ? username : "system");

        if (result.isCompleted()) {
            // 同步模式：图片已生成
            return result.getMessage();
        } else if (result.isSubmitted()) {
            // 异步模式：已提交
            return result.getMessage();
        } else {
            return "图片生成失败：" + result.getMessage();
        }
    }

    // ==================== 辅助方法 ====================

    private String formatTaskStatus(AsyncTaskInfo info) {
        return switch (info.getStatus()) {
            case "pending" -> "排队中，请稍候...";
            case "running" -> {
                String progressStr = info.getProgress() != null && info.getProgress() > 0
                        ? "（进度: " + info.getProgress() + "%）" : "";
                yield "生成中" + progressStr + "（" + info.getProviderName() + "）";
            }
            case "succeeded" -> "已完成，图片已显示在对话中";
            case "failed" -> "失败：" + (info.getErrorMessage() != null ? info.getErrorMessage() : "未知错误");
            default -> "状态: " + info.getStatus();
        };
    }
}
