package edu.maltepe.ogvpts.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class ReportDtos {

    private ReportDtos() {
    }

    /** Bir danışmanın dönem içindeki görüşmeleri: toplam ve duruma göre dağılım. */
    public record AdvisorMeetingRow(Integer advisorId, String advisorName, long total, Map<String, Long> byStatus) {
    }

    /** FR-11 raporu. */
    public record ReportResponse(LocalDate from, LocalDate to,
                                 List<AdvisorMeetingRow> meetingsPerAdvisor,
                                 Map<String, Long> projectStatus,
                                 long totalMeetings, long totalProjects) {
    }
}
