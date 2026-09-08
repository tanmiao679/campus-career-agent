package com.tanmiao.careeragent.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentRequest(
        String conversationId,
        @NotBlank(message = "消息不能为空")
        @Size(max = 8000, message = "消息不能超过8000个字符")
        String message
) {
}
