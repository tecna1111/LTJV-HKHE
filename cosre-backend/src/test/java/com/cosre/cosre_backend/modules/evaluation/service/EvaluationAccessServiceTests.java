package com.cosre.cosre_backend.modules.evaluation.service;
import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.project.repository.ProjectRepository;
import com.cosre.cosre_backend.modules.team.entity.Team;
import com.cosre.cosre_backend.modules.team.repository.TeamRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class EvaluationAccessServiceTests {
    @Mock TeamRepository teams; @Mock ProjectRepository projects; @Mock UserRepository users; @Mock EntityManager em;
    @InjectMocks EvaluationAccessService service;
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void rejectsStudentOutsideTeam() {
        assertThatThrownBy(() -> service.member(new Team(),9L)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void rejectsWrongProject() {
        var team=new Team();team.setProjectId(5L);when(teams.findById(2L)).thenReturn(Optional.of(team));
        assertThatThrownBy(() -> service.team(2L,1L)).hasMessageContaining("project");
    }
    @Test void rejectsUnassignedLecturer() {
        var owner=new User();owner.setId(9L);
        var user=new User();user.setId(8L);user.setRole(RoleEnum.LECTURER);
        var team=new Team();team.setProjectId(1L);team.setLecturer(owner);
        when(teams.findById(2L)).thenReturn(Optional.of(team));when(users.findByUsername("other")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("other","unused",List.of()));
        assertThatThrownBy(() -> service.lecturer(2L,1L)).isInstanceOf(AccessDeniedException.class);
    }
}
