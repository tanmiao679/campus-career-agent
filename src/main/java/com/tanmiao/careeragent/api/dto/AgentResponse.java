package com.tanmiao.careeragent.api.dto;

import com.tanmiao.careeragent.trace.ToolTrace;

import java.util.List;

public record AgentResponse(
        String requestId,
        String conversationId,
        String answer,
        List<ToolTrace> toolTraces
) {
    public AgentResponse {
        toolTraces = List.copyOf(toolTraces);
    }
}
