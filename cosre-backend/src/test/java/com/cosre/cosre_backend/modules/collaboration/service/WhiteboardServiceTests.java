package com.cosre.cosre_backend.modules.collaboration.service;

import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.account.repository.UserRepository;
import com.cosre.cosre_backend.modules.collaboration.dto.SaveWhiteboardRequest;
import com.cosre.cosre_backend.modules.collaboration.entity.Whiteboard;
import com.cosre.cosre_backend.modules.collaboration.repository.WhiteboardRepository;
import com.cosre.cosre_backend.modules.team.service.TeamAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhiteboardServiceTests {

    @Mock private WhiteboardRepository whiteboardRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamAccessService teamAccessService;
    private WhiteboardService service;

    @BeforeEach
    void setUp() {
        service = new WhiteboardService(whiteboardRepository, userRepository, teamAccessService);
    }

    @Test
    void savesANewWhiteboardForAnAccessibleTeam() {
        User actor = new User();
        actor.setId(9L);
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(actor));
        when(whiteboardRepository.findByTeamId(2L)).thenReturn(Optional.empty());
        when(whiteboardRepository.save(any(Whiteboard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.save(2L, "student", new SaveWhiteboardRequest("{\"objects\":[]}", 0L));

        assertEquals(2L, response.teamId());
        assertEquals("{\"objects\":[]}", response.canvasData());
        verify(teamAccessService).requireViewAccess(2L, "student");
    }

    @Test
    void rejectsSavingAStaleVersion() {
        User actor = new User();
        actor.setId(9L);
        Whiteboard current = new Whiteboard();
        current.setTeamId(2L);
        current.setCanvasData("{}");
        setVersion(current, 3L);
        when(userRepository.findByUsername("student")).thenReturn(Optional.of(actor));
        when(whiteboardRepository.findByTeamId(2L)).thenReturn(Optional.of(current));

        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> service.save(2L, "student", new SaveWhiteboardRequest("{\"objects\":[]}", 2L)));
    }

    private void setVersion(Whiteboard whiteboard, Long version) {
        try {
            var field = Whiteboard.class.getDeclaredField("version");
            field.setAccessible(true);
            field.set(whiteboard, version);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
