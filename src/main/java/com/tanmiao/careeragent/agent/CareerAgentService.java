package com.tanmiao.careeragent.agent;

import com.tanmiao.careeragent.api.dto.AgentResponse;
import com.tanmiao.careeragent.error.AgentExecutionException;
import com.tanmiao.careeragent.trace.ToolTraceRecorder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CareerAgentService {

    private static final int MAX_MESSAGE_LENGTH = 8_000;

    private final CareerAgentGateway gateway;
    private final ToolTraceRecorder traceRecorder;

    public CareerAgentService(CareerAgentGateway gateway, ToolTraceRecorder traceRecorder) {
        this.gateway = gateway;
        this.traceRecorder = traceRecorder;
    }

    public AgentResponse chat(String conversationId, String message) {
        validate(message);
        String resolvedConversationId = conversationId == null || conversationId.isBlank()
                ? UUID.randomUUID().toString()
                : conversationId.strip();
        String requestId = UUID.randomUUID().toString();

        try {
            String answer = gateway.execute(resolvedConversationId, requestId, message.strip());
            return new AgentResponse(requestId, resolvedConversationId, answer,
                    traceRecorder.drainByRequestId(requestId));
        } catch (RuntimeException exception) {
            traceRecorder.drainByRequestId(requestId);
            throw new AgentExecutionException();
        }
    }

    private static void validate(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("消息不能为空");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("消息不能超过8000个字符");
        }
    }
}
