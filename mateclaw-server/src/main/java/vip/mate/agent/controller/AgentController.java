package vip.mate.agent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vip.mate.agent.AgentService;
import vip.mate.agent.AgentState;
import vip.mate.agent.model.AgentEntity;
import vip.mate.audit.service.AuditEventService;
import vip.mate.common.result.R;
import vip.mate.workspace.core.annotation.RequireWorkspaceRole;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Agent 管理接口
 *
 * @author MateClaw Team
 */
@Tag(name = "Agent管理")
@Slf4j
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final AuditEventService auditEventService;
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    @Operation(summary = "获取Agent列表")
    @GetMapping
    @RequireWorkspaceRole("viewer")
    public R<List<AgentEntity>> list(
            @RequestHeader(value = "X-Workspace-Id", required = false) Long workspaceId) {
        if (workspaceId != null) {
            return R.ok(agentService.listAgentsByWorkspace(workspaceId));
        }
        return R.ok(agentService.listAgents());
    }

    @Operation(summary = "获取Agent详情")
    @GetMapping("/{id}")
    @RequireWorkspaceRole("viewer")
    public R<AgentEntity> get(@PathVariable Long id) {
        return R.ok(agentService.getAgent(id));
    }

    @Operation(summary = "创建Agent")
    @PostMapping
    @RequireWorkspaceRole("member")
    public R<AgentEntity> create(
            @RequestHeader(value = "X-Workspace-Id", required = false) Long workspaceId,
            @RequestBody AgentEntity agent) {
        if (workspaceId != null) {
            agent.setWorkspaceId(workspaceId);
        }
        AgentEntity created = agentService.createAgent(agent);
        auditEventService.record("CREATE", "AGENT", String.valueOf(created.getId()), created.getName(), null);
        return R.ok(created);
    }

    @Operation(summary = "更新Agent")
    @PutMapping("/{id}")
    @RequireWorkspaceRole("member")
    public R<AgentEntity> update(@PathVariable Long id, @RequestBody AgentEntity agent) {
        agent.setId(id);
        AgentEntity updated = agentService.updateAgent(agent);
        auditEventService.record("UPDATE", "AGENT", String.valueOf(id), updated.getName(), null);
        return R.ok(updated);
    }

    @Operation(summary = "删除Agent")
    @DeleteMapping("/{id}")
    @RequireWorkspaceRole("admin")
    public R<Void> delete(@PathVariable Long id) {
        AgentEntity agent = agentService.getAgent(id);
        agentService.deleteAgent(id);
        auditEventService.record("DELETE", "AGENT", String.valueOf(id), agent != null ? agent.getName() : null, null);
        return R.ok();
    }

    @Operation(summary = "流式对话（SSE）")
    @GetMapping("/{id}/chat/stream")
    public SseEmitter chatStream(
            @PathVariable Long id,
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String conversationId) {

        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        sseExecutor.execute(() -> {
            try {
                agentService.chatStream(id, message, conversationId)
                        .doOnNext(chunk -> {
                            try {
                                emitter.send(SseEmitter.event().name("message").data(chunk));
                            } catch (IOException e) {
                                log.warn("SSE send error: {}", e.getMessage());
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnError(emitter::completeWithError)
                        .subscribe();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    @Operation(summary = "同步对话")
    @PostMapping("/{id}/chat")
    public R<String> chat(
            @PathVariable Long id,
            @RequestBody ChatRequest request) {
        return R.ok(agentService.chat(id, request.getMessage(), request.getConversationId()));
    }

    @Operation(summary = "执行复杂任务（Plan-Execute）")
    @PostMapping("/{id}/execute")
    public R<String> execute(
            @PathVariable Long id,
            @RequestBody ChatRequest request) {
        return R.ok(agentService.execute(id, request.getMessage(), request.getConversationId()));
    }

    @Operation(summary = "获取Agent运行状态")
    @GetMapping("/{id}/state")
    public R<AgentState> getState(@PathVariable Long id) {
        return R.ok(agentService.getAgentState(id));
    }

    @lombok.Data
    public static class ChatRequest {
        private String message;
        private String conversationId = "default";
    }
}
