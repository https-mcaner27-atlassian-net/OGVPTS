package edu.maltepe.ogvpts.controller;

import edu.maltepe.ogvpts.dto.GroupDtos.AddMemberRequest;
import edu.maltepe.ogvpts.dto.GroupDtos.CreateGroupRequest;
import edu.maltepe.ogvpts.dto.GroupDtos.GroupResponse;
import edu.maltepe.ogvpts.security.CurrentUserProvider;
import edu.maltepe.ogvpts.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@Tag(name = "2. Proje grupları", description = "FR-12")
public class GroupController {

    private final GroupService groupService;
    private final CurrentUserProvider currentUser;

    public GroupController(GroupService groupService, CurrentUserProvider currentUser) {
        this.groupService = groupService;
        this.currentUser = currentUser;
    }

    @GetMapping("/mine")
    @Operation(summary = "Öğrencinin grubu (yoksa 204)")
    public ResponseEntity<GroupResponse> mine() {
        return groupService.myGroup(currentUser.get())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Grup oluştur (FR-12)")
    public GroupResponse create(@Valid @RequestBody CreateGroupRequest req) {
        return groupService.create(currentUser.get(), req);
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Gruba öğrenci ekle (en fazla 2 kişi)")
    public GroupResponse addMember(@PathVariable Integer id, @Valid @RequestBody AddMemberRequest req) {
        return groupService.addMember(currentUser.get(), id, req);
    }

    @GetMapping
    @Operation(summary = "Danışmanın sorumlu olduğu gruplar")
    public List<GroupResponse> advisorGroups() {
        return groupService.advisorGroups(currentUser.get());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Grup ayrıntısı (yetki kontrollü)")
    public GroupResponse get(@PathVariable Integer id) {
        return groupService.get(currentUser.get(), id);
    }
}
