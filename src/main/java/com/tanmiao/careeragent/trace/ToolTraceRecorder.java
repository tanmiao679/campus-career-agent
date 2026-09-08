package com.tanmiao.careeragent.trace;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Component
public class ToolTraceRecorder {

    private static final int MAX_SUMMARY_LENGTH = 200;
    private static final Pattern EMAIL = Pattern.compile(
            "(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern MOBILE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern CREDENTIAL = Pattern.compile(
            "(?i)(?:Bearer\\s+\\S+|sk-[A-Za-z0-9_-]{4,})");

    private final Map<String, CopyOnWriteArrayList<ToolTrace>> traces = new ConcurrentHashMap<>();

    public <T> T record(String requestId, String toolName, String inputSummary, Supplier<T> action) {
        long startedAt = System.nanoTime();
        try {
            T result = action.get();
            append(requestId, new ToolTrace(toolName, ToolTraceStatus.SUCCESS,
                    sanitize(inputSummary), elapsedMs(startedAt), null));
            return result;
        } catch (RuntimeException exception) {
            append(requestId, new ToolTrace(toolName, ToolTraceStatus.FAILED,
                    sanitize(inputSummary), elapsedMs(startedAt), exception.getClass().getSimpleName()));
            throw exception;
        }
    }

    public List<ToolTrace> findByRequestId(String requestId) {
        var requestTraces = traces.get(requestId);
        return requestTraces == null ? List.of() : List.copyOf(requestTraces);
    }

    public List<ToolTrace> drainByRequestId(String requestId) {
        var requestTraces = traces.remove(requestId);
        return requestTraces == null ? List.of() : List.copyOf(requestTraces);
    }

    private void append(String requestId, ToolTrace trace) {
        traces.computeIfAbsent(requestId, ignored -> new CopyOnWriteArrayList<>()).add(trace);
    }

    private static long elapsedMs(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private static String sanitize(String value) {
        String sanitized = value == null ? "" : value;
        sanitized = EMAIL.matcher(sanitized).replaceAll("[REDACTED]");
        sanitized = MOBILE.matcher(sanitized).replaceAll("[REDACTED]");
        sanitized = CREDENTIAL.matcher(sanitized).replaceAll("[REDACTED]");
        return sanitized.length() <= MAX_SUMMARY_LENGTH
                ? sanitized
                : sanitized.substring(0, MAX_SUMMARY_LENGTH);
    }
}
