package vip.mate.tool.builtin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import vip.mate.tool.search.SearchQuery;

/**
 * 内置工具：网页搜索
 * <p>通过 WebSearchService 动态路由至最佳搜索 provider（含 keyless fallback），
 * 支持 freshness / language / count 等高级搜索参数。
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSearchTool {

    private final WebSearchService webSearchService;

    @Tool(description = "在互联网上搜索最新信息。当需要查询实时新闻、最新数据或不确定的事实时使用此工具。"
            + "支持可选参数：freshness 控制时间范围（day/week/month/year），"
            + "language 指定语言偏好（zh-CN/en），count 指定结果数量（1-10）。")
    public String search(
            @ToolParam(description = "搜索关键词") String query,
            @ToolParam(description = "时间范围过滤: day (今天), week (本周), month (本月), year (今年)", required = false) String freshness,
            @ToolParam(description = "语言偏好: zh-CN (中文), en (英文)", required = false) String language,
            @ToolParam(description = "最大结果数量: 1-10, 默认 5", required = false) Integer count
    ) {
        SearchQuery searchQuery = new SearchQuery(query, freshness, language, count);
        return webSearchService.search(searchQuery);
    }
}
