package com.tanmiao.careeragent.tool;

import com.tanmiao.careeragent.trace.ToolTraceRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobDescriptionToolsTest {

    @Test
    void extractsOnlyExplicitRequirementsWithoutDuplicates() {
        var tools = new JobDescriptionTools(new ToolTraceRecorder());

        var result = tools.analyzeJobDescription(
                "AI应用开发实习生",
                "本科及以上，负责 Java 服务开发与测试；熟悉 Spring Boot、Spring AI、RAG、Redis，了解 Java 工程化。",
                context("req-1"));

        assertThat(result.skills())
                .containsExactly("Java", "Spring Boot", "Spring AI", "RAG", "Redis");
        assertThat(result.educationRequirements()).containsExactly("本科");
        assertThat(result.responsibilityKeywords()).containsExactly("开发", "测试");
    }

    @Test
    void rejectsBlankAndOversizedDescriptions() {
        var tools = new JobDescriptionTools(new ToolTraceRecorder());

        assertThatThrownBy(() -> tools.analyzeJobDescription("AI实习生", " ", context("req-2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("岗位描述不能为空");
        assertThatThrownBy(() -> tools.analyzeJobDescription(
                "AI实习生", "x".repeat(8001), context("req-3")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("岗位描述不能超过8000个字符");
    }

    @Test
    void rejectsBlankJobTitle() {
        var tools = new JobDescriptionTools(new ToolTraceRecorder());

        assertThatThrownBy(() -> tools.analyzeJobDescription(" ", "Java", context("req-4")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("目标岗位不能为空");
    }

    private static ToolContext context(String requestId) {
        return new ToolContext(Map.of("requestId", requestId));
    }
}
