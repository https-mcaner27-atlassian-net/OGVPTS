package edu.maltepe.ogvpts.controller;

import edu.maltepe.ogvpts.dto.TaskDtos.CreateTaskRequest;
import edu.maltepe.ogvpts.dto.TaskDtos.FeedbackRequest;
import edu.maltepe.ogvpts.dto.TaskDtos.TaskDetail;
import edu.maltepe.ogvpts.dto.TaskDtos.TaskResponse;
import edu.maltepe.ogvpts.security.CurrentUserProvider;
import edu.maltepe.ogvpts.service.TaskService;
import edu.maltepe.ogvpts.service.TaskService.DownloadedFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "5. Görevler ve teslimler", description = "FR-13, FR-14, FR-15")
public class TaskController {

    private final TaskService taskService;
    private final CurrentUserProvider currentUser;

    public TaskController(TaskService taskService, CurrentUserProvider currentUser) {
        this.taskService = taskService;
        this.currentUser = currentUser;
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Danışman: gruba görev ata (FR-13)")
    public TaskResponse create(@Valid @RequestBody CreateTaskRequest req) {
        return taskService.create(currentUser.get(), req);
    }

    @GetMapping("/tasks")
    @Operation(summary = "Görevlerim (öğrenci: grubumun; danışman: atadıklarım)")
    public List<TaskResponse> list() {
        return taskService.list(currentUser.get());
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "Görev ayrıntısı: tüm teslimler ve geri bildirimler")
    public TaskDetail detail(@PathVariable Integer id) {
        return taskService.detail(currentUser.get(), id);
    }

    @PostMapping(value = "/tasks/{id}/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Öğrenci: dosya teslim et (FR-14) — PDF/DOC/DOCX/ZIP/PPTX, en fazla 20 MB")
    public TaskDetail submit(@PathVariable Integer id, @RequestParam("file") MultipartFile file) {
        return taskService.submit(currentUser.get(), id, file);
    }

    @GetMapping("/submissions/{id}/file")
    @Operation(summary = "Teslim dosyasını indir")
    public ResponseEntity<Resource> download(@PathVariable Integer id) {
        DownloadedFile f = taskService.download(currentUser.get(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(f.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(f.resource());
    }

    @PostMapping("/submissions/{id}/feedback")
    @Operation(summary = "Danışman: onayla veya düzeltme iste (FR-15)")
    public TaskDetail feedback(@PathVariable Integer id, @Valid @RequestBody FeedbackRequest req) {
        return taskService.giveFeedback(currentUser.get(), id, req);
    }
}
