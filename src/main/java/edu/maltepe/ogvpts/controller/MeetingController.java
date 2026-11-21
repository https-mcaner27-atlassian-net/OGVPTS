package edu.maltepe.ogvpts.controller;

import edu.maltepe.ogvpts.dto.MeetingDtos.MeetingRequest;
import edu.maltepe.ogvpts.dto.MeetingDtos.MeetingResponse;
import edu.maltepe.ogvpts.dto.MeetingDtos.NoteRequest;
import edu.maltepe.ogvpts.dto.MeetingDtos.ProposeRequest;
import edu.maltepe.ogvpts.dto.MeetingDtos.SlotRequest;
import edu.maltepe.ogvpts.dto.MeetingDtos.SlotResponse;
import edu.maltepe.ogvpts.model.enums.MeetingStatus;
import edu.maltepe.ogvpts.security.CurrentUserProvider;
import edu.maltepe.ogvpts.service.AvailabilityService;
import edu.maltepe.ogvpts.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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

@RestController
@RequestMapping("/api")
@Tag(name = "4. Müsaitlik ve görüşmeler", description = "FR-02, FR-03, FR-04, FR-09, BR-02, BR-03, BR-04")
public class MeetingController {

    private final AvailabilityService availability;
    private final MeetingService meetings;
    private final CurrentUserProvider currentUser;

    public MeetingController(AvailabilityService availability, MeetingService meetings, CurrentUserProvider currentUser) {
        this.availability = availability;
        this.meetings = meetings;
        this.currentUser = currentUser;
    }

    // ---- müsaitlik saatleri

    @GetMapping("/slots/mine")
    @Operation(summary = "Danışman: kendi saatlerim ve doluluk durumu")
    public List<SlotResponse> mySlots() {
        return availability.mySlots(currentUser.get());
    }

    @GetMapping("/slots/mine/free")
    @Operation(summary = "Danışman: boş saatlerim (yeni saat önermek için)")
    public List<SlotResponse> myFreeSlots() {
        return availability.myFreeSlots(currentUser.get());
    }

    @PostMapping("/slots")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Danışman: müsait saat ekle")
    public SlotResponse createSlot(@Valid @RequestBody SlotRequest req) {
        return availability.create(currentUser.get(), req);
    }

    @DeleteMapping("/slots/{id}")
    @Operation(summary = "Danışman: müsait saati sil")
    public ResponseEntity<Void> deleteSlot(@PathVariable Integer id) {
        availability.delete(currentUser.get(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/slots/available")
    @Operation(summary = "Öğrenci: danışmanımın boş saatleri (FR-02)")
    public List<SlotResponse> available() {
        return availability.availableForStudent(currentUser.get());
    }

    // ---- görüşmeler

    @GetMapping("/meetings")
    @Operation(summary = "Görüşmelerim (FR-09) — durum ve tarih filtresi isteğe bağlı")
    public List<MeetingResponse> list(
            @RequestParam(required = false) MeetingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return meetings.list(currentUser.get(), status, from, to);
    }

    @GetMapping("/meetings/{id}")
    public MeetingResponse get(@PathVariable Integer id) {
        return meetings.get(currentUser.get(), id);
    }

    @PostMapping("/meetings")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Öğrenci: randevu talep et (FR-02)")
    public MeetingResponse request(@Valid @RequestBody MeetingRequest req) {
        return meetings.request(currentUser.get(), req);
    }

    @PostMapping("/meetings/{id}/approve")
    @Operation(summary = "Danışman: onayla (FR-03, BR-02)")
    public MeetingResponse approve(@PathVariable Integer id) {
        return meetings.approve(currentUser.get(), id);
    }

    @PostMapping("/meetings/{id}/reject")
    @Operation(summary = "Danışman: reddet (FR-03)")
    public MeetingResponse reject(@PathVariable Integer id) {
        return meetings.reject(currentUser.get(), id);
    }

    @PostMapping("/meetings/{id}/propose")
    @Operation(summary = "Danışman: yeni saat öner (FR-03)")
    public MeetingResponse propose(@PathVariable Integer id, @Valid @RequestBody ProposeRequest req) {
        return meetings.proposeNewTime(currentUser.get(), id, req.slotId());
    }

    @PostMapping("/meetings/{id}/accept-proposal")
    @Operation(summary = "Öğrenci: önerilen saati kabul et")
    public MeetingResponse acceptProposal(@PathVariable Integer id) {
        return meetings.acceptProposal(currentUser.get(), id);
    }

    @PostMapping("/meetings/{id}/decline-proposal")
    @Operation(summary = "Öğrenci: önerilen saati reddet")
    public MeetingResponse declineProposal(@PathVariable Integer id) {
        return meetings.declineProposal(currentUser.get(), id);
    }

    @PostMapping("/meetings/{id}/cancel")
    @Operation(summary = "İptal (onaylıysa en az 24 saat önce — BR-03)")
    public MeetingResponse cancel(@PathVariable Integer id) {
        return meetings.cancel(currentUser.get(), id);
    }

    @PostMapping("/meetings/{id}/complete")
    @Operation(summary = "Danışman: görüşme yapıldı olarak işaretle")
    public MeetingResponse complete(@PathVariable Integer id) {
        return meetings.complete(currentUser.get(), id);
    }

    @PutMapping("/meetings/{id}/note")
    @Operation(summary = "Danışman: görüşme notu ve kararlar (FR-04, BR-04)")
    public MeetingResponse saveNote(@PathVariable Integer id, @Valid @RequestBody NoteRequest req) {
        return meetings.saveNote(currentUser.get(), id, req);
    }
}
