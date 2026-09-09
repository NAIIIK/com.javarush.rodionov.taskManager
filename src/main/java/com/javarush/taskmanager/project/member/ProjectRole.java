package com.javarush.taskmanager.project.member;

import lombok.Getter;

@Getter
public enum ProjectRole {
    OWNER(0),
    MANAGER(1),
    MEMBER(2);

    private final int weight;

    ProjectRole(int weight) {
        this.weight = weight;
    }
}