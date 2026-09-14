package com.cosre.cosre_backend.modules.incident.controller;
import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.incident.entity.IncidentReport;
import com.cosre.cosre_backend.modules.incident.service.IncidentReportService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
@RestController @RequestMapping("/api/v1/incidents") @RequiredArgsConstructor
public class IncidentReportController {
    private final IncidentReportService service;
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ApiResponse<IncidentReport> create(@Valid @RequestBody IncidentReportService.CreateRequest request) { return ApiResponse.success(service.create(request)); }
    @GetMapping public ApiResponse<List<IncidentReport>> list() { return ApiResponse.success(service.list()); }
    @GetMapping("/{id}") public ApiResponse<IncidentReport> get(@PathVariable Long id) { return ApiResponse.success(service.get(id)); }
    @PatchMapping("/{id}/status") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<IncidentReport> update(@PathVariable Long id, @Valid @RequestBody IncidentReportService.UpdateRequest request) { return ApiResponse.success(service.update(id, request)); }
}
