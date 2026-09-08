package com.tanmiao.careeragent.tool;

import com.tanmiao.careeragent.trace.ToolTraceRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudyPlanToolsTest {

    @Test
    void prioritizesCoreAgentSkillsAndBuildsThreePhases() {
        var tools = new StudyPlanTools(new ToolTraceRecorder());

        var result = tools.generateStudyPlan(
                "Java AI应用开发",
                List.of("MCP", "Spring AI", "Tool Calling", "RAG", "spring ai"),
                5,
                context("req-1"));

        assertThat(result.prioritizedSkills())
                .containsExactly("Spring AI", "Tool Calling", "RAG", "MCP");
        assertThat(result.phases()).extracting("name")
                .containsExactly("基础补齐", "项目实现", "面试验证");
        assertThat(result.phases()).allSatisfy(phase ->
                assertThat(phase.deliverables()).isNotEmpty());
    }

    @Test
    void rejectsInvalidStudyDays() {
        var tools = new StudyPlanTools(new ToolTraceRecorder());

        assertThatThrownBy(() -> tools.generateStudyPlan(
                "Java", List.of("RAG"), 0, context("req-2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("每周学习天数必须在1到7之间");
    }

    @Test
    void requiresRoleAndAtLeastOneMissingSkill() {
        var tools = new StudyPlanTools(new ToolTraceRecorder());

        assertThatThrownBy(() -> tools.generateStudyPlan(
                " ", List.of("RAG"), 5, context("req-3")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("目标岗位不能为空");
        assertThatThrownBy(() -> tools.generateStudyPlan(
                "Java", List.of(), 5, context("req-4")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("至少提供一个待提升技能");
    }

    private static ToolContext context(String requestId) {
        return new ToolContext(Map.of("requestId", requestId));
    }
}
