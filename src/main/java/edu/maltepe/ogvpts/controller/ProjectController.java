package edu.maltepe.ogvpts.controller;

import edu.maltepe.ogvpts.dto.ProjectDtos.CommentRequest;
import edu.maltepe.ogvpts.dto.ProjectDtos.CreateProjectRequest;
import edu.maltepe.ogvpts.dto.ProjectDtos.MilestoneRequest;
import edu.maltepe.ogvpts.dto.ProjectDtos.ProjectResponse;
import edu.maltepe.ogvpts.dto.ProjectDtos.ProjectSummary;
import edu.maltepe.ogvpts.dto.ProjectDtos.UpdateMilestoneStatusRequest;
import edu.maltepe.ogvpts.dto.ProjectDtos.UpdateProjectRequest;
import edu.maltepe.ogvpts.security.CurrentUser;
import edu.maltepe.ogvpts.security.CurrentUserProvider;
import edu.maltepe.ogvpts.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "3. Projeler", description = "FR-05, FR-06, FR-07")
public class ProjectController {

    private final ProjectService projectService;
    private final CurrentUserProvider currentUser;

    public ProjectController(ProjectService projectService, CurrentUserProvider currentUser) {
        this.projectService = projectService;
        this.currentUser = currentUser;
    }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Proje oluştur: ad, açıklama, en az bir aşama (FR-05)")
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest req) {
        return projectService.create(currentUser.get(), req);
    }

    @GetMapping("/projects/mine")
    @Operation(summary = "Öğrencinin grubunun projesi (yoksa 204)")
    public ResponseEntity<ProjectResponse> mine() {
        return projectService.mine(currentUser.get())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/projects")
    @Operation(summary = "Danışman: kendi gruplarının projeleri; yönetici: tüm projeler (FR-07)")
    public List<ProjectSummary> list() {
        CurrentUser cu = currentUser.get();
        return cu.isAdmin() ? projectService.listAll() : projectService.listForAdvisor(cu);
    }

    @GetMapping("/projects/{id}")
    @Operation(summary = "Proje ayrıntısı: aşamalar ve yorumlar")
    public ProjectResponse get(@PathVariable Integer id) {
        return projectService.get(currentUser.get(), id);
    }

    @PutMapping("/projects/{id}")
    @Operation(summary = "Proje adı/açıklamasını güncelle")
    public ProjectResponse update(@PathVariable Integer id, @Valid @RequestBody UpdateProjectRequest req) {
        return projectService.update(currentUser.get(), id, req);
    }

    @PostMapping("/projects/{id}/milestones")
    @Operation(summary = "Aşama ekle")
    public ProjectResponse addMilestone(@PathVariable Integer id, @Valid @RequestBody MilestoneRequest req) {
        return projectService.addMilestone(currentUser.get(), id, req);
    }

    @PatchMapping("/milestones/{id}/status")
    @Operation(summary = "Aşama durumunu güncelle (FR-06)")
    public ProjectResponse updateMilestone(@PathVariable Integer id, @Valid @RequestBody UpdateMilestoneStatusRequest req) {
        return projectService.updateMilestoneStatus(currentUser.get(), id, req.status());
    }

    @DeleteMapping("/milestones/{id}")
    @Operation(summary = "Aşamayı sil (en az bir aşama kalmalı)")
    public ProjectResponse deleteMilestone(@PathVariable Integer id) {
        return projectService.deleteMilestone(currentUser.get(), id);
    }

    @PostMapping("/projects/{id}/comments")
    @Operation(summary = "Danışman yorumu (FR-07)")
    public ProjectResponse comment(@PathVariable Integer id, @Valid @RequestBody CommentRequest req) {
        return projectService.addComment(currentUser.get(), id, req);
    }
}
