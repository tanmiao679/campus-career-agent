package com.tanmiao.careeragent.tool;

import com.tanmiao.careeragent.trace.ToolTraceRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterviewToolsTest {

    @Test
    void returnsRequestedNumberOfRelevantDistinctQuestions() {
        var tools = new InterviewTools(new ToolTraceRecorder());

        var result = tools.generateInterviewQuestions(
                "Java Agent 实习生",
                List.of("Spring AI", "RAG", "Redis"),
                5,
                context("req-1"));

        assertThat(result.questions()).hasSize(5).doesNotHaveDuplicates();
        assertThat(result.questions()).anyMatch(question ->
                question.contains("Spring AI") || question.contains("RAG"));
    }

    @Test
    void usesBackendQuestionsWhenSkillsAreUnknown() {
        var tools = new InterviewTools(new ToolTraceRecorder());

        var result = tools.generateInterviewQuestions(
                "AI应用开发", List.of("未知框架"), 3, context("req-2"));

        assertThat(result.questions()).hasSize(3);
        assertThat(result.questions()).allMatch(question -> question.contains("Java"));
    }

    @Test
    void rejectsCountsOutsideOneToTen() {
        var tools = new InterviewTools(new ToolTraceRecorder());

        assertThatThrownBy(() -> tools.generateInterviewQuestions(
                "Java", List.of("Java"), 11, context("req-3")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("面试题数量必须在1到10之间");
    }

    @Test
    void requiresRoleAndSkills() {
        var tools = new InterviewTools(new ToolTraceRecorder());

        assertThatThrownBy(() -> tools.generateInterviewQuestions(
                " ", List.of("Java"), 3, context("req-4")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("目标岗位不能为空");
        assertThatThrownBy(() -> tools.generateInterviewQuestions(
                "Java", List.of(), 3, context("req-5")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("至少提供一个岗位技能");
    }

    private static ToolContext context(String requestId) {
        return new ToolContext(Map.of("requestId", requestId));
    }
}
