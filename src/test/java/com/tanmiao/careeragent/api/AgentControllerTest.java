package com.tanmiao.careeragent.api;

import com.tanmiao.careeragent.agent.CareerAgentService;
import com.tanmiao.careeragent.api.dto.AgentResponse;
import com.tanmiao.careeragent.error.AgentExecutionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentController.class)
class AgentControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CareerAgentService service;

    @Test
    void returnsAgentResponse() throws Exception {
        when(service.chat(isNull(), org.mockito.ArgumentMatchers.eq("分析 Java Agent JD")))
                .thenReturn(new AgentResponse("r1", "c1", "完成", List.of()));

        mvc.perform(post("/api/v1/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"分析 Java Agent JD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("r1"))
                .andExpect(jsonPath("$.conversationId").value("c1"))
                .andExpect(jsonPath("$.answer").value("完成"));
    }

    @Test
    void rejectsBlankMessageWithStableErrorShape() throws Exception {
        mvc.perform(post("/api/v1/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("消息不能为空"));
    }

    @Test
    void mapsProviderFailureWithoutLeakingDetails() throws Exception {
        when(service.chat(isNull(), org.mockito.ArgumentMatchers.eq("分析岗位")))
                .thenThrow(new AgentExecutionException());

        mvc.perform(post("/api/v1/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"分析岗位\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("AI 服务暂时不可用，请稍后重试"));
    }
}
