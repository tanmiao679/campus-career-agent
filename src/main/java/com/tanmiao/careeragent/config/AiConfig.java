package com.tanmiao.careeragent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(12)
                .build();
    }

    @Bean
    ChatClient careerChatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultSystem("""
                        你是校园求职助手，只处理求职、岗位分析、学习规划和面试准备。
                        涉及岗位要求时优先调用岗位分析工具；需要计划或题目时调用对应工具。
                        不虚构用户经历，不声称用户掌握未提供的技能。工具失败时说明限制并停止重复调用。
                        对明显无关的请求礼貌拒绝，不调用求职工具。
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
