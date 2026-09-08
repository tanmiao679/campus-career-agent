package com.tanmiao.careeragent.smoke;

import com.tanmiao.careeragent.agent.CareerAgentService;
import com.tanmiao.careeragent.api.dto.AgentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.main.web-application-type=none")
@Import(RealDeepSeekSmokeTest.SmokeHttpConfiguration.class)
@EnabledIfEnvironmentVariable(named = "SPRING_AI_DEEPSEEK_API_KEY", matches = ".+")
class RealDeepSeekSmokeTest {

    @Autowired
    private CareerAgentService careerAgentService;

    @Test
    void completesTheFiveRecruitmentScenariosWithoutStartingAWebServer() {
        AgentResponse jd = run(1, null,
                "请分析：招聘 Java AI 应用开发实习生，要求 Spring Boot、Spring AI、RAG、Redis。");
        assertThat(toolNames(jd)).contains("analyze_job_description");

        AgentResponse plan = run(2, jd.conversationId(),
                "根据刚才的岗位要求，为只会 Java 和 Spring Boot 的学生制定每周学习 5 天的计划。");
        assertThat(toolNames(plan)).contains("generate_study_plan");

        AgentResponse questions = run(3, jd.conversationId(),
                "根据刚才的岗位生成 5 道面试题。");
        assertThat(toolNames(questions)).contains("generate_interview_questions");

        AgentResponse unrelated = run(4, null, "帮我写一首与求职无关的长诗。");
        assertThat(unrelated.toolTraces()).isEmpty();

        AgentResponse invalid = run(5, null,
                "严格按参数调用学习计划工具：目标岗位 Java AI 应用开发，缺口技能 Spring AI，daysPerWeek 必须为 0；"
                        + "如果工具拒绝该参数，请解释失败原因并停止继续调用。");
        assertThat(invalid.answer()).isNotBlank();
        assertThat(invalid.toolTraces()).hasSizeLessThanOrEqualTo(2);
    }

    private AgentResponse run(int scenario, String conversationId, String prompt) {
        AgentResponse response = careerAgentService.chat(conversationId, prompt);
        assertThat(response.answer()).isNotBlank();
        System.out.printf("[SMOKE] scenario=%d status=PASS tools=%s answerChars=%d%n",
                scenario, toolNames(response), response.answer().length());
        return response;
    }

    private static List<String> toolNames(AgentResponse response) {
        return response.toolTraces().stream().map(trace -> trace.toolName()).toList();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SmokeHttpConfiguration {

        @Bean
        RestClientCustomizer blockingRequestFactoryCustomizer() {
            return builder -> builder.requestFactory(new SimpleClientHttpRequestFactory());
        }
    }
}
