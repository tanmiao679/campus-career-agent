package com.tanmiao.careeragent.tool;

import com.tanmiao.careeragent.tool.model.StudyPhase;
import com.tanmiao.careeragent.tool.model.StudyPlan;
import com.tanmiao.careeragent.trace.ToolTraceRecorder;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class StudyPlanTools {

    private static final List<String> PRIORITY_ORDER = List.of(
            "Java", "Spring Boot", "REST API",
            "Spring AI", "Tool Calling", "Prompt Engineering",
            "RAG", "pgvector", "PostgreSQL",
            "测试", "异常处理", "MCP");

    private static final Map<String, String> CANONICAL_NAMES = canonicalNames();

    private final ToolTraceRecorder traceRecorder;

    public StudyPlanTools(ToolTraceRecorder traceRecorder) {
        this.traceRecorder = traceRecorder;
    }

    @Tool(name = "generate_study_plan",
            description = "根据目标岗位和明确的能力缺口，生成分为基础补齐、项目实现、面试验证三个阶段的学习计划。")
    public StudyPlan generateStudyPlan(
            @ToolParam(description = "目标岗位名称") String targetRole,
            @ToolParam(description = "需要提升的技能列表") List<String> missingSkills,
            @ToolParam(description = "每周可学习天数，1到7") int daysPerWeek,
            ToolContext toolContext) {
        validate(targetRole, missingSkills, daysPerWeek);
        List<String> prioritized = prioritize(missingSkills);
        if (prioritized.isEmpty()) {
            throw new IllegalArgumentException("至少提供一个待提升技能");
        }

        String requestId = requestId(toolContext);
        String summary = "岗位=" + targetRole.strip() + ", 技能数=" + prioritized.size()
                + ", 每周天数=" + daysPerWeek;
        return traceRecorder.record(requestId, "generate_study_plan", summary,
                () -> buildPlan(targetRole.strip(), prioritized, daysPerWeek));
    }

    private static StudyPlan buildPlan(String targetRole, List<String> skills, int daysPerWeek) {
        int foundationEnd = Math.min(2, skills.size());
        List<String> foundationSkills = skills.subList(0, foundationEnd);
        List<String> projectSkills = skills.size() > foundationEnd
                ? skills.subList(foundationEnd, skills.size())
                : skills;

        List<StudyPhase> phases = List.of(
                new StudyPhase("基础补齐", foundationSkills,
                        List.of("完成核心概念笔记，并为每个技能写一个最小代码练习")),
                new StudyPhase("项目实现", projectSkills,
                        List.of("把目标技能接入可运行项目，并补充自动化测试与失败场景")),
                new StudyPhase("面试验证", skills,
                        List.of("准备60秒项目介绍，完成技术追问复盘并现场演示一次"))
        );
        return new StudyPlan(targetRole, daysPerWeek, skills, phases);
    }

    private static List<String> prioritize(List<String> missingSkills) {
        Map<String, String> unique = new LinkedHashMap<>();
        for (String rawSkill : missingSkills) {
            if (rawSkill == null || rawSkill.isBlank()) {
                continue;
            }
            String trimmed = rawSkill.strip();
            String key = trimmed.toLowerCase(Locale.ROOT);
            unique.putIfAbsent(key, CANONICAL_NAMES.getOrDefault(key, trimmed));
        }
        List<String> result = new ArrayList<>(unique.values());
        result.sort((left, right) -> Integer.compare(priority(left), priority(right)));
        return List.copyOf(result);
    }

    private static int priority(String skill) {
        int index = PRIORITY_ORDER.indexOf(skill);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private static Map<String, String> canonicalNames() {
        Map<String, String> names = new LinkedHashMap<>();
        PRIORITY_ORDER.forEach(skill -> names.put(skill.toLowerCase(Locale.ROOT), skill));
        return Map.copyOf(names);
    }

    private static void validate(String targetRole, List<String> missingSkills, int daysPerWeek) {
        if (targetRole == null || targetRole.isBlank()) {
            throw new IllegalArgumentException("目标岗位不能为空");
        }
        if (missingSkills == null || missingSkills.isEmpty()) {
            throw new IllegalArgumentException("至少提供一个待提升技能");
        }
        if (daysPerWeek < 1 || daysPerWeek > 7) {
            throw new IllegalArgumentException("每周学习天数必须在1到7之间");
        }
    }

    private static String requestId(ToolContext context) {
        if (context == null || context.getContext() == null) {
            return "unknown-request";
        }
        return String.valueOf(context.getContext().getOrDefault("requestId", "unknown-request"));
    }
}
