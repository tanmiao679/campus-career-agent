package com.tanmiao.careeragent.trace;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolTraceRecorderTest {

    @Test
    void recordsSuccessfulExecution() {
        var recorder = new ToolTraceRecorder();

        String result = recorder.record("req-1", "analyze_job_description", "Java JD", () -> "ok");

        assertThat(result).isEqualTo("ok");
        assertThat(recorder.findByRequestId("req-1")).singleElement()
                .satisfies(trace -> {
                    assertThat(trace.toolName()).isEqualTo("analyze_job_description");
                    assertThat(trace.status()).isEqualTo(ToolTraceStatus.SUCCESS);
                    assertThat(trace.errorMessage()).isNull();
                });
    }

    @Test
    void recordsFailureWithoutPersistingExceptionDetails() {
        var recorder = new ToolTraceRecorder();

        assertThatThrownBy(() -> recorder.record("req-2", "study_plan", "input",
                () -> {
                    throw new IllegalStateException("Authorization: Bearer sk-secret");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(recorder.findByRequestId("req-2")).singleElement()
                .satisfies(trace -> {
                    assertThat(trace.status()).isEqualTo(ToolTraceStatus.FAILED);
                    assertThat(trace.errorMessage()).isEqualTo("IllegalStateException");
                });
    }

    @Test
    void truncatesAndRedactsTraceInput() {
        var recorder = new ToolTraceRecorder();

        recorder.record("req-3", "tool",
                "邮箱 test@example.com 电话 13800138000 " + "x".repeat(300), () -> "ok");

        String summary = recorder.findByRequestId("req-3").getFirst().inputSummary();
        assertThat(summary).doesNotContain("test@example.com", "13800138000");
        assertThat(summary).contains("[REDACTED]");
        assertThat(summary.length()).isLessThanOrEqualTo(200);
    }

    @Test
    void drainsCompletedRequestToAvoidUnboundedRetention() {
        var recorder = new ToolTraceRecorder();
        recorder.record("req-4", "tool", "input", () -> "ok");

        assertThat(recorder.drainByRequestId("req-4")).hasSize(1);
        assertThat(recorder.findByRequestId("req-4")).isEmpty();
    }
}
