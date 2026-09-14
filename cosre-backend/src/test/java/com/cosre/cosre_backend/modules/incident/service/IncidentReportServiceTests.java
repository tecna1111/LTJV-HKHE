package com.cosre.cosre_backend.modules.incident.service;
import com.cosre.cosre_backend.common.constants.RoleEnum;
import com.cosre.cosre_backend.modules.account.entity.User;
import com.cosre.cosre_backend.modules.evaluation.service.EvaluationAccessService;
import com.cosre.cosre_backend.modules.incident.entity.IncidentReport;
import com.cosre.cosre_backend.modules.incident.repository.IncidentReportRepository;
import com.cosre.cosre_backend.modules.incident.event.IncidentReportedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class IncidentReportServiceTests {
    @Mock IncidentReportRepository reports; @Mock EvaluationAccessService access; @Mock ApplicationEventPublisher events;
    @InjectMocks IncidentReportService service;
    User user;
    @BeforeEach void setup() { user=new User();user.setId(1L);user.setRole(RoleEnum.STUDENT);when(access.currentUser()).thenReturn(user); }
    @Test void createsReportAndPublishesAdminEmailEvent() {
        when(reports.save(any())).thenAnswer(i -> { IncidentReport r=i.getArgument(0);r.setId(7L);return r; });
        var report=service.create(new IncidentReportService.CreateRequest("Upload failed","Cannot submit file"));
        assertThat(report.getStatus()).isEqualTo(IncidentReport.Status.OPEN);
        verify(events).publishEvent(any(IncidentReportedEvent.class));
    }
    @Test void studentCannotHandleReport() {
        assertThatThrownBy(() -> service.update(7L,new IncidentReportService.UpdateRequest(IncidentReport.Status.IN_PROGRESS,null))).isInstanceOf(AccessDeniedException.class);
    }
    @Test void cannotReadOtherReport() {
        var report=new IncidentReport();report.setReporterId(2L);when(reports.findById(7L)).thenReturn(Optional.of(report));
        assertThatThrownBy(() -> service.get(7L)).isInstanceOf(AccessDeniedException.class);
    }
    @Test void adminCannotSkipHandlingStatus() {
        user.setRole(RoleEnum.ADMIN);var report=new IncidentReport();report.setStatus(IncidentReport.Status.OPEN);
        when(reports.findById(7L)).thenReturn(Optional.of(report));
        assertThatThrownBy(() -> service.update(7L,new IncidentReportService.UpdateRequest(IncidentReport.Status.CLOSED,"Fixed"))).hasMessageContaining("transition");
    }
    @Test void resolutionRequired() {
        user.setRole(RoleEnum.ADMIN);var report=new IncidentReport();report.setStatus(IncidentReport.Status.IN_PROGRESS);
        when(reports.findById(7L)).thenReturn(Optional.of(report));
        assertThatThrownBy(() -> service.update(7L,new IncidentReportService.UpdateRequest(IncidentReport.Status.RESOLVED,null))).hasMessageContaining("Resolution");
    }
}
