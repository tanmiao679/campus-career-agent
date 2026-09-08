package com.tanmiao.careeragent.tool;

import com.tanmiao.careeragent.tool.model.InterviewQuestionSet;
import com.tanmiao.careeragent.trace.ToolTraceRecorder;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class InterviewTools {

    private static final Map<String, List<String>> QUESTION_BANK = questionBank();
    private static final List<String> JAVA_FALLBACK = List.of(
            "Java 中 HashMap 的查找过程和扩容机制是什么？",
            "Java 的 checked exception 与 unchecked exception 应该如何取舍？",
            "Java 线程池的核心参数分别控制什么行为？",
            "Java record 适合哪些场景，又有哪些限制？",
            "Java 中 equals 与 hashCode 为什么必须保持一致？",
            "Java 服务发生内存持续上涨时，你会如何定位？",
            "Java 中 Stream 的有状态操作有哪些性能风险？",
            "Java 接口与抽象类在工程设计中如何选择？",
            "Java 单元测试应该如何划分正常、边界和异常场景？",
            "Java REST 服务如何避免把内部异常和敏感信息暴露给调用方？"
    );

    private final ToolTraceRecorder traceRecorder;

    public InterviewTools(ToolTraceRecorder traceRecorder) {
        this.traceRecorder = traceRecorder;
    }

    @Tool(name = "generate_interview_questions",
            description = "根据目标岗位和明确技能生成1到10道可用于技术面试准备的针对性问题。")
    public InterviewQuestionSet generateInterviewQuestions(
            @ToolParam(description = "目标岗位名称") String targetRole,
            @ToolParam(description = "岗位涉及的技能列表") List<String> skills,
            @ToolParam(description = "需要生成的题目数量，1到10") int count,
            ToolContext toolContext) {
        validate(targetRole, skills, count);
        List<String> cleanedSkills = cleanSkills(skills);
        if (cleanedSkills.isEmpty()) {
            throw new IllegalArgumentException("至少提供一个岗位技能");
        }

        String summary = "岗位=" + targetRole.strip() + ", 技能数=" + cleanedSkills.size()
                + ", 题目数=" + count;
        return traceRecorder.record(requestId(toolContext), "generate_interview_questions", summary,
                () -> buildQuestionSet(targetRole.strip(), cleanedSkills, count));
    }

    private static InterviewQuestionSet buildQuestionSet(String role, List<String> skills, int count) {
        LinkedHashSet<String> selected = new LinkedHashSet<>();
        for (String skill : skills) {
            List<String> questions = QUESTION_BANK.get(skill.toLowerCase(Locale.ROOT));
            if (questions != null) {
                selected.addAll(questions);
            }
            if (selected.size() >= count) {
                break;
            }
        }
        for (String fallback : JAVA_FALLBACK) {
            if (selected.size() >= count) {
                break;
            }
            selected.add(fallback);
        }
        return new InterviewQuestionSet(role, skills,
                new ArrayList<>(selected).subList(0, count));
    }

    private static List<String> cleanSkills(List<String> skills) {
        Map<String, String> unique = new LinkedHashMap<>();
        for (String skill : skills) {
            if (skill != null && !skill.isBlank()) {
                unique.putIfAbsent(skill.strip().toLowerCase(Locale.ROOT), skill.strip());
            }
        }
        return List.copyOf(unique.values());
    }

    private static void validate(String role, List<String> skills, int count) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("目标岗位不能为空");
        }
        if (skills == null || skills.isEmpty()) {
            throw new IllegalArgumentException("至少提供一个岗位技能");
        }
        if (count < 1 || count > 10) {
            throw new IllegalArgumentException("面试题数量必须在1到10之间");
        }
    }

    private static String requestId(ToolContext context) {
        if (context == null || context.getContext() == null) {
            return "unknown-request";
        }
        return String.valueOf(context.getContext().getOrDefault("requestId", "unknown-request"));
    }

    private static Map<String, List<String>> questionBank() {
        Map<String, List<String>> bank = new LinkedHashMap<>();
        bank.put("java", List.of(
                "Java 中 HashMap 在并发写入时可能出现什么问题？",
                "Java 线程池如何设置队列和拒绝策略以避免服务雪崩？"));
        bank.put("spring boot", List.of(
                "Spring Boot 自动配置的生效条件和排查方式是什么？",
                "Spring Boot 中如何设计统一参数校验和异常响应？"));
        bank.put("spring ai", List.of(
                "Spring AI 的 ChatClient、ChatModel 和 Advisor 各自承担什么职责？",
                "Spring AI 如何注册工具并把工具结果送回模型继续推理？"));
        bank.put("tool calling", List.of(
                "Tool Calling 与把全部逻辑写进一个 Prompt 相比有什么优势？",
                "Tool Calling 怎样限制循环次数并处理非法工具参数？"));
        bank.put("rag", List.of(
                "RAG 的召回、重排和生成三个环节分别可能出现什么问题？",
                "RAG 中如何设计可复现的检索质量评测？"));
        bank.put("redis", List.of(
                "Redis 适合保存会话记忆吗？需要考虑哪些过期和一致性问题？",
                "Redis 缓存穿透、击穿和雪崩分别如何治理？"));
        bank.put("postgresql", List.of(
                "PostgreSQL 的索引为什么可能存在但不被查询计划使用？",
                "PostgreSQL 事务隔离级别如何影响并发读写？"));
        bank.put("websocket", List.of(
                "WebSocket 连接断开后如何实现可靠重连和状态恢复？",
                "WebSocket 与 SSE 在流式AI回答场景中如何选择？"));
        bank.put("docker", List.of(
                "Docker 镜像如何通过多阶段构建减少体积？",
                "Docker 容器中的健康检查与应用就绪检查有什么区别？"));
        return Map.copyOf(bank);
    }
}
