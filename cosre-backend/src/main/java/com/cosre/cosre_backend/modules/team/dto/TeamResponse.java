package com.cosre.cosre_backend.modules.team.dto;

import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.team.entity.Team;
import java.time.LocalDateTime;
import java.util.List;

public record TeamResponse(Long id, String name, String description, Long classroomId, Long projectId,
        Person lecturer, Person leader, List<Person> members, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public record Person(Long id, String username, String fullName, String email) {
        static Person from(User user) { return user == null ? null : new Person(user.getId(), user.getUsername(), user.getFullName(), user.getEmail()); }
    }
    public static TeamResponse from(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getDescription(), team.getClassroomId(), team.getProjectId(),
                Person.from(team.getLecturer()), Person.from(team.getLeader()), team.getMembers().stream().map(Person::from).toList(),
                team.getCreatedAt(), team.getUpdatedAt());
    }
}
