package com.tanmiao.careeragent.trace;

public record ToolTrace(
        String toolName,
        ToolTraceStatus status,
        String inputSummary,
        long durationMs,
        String errorMessage
) {
}
