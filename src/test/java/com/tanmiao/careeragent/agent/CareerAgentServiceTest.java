package com.tanmiao.careeragent.agent;

import com.tanmiao.careeragent.error.AgentExecutionException;
import com.tanmiao.careeragent.trace.ToolTraceRecorder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CareerAgentServiceTest {

    @Test
    void generatesIdsAndReturnsThenClearsTraces() {
        var traces = new ToolTraceRecorder();
        CareerAgentGateway gateway = (conversationId, requestId, message) -> {
            traces.record(requestId, "analyze_job_description", "summary", () -> "ok");
            return "分析完成";
        };
        var service = new CareerAgentService(gateway, traces);

        var response = service.chat(null, "分析这份 Java JD");

        assertThat(response.requestId()).isNotBlank();
        assertThat(response.conversationId()).isNotBlank();
        assertThat(response.answer()).isEqualTo("分析完成");
        assertThat(response.toolTraces()).singleElement()
                .satisfies(trace -> assertThat(trace.toolName())
                        .isEqualTo("analyze_job_description"));
        assertThat(traces.findByRequestId(response.requestId())).isEmpty();
    }

    @Test
    void preservesProvidedConversationId() {
        CareerAgentGateway gateway = (conversationId, requestId, message) -> "继续";
        var service = new CareerAgentService(gateway, new ToolTraceRecorder());

        assertThat(service.chat("conversation-1", "继续准备").conversationId())
                .isEqualTo("conversation-1");
    }

    @Test
    void rejectsBlankAndOversizedMessagesBeforeCallingGateway() {
        CareerAgentGateway shouldNotRun = (conversationId, requestId, message) -> {
            throw new AssertionError("gateway should not be called");
        };
        var service = new CareerAgentService(shouldNotRun, new ToolTraceRecorder());

        assertThatThrownBy(() -> service.chat(null, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("消息不能为空");
        assertThatThrownBy(() -> service.chat(null, "x".repeat(8001)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("消息不能超过8000个字符");
    }

    @Test
    void wrapsProviderFailureAndClearsTracesWithoutLeakingSecrets() {
        var traces = new ToolTraceRecorder();
        CareerAgentGateway gateway = (conversationId, requestId, message) -> {
            traces.record(requestId, "analyze_job_description", "summary", () -> "ok");
            throw new RuntimeException("Authorization: Bearer sk-secret");
        };
        var service = new CareerAgentService(gateway, traces);

        assertThatThrownBy(() -> service.chat("c1", "hello"))
                .isInstanceOf(AgentExecutionException.class)
                .hasMessage("AI 服务暂时不可用，请稍后重试");
    }
}
