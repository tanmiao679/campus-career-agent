package com.tanmiao.careeragent.tool;

import com.tanmiao.careeragent.tool.model.JobAnalysis;
import com.tanmiao.careeragent.trace.ToolTraceRecorder;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class JobDescriptionTools {

    private static final int MAX_DESCRIPTION_LENGTH = 8_000;

    private static final List<SkillDefinition> SKILLS = List.of(
            skill("Java", "java"),
            skill("Spring Boot", "spring boot", "springboot"),
            skill("Spring AI", "spring ai"),
            skill("RAG", "rag", "retrieval augmented generation", "检索增强"),
            skill("PostgreSQL", "postgresql", "postgres"),
            skill("pgvector", "pgvector"),
            skill("Redis", "redis"),
            skill("Docker", "docker"),
            skill("Git", "git"),
            skill("REST API", "rest api", "restful"),
            skill("WebSocket", "websocket"),
            skill("Tool Calling", "tool calling", "工具调用"),
            skill("MCP", "mcp"),
            skill("Prompt Engineering", "prompt engineering", "提示词工程")
    );

    private static final List<String> EDUCATION = List.of("大专", "本科", "硕士", "博士");
    private static final List<String> RESPONSIBILITIES = List.of(
            "设计", "开发", "测试", "部署", "优化", "维护", "协作", "分析");

    private final ToolTraceRecorder traceRecorder;

    public JobDescriptionTools(ToolTraceRecorder traceRecorder) {
        this.traceRecorder = traceRecorder;
    }

    @Tool(name = "analyze_job_description",
            description = "分析软件或AI岗位描述，返回明确写出的技能、学历要求和职责关键词。根据JD制定学习计划或面试题之前优先使用。")
    public JobAnalysis analyzeJobDescription(
            @ToolParam(description = "目标岗位名称") String jobTitle,
            @ToolParam(description = "完整岗位描述，最多8000字符") String jobDescription,
            ToolContext toolContext) {
        validate(jobTitle, jobDescription);
        String requestId = requestId(toolContext);
        String summary = "岗位=" + jobTitle.strip() + ", JD长度=" + jobDescription.length();
        return traceRecorder.record(requestId, "analyze_job_description", summary,
                () -> analyze(jobTitle.strip(), jobDescription));
    }

    private static JobAnalysis analyze(String jobTitle, String description) {
        String normalized = description.toLowerCase(Locale.ROOT);
        List<String> skills = SKILLS.stream()
                .filter(skill -> skill.aliases().stream().anyMatch(normalized::contains))
                .map(SkillDefinition::name)
                .toList();
        List<String> education = EDUCATION.stream().filter(description::contains).toList();
        List<String> responsibilities = RESPONSIBILITIES.stream().filter(description::contains).toList();
        return new JobAnalysis(jobTitle, skills, education, responsibilities);
    }

    private static void validate(String jobTitle, String jobDescription) {
        if (jobTitle == null || jobTitle.isBlank()) {
            throw new IllegalArgumentException("目标岗位不能为空");
        }
        if (jobDescription == null || jobDescription.isBlank()) {
            throw new IllegalArgumentException("岗位描述不能为空");
        }
        if (jobDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("岗位描述不能超过8000个字符");
        }
    }

    private static String requestId(ToolContext context) {
        if (context == null || context.getContext() == null) {
            return "unknown-request";
        }
        return String.valueOf(context.getContext().getOrDefault("requestId", "unknown-request"));
    }

    private static SkillDefinition skill(String name, String... aliases) {
        return new SkillDefinition(name, List.of(aliases));
    }

    private record SkillDefinition(String name, List<String> aliases) {
    }
}
