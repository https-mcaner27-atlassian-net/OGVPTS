package edu.maltepe.ogvpts.controller;

import edu.maltepe.ogvpts.dto.GroupDtos.AdvisorLoad;
import edu.maltepe.ogvpts.dto.GroupDtos.AssignAdvisorRequest;
import edu.maltepe.ogvpts.dto.GroupDtos.GroupResponse;
import edu.maltepe.ogvpts.dto.ReportDtos.ReportResponse;
import edu.maltepe.ogvpts.dto.UserDtos.CreateUserRequest;
import edu.maltepe.ogvpts.dto.UserDtos.ResetPasswordRequest;
import edu.maltepe.ogvpts.dto.UserDtos.UserAdminView;
import edu.maltepe.ogvpts.dto.UserDtos.UserSummary;
import edu.maltepe.ogvpts.model.enums.UserRole;
import edu.maltepe.ogvpts.service.AdminService;
import edu.maltepe.ogvpts.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Yönetici ekranları (ROL-03). SecurityConfig: /api/admin/** → yalnızca YONETICI. */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "7. Yönetici", description = "FR-10, FR-11, kullanıcı yönetimi")
public class AdminController {

    private final AdminService adminService;
    private final ReportService reportService;

    public AdminController(AdminService adminService, ReportService reportService) {
        this.adminService = adminService;
        this.reportService = reportService;
    }

    @GetMapping("/users")
    public List<UserAdminView> users(@RequestParam(required = false) UserRole role) {
        return adminService.listUsers(role);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Kullanıcı oluştur (öğrenci / danışman / yönetici)")
    public UserSummary createUser(@Valid @RequestBody CreateUserRequest req) {
        return adminService.createUser(req);
    }

    @PutMapping("/users/{id}/password")
    @Operation(summary = "Kullanıcının şifresini sıfırla")
    public ResponseEntity<Void> resetPassword(@PathVariable Integer id, @Valid @RequestBody ResetPasswordRequest req) {
        adminService.resetPassword(id, req.password());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/groups")
    public List<GroupResponse> groups() {
        return adminService.listGroups();
    }

    @GetMapping("/advisors")
    @Operation(summary = "Danışmanlar ve kontenjan doluluğu")
    public List<AdvisorLoad> advisors() {
        return adminService.advisorLoads();
    }

    @PutMapping("/groups/{id}/advisor")
    @Operation(summary = "Gruba danışman ata / değiştir (FR-10, BR-01)")
    public GroupResponse assignAdvisor(@PathVariable Integer id, @Valid @RequestBody AssignAdvisorRequest req) {
        return adminService.assignAdvisor(id, req);
    }

    @GetMapping("/reports")
    @Operation(summary = "Dönem raporu (FR-11)")
    public ReportResponse report(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.build(from, to);
    }

    @GetMapping("/reports/excel")
    @Operation(summary = "Dönem raporu — Excel (FR-11)")
    public ResponseEntity<byte[]> reportExcel(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] bytes = reportService.toExcel(from, to);
        String name = "ogvpts-rapor-" + from + "-" + to + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
                .body(bytes);
    }
}
