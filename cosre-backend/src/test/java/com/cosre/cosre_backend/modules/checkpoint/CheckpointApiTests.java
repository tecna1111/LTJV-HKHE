package com.cosre.cosre_backend.modules.checkpoint;

import com.cosre.cosre_backend.config.JwtConfig;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.classroom.repository.ClassroomRepository;
import com.cosre.cosre_backend.modules.team.entity.Team;
import com.cosre.cosre_backend.modules.team.repository.TeamRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class CheckpointApiTests {
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy security;
    @Autowired JwtConfig jwt;
    @Autowired UserRepository users;
    @Autowired TeamRepository teams;
    @Autowired ClassroomRepository classrooms;
    MockMvc mvc;
    User leader, member;
    Team team;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        leader = users.findByUsername("student1").orElseThrow();
        member = users.findByUsername("student2").orElseThrow();
        var lecturer = users.findByUsername("lecturer").orElseThrow();
        team = new Team(); team.setName("Checkpoint API test");
        team.setClassroomId(classrooms.findByCodeIgnoreCase("SE101-DEMO").orElseThrow().getId());
        team.setLecturer(lecturer); team.setLeader(leader);
        team.getMembers().add(leader); team.getMembers().add(member);
        team = teams.saveAndFlush(team);
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    String token(User user) { return "Bearer " + jwt.generateToken(user.getUsername(), user.getRole()); }
    String body() {
        return "{\"teamId\":" + team.getId() + ",\"title\":\"API checkpoint\",\"dueAt\":\"2099-01-01T12:00:00\",\"assigneeIds\":[" + member.getId() + "]}";
    }
    @Test void authenticatedLeaderCreatesWithoutRequestAttribute() throws Exception {
        mvc.perform(post("/api/v1/checkpoints").header("Authorization", token(leader))
                .contentType("application/json").content(body()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.createdBy").value(leader.getId()));
    }
    @Test void forgedActorCannotGrantLeaderPermission() throws Exception {
        mvc.perform(post("/api/v1/checkpoints").header("Authorization", token(member))
                .header("userId", leader.getId()).param("userId", leader.getId().toString())
                .requestAttr("userId", leader.getId()).contentType("application/json").content(body()))
                .andExpect(status().isForbidden());
    }
    @Test void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(get("/api/v1/checkpoints/team/" + team.getId()))
                .andExpect(status().is4xxClientError());
    }
}
