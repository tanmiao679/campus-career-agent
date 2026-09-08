package com.tanmiao.careeragent.tool.model;

import java.util.List;

public record StudyPhase(String name, List<String> skills, List<String> deliverables) {
    public StudyPhase {
        skills = List.copyOf(skills);
        deliverables = List.copyOf(deliverables);
    }
}
