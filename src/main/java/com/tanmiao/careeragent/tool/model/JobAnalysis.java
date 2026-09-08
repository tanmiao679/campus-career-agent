package com.tanmiao.careeragent.tool.model;

import java.util.List;

public record JobAnalysis(
        String jobTitle,
        List<String> skills,
        List<String> educationRequirements,
        List<String> responsibilityKeywords
) {
    public JobAnalysis {
        skills = List.copyOf(skills);
        educationRequirements = List.copyOf(educationRequirements);
        responsibilityKeywords = List.copyOf(responsibilityKeywords);
    }
}
