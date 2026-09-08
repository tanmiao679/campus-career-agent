package com.tanmiao.careeragent.api;

import com.tanmiao.careeragent.agent.CareerAgentService;
import com.tanmiao.careeragent.api.dto.AgentRequest;
import com.tanmiao.careeragent.api.dto.AgentResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    private final CareerAgentService service;

    public AgentController(CareerAgentService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    public AgentResponse chat(@Valid @RequestBody AgentRequest request) {
        return service.chat(request.conversationId(), request.message());
    }
}
