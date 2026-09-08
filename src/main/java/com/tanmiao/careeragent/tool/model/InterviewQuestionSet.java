package com.tanmiao.careeragent.tool.model;

import java.util.List;

public record InterviewQuestionSet(
        String targetRole,
        List<String> skills,
        List<String> questions
) {
    public InterviewQuestionSet {
        skills = List.copyOf(skills);
        questions = List.copyOf(questions);
    }
}
