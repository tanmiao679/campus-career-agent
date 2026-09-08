package com.tanmiao.careeragent.agent;

import com.tanmiao.careeragent.tool.InterviewTools;
import com.tanmiao.careeragent.tool.JobDescriptionTools;
import com.tanmiao.careeragent.tool.StudyPlanTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SpringAiCareerAgentGateway implements CareerAgentGateway {

    private final ChatClient chatClient;
    private final JobDescriptionTools jobDescriptionTools;
    private final StudyPlanTools studyPlanTools;
    private final InterviewTools interviewTools;

    public SpringAiCareerAgentGateway(
            ChatClient chatClient,
            JobDescriptionTools jobDescriptionTools,
            StudyPlanTools studyPlanTools,
            InterviewTools interviewTools) {
        this.chatClient = chatClient;
        this.jobDescriptionTools = jobDescriptionTools;
        this.studyPlanTools = studyPlanTools;
        this.interviewTools = interviewTools;
    }

    @Override
    public String execute(String conversationId, String requestId, String message) {
        return chatClient.prompt()
                .user(message)
                .tools(jobDescriptionTools, studyPlanTools, interviewTools)
                .toolContext(Map.of("requestId", requestId))
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
