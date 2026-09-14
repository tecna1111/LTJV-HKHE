package com.cosre.cosre_backend.modules.syllabus.controller;

import com.cosre.cosre_backend.common.dto.ApiResponse;
import com.cosre.cosre_backend.modules.syllabus.dto.*;
import com.cosre.cosre_backend.modules.syllabus.service.SyllabusService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/syllabi")
public class SyllabusController {
    private final SyllabusService service;
    public SyllabusController(SyllabusService service) { this.service = service; }
    @GetMapping @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<SyllabusResponse>> list(@RequestParam Long subjectId) { return ok("Syllabi loaded", service.list(subjectId).stream().map(SyllabusResponse::from).toList()); }
    @GetMapping("/{id}") @PreAuthorize("isAuthenticated()")
    public ApiResponse<SyllabusResponse> get(@PathVariable Long id) { return ok("Syllabus loaded", SyllabusResponse.from(service.get(id))); }
    @PostMapping @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ApiResponse<SyllabusResponse>> create(@Valid @RequestBody SyllabusRequest request) { return ResponseEntity.status(201).body(ok("Syllabus created", SyllabusResponse.from(service.create(request)))); }
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<SyllabusResponse> update(@PathVariable Long id, @Valid @RequestBody SyllabusRequest request) { return ok("Syllabus updated", SyllabusResponse.from(service.update(id, request))); }
    @PutMapping("/{id}/status") @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<SyllabusResponse> status(@PathVariable Long id, @RequestParam boolean active) { return ok("Syllabus status updated", SyllabusResponse.from(service.setActive(id, active))); }
    private <T> ApiResponse<T> ok(String message, T data) { return new ApiResponse<>(true, message, data); }
}
