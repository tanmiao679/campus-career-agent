package com.tanmiao.careeragent.agent;

@FunctionalInterface
public interface CareerAgentGateway {

    String execute(String conversationId, String requestId, String message);
}
