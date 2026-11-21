package edu.maltepe.ogvpts.service;

import edu.maltepe.ogvpts.dto.ReportDtos.AdvisorMeetingRow;
import edu.maltepe.ogvpts.dto.ReportDtos.ReportResponse;
import edu.maltepe.ogvpts.model.enums.MeetingStatus;
import edu.maltepe.ogvpts.model.enums.ProjectStatus;
import edu.maltepe.ogvpts.repository.MeetingRepository;
import edu.maltepe.ogvpts.repository.ProjectRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FR-11: seçilen dönem için danışman başına görüşme sayısı ve proje durum dağılımı.
 * Sayılar doğrudan veritabanından (GROUP BY ile) hesaplanır; böylece kayıtlarla birebir eşleşir.
 */
@Service
public class ReportService {

    /** Rapor tablolarında kullanılan Türkçe durum adları. */
    public static final Map<MeetingStatus, String> MEETING_LABELS = new LinkedHashMap<>();
    public static final Map<ProjectStatus, String> PROJECT_LABELS = new LinkedHashMap<>();

    static {
        MEETING_LABELS.put(MeetingStatus.BEKLEMEDE, "Beklemede");
        MEETING_LABELS.put(MeetingStatus.YENI_SAAT_ONERILDI, "Yeni saat önerildi");
        MEETING_LABELS.put(MeetingStatus.ONAYLANDI, "Onaylandı");
        MEETING_LABELS.put(MeetingStatus.TAMAMLANDI, "Tamamlandı");
        MEETING_LABELS.put(MeetingStatus.REDDEDILDI, "Reddedildi");
        MEETING_LABELS.put(MeetingStatus.IPTAL, "İptal");
        PROJECT_LABELS.put(ProjectStatus.BASLAMADI, "Başlamadı");
        PROJECT_LABELS.put(ProjectStatus.DEVAM_EDIYOR, "Devam ediyor");
        PROJECT_LABELS.put(ProjectStatus.TAMAMLANDI, "Tamamlandı");
    }

    private final MeetingRepository meetings;
    private final ProjectRepository projects;

    public ReportService(MeetingRepository meetings, ProjectRepository projects) {
        this.meetings = meetings;
        this.projects = projects;
    }

    @Transactional(readOnly = true)
    public ReportResponse build(LocalDate from, LocalDate to) {
        // [from, to] kapalı aralık → sorguda [from 00:00, to+1 00:00)
        List<Object[]> rows = meetings.countByAdvisorAndStatus(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        Map<Integer, AdvisorAccumulator> byAdvisor = new LinkedHashMap<>();
        long totalMeetings = 0;
        for (Object[] r : rows) {
            Integer advisorId = (Integer) r[0];
            String name = (String) r[1];
            MeetingStatus status = (MeetingStatus) r[2];
            long count = ((Number) r[3]).longValue();
            byAdvisor.computeIfAbsent(advisorId, id -> new AdvisorAccumulator(id, name)).add(status, count);
            totalMeetings += count;
        }
        List<AdvisorMeetingRow> advisorRows = byAdvisor.values().stream()
                .map(AdvisorAccumulator::toRow)
                .sorted(Comparator.comparing(AdvisorMeetingRow::advisorName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<String, Long> projectStatus = new LinkedHashMap<>();
        PROJECT_LABELS.keySet().forEach(s -> projectStatus.put(s.name(), 0L));
        long totalProjects = 0;
        for (Object[] r : projects.countByStatus()) {
            long c = ((Number) r[1]).longValue();
            projectStatus.put(((ProjectStatus) r[0]).name(), c);
            totalProjects += c;
        }
        return new ReportResponse(from, to, advisorRows, projectStatus, totalMeetings, totalProjects);
    }

    /** Raporun Excel (.xlsx) hali: iki sayfa — Görüşmeler ve Projeler. */
    @Transactional(readOnly = true)
    public byte[] toExcel(LocalDate from, LocalDate to) {
        ReportResponse report = build(from, to);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle bold = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            bold.setFont(font);

            Sheet s1 = wb.createSheet("Görüşmeler");
            int r = 0;
            Row title = s1.createRow(r++);
            cell(title, 0, "Danışman başına görüşmeler (" + from + " – " + to + ")", bold);
            r++;
            Row header = s1.createRow(r++);
            List<String> headers = new ArrayList<>(List.of("Danışman", "Toplam"));
            headers.addAll(MEETING_LABELS.values());
            for (int i = 0; i < headers.size(); i++) {
                cell(header, i, headers.get(i), bold);
            }
            for (AdvisorMeetingRow row : report.meetingsPerAdvisor()) {
                Row x = s1.createRow(r++);
                cell(x, 0, row.advisorName(), null);
                x.createCell(1).setCellValue(row.total());
                int c = 2;
                for (MeetingStatus st : MEETING_LABELS.keySet()) {
                    x.createCell(c++).setCellValue(row.byStatus().getOrDefault(st.name(), 0L));
                }
            }
            Row total = s1.createRow(r);
            cell(total, 0, "Toplam", bold);
            total.createCell(1).setCellValue(report.totalMeetings());
            for (int i = 0; i < headers.size(); i++) {
                s1.autoSizeColumn(i);
            }

            Sheet s2 = wb.createSheet("Projeler");
            r = 0;
            cell(s2.createRow(r++), 0, "Proje durum dağılımı", bold);
            r++;
            Row h2 = s2.createRow(r++);
            cell(h2, 0, "Durum", bold);
            cell(h2, 1, "Proje sayısı", bold);
            for (ProjectStatus st : PROJECT_LABELS.keySet()) {
                Row x = s2.createRow(r++);
                cell(x, 0, PROJECT_LABELS.get(st), null);
                x.createCell(1).setCellValue(report.projectStatus().getOrDefault(st.name(), 0L));
            }
            Row t2 = s2.createRow(r);
            cell(t2, 0, "Toplam", bold);
            t2.createCell(1).setCellValue(report.totalProjects());
            s2.autoSizeColumn(0);
            s2.autoSizeColumn(1);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Excel raporu oluşturulamadı", e);
        }
    }

    private static void cell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        if (style != null) {
            c.setCellStyle(style);
        }
    }

    /** Bir danışmanın satırını biriktiren yardımcı. */
    private static final class AdvisorAccumulator {
        private final Integer id;
        private final String name;
        private final Map<String, Long> byStatus = new LinkedHashMap<>();
        private long total;

        AdvisorAccumulator(Integer id, String name) {
            this.id = id;
            this.name = name;
            MEETING_LABELS.keySet().forEach(s -> byStatus.put(s.name(), 0L));
        }

        void add(MeetingStatus status, long count) {
            byStatus.merge(status.name(), count, Long::sum);
            total += count;
        }

        AdvisorMeetingRow toRow() {
            return new AdvisorMeetingRow(id, name, total, byStatus);
        }
    }
}
