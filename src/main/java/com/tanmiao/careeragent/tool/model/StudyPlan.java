package com.tanmiao.careeragent.tool.model;

import java.util.List;

public record StudyPlan(
        String targetRole,
        int daysPerWeek,
        List<String> prioritizedSkills,
        List<StudyPhase> phases
) {
    public StudyPlan {
        prioritizedSkills = List.copyOf(prioritizedSkills);
        phases = List.copyOf(phases);
    }
}
