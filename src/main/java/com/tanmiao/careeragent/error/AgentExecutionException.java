package com.tanmiao.careeragent.error;

public class AgentExecutionException extends RuntimeException {

    public AgentExecutionException() {
        super("AI 服务暂时不可用，请稍后重试");
    }
}
