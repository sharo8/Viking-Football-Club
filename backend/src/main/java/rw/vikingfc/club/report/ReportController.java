package rw.vikingfc.club.report;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import rw.vikingfc.club.common.api.ApiResponse;
import rw.vikingfc.club.finance.FinanceService;
import rw.vikingfc.club.medical.MedicalService;
import rw.vikingfc.club.partner.PartnerService;
import rw.vikingfc.club.sanction.FineService;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Reports")
public class ReportController {

    private final ReportService reportService;
    private final WorkbookImportService importService;
    private final FinanceService financeService;
    private final FineService fineService;
    private final MedicalService medicalService;
    private final PartnerService partnerService;

    @GetMapping("/reports/workbook")
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ResponseEntity<byte[]> workbook() {
        return file("viking-fc-2026-2027.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", reportService.workbook());
    }

    @GetMapping(value = "/finance/players/{id}/payslip", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ResponseEntity<byte[]> payslip(@PathVariable Long id) {
        return file("payslip-" + id + ".pdf", MediaType.APPLICATION_PDF_VALUE, reportService.payslip(id));
    }

    @GetMapping(value = "/reports/fines.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('EXECUTIVE','ASSISTANT_MANAGER','HEAD_COACH')")
    public ResponseEntity<byte[]> fines() {
        return file("fines.pdf", MediaType.APPLICATION_PDF_VALUE,
                reportService.textReport("Fines statement", String.valueOf(fineService.summary())));
    }

    @GetMapping(value = "/reports/medical.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('EXECUTIVE','DOCTOR')")
    public ResponseEntity<byte[]> medical() {
        return file("injuries.pdf", MediaType.APPLICATION_PDF_VALUE,
                reportService.textReport("Injury report", String.valueOf(medicalService.dashboard())));
    }

    @GetMapping(value = "/reports/partners.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('EXECUTIVE','MARKETING_MANAGER')")
    public ResponseEntity<byte[]> partners() {
        return file("partners.pdf", MediaType.APPLICATION_PDF_VALUE,
                reportService.textReport("Partner report", String.valueOf(partnerService.report())));
    }

    @GetMapping(value = "/reports/season.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ResponseEntity<byte[]> season() {
        return file("season.pdf", MediaType.APPLICATION_PDF_VALUE,
                reportService.textReport("Season report", String.valueOf(financeService.summary())));
    }

    @PostMapping(value = "/admin/import/workbook", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EXECUTIVE')")
    public ApiResponse<Map<String, Object>> importWorkbook(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok("Workbook imported", importService.importWorkbook(file));
    }

    private ResponseEntity<byte[]> file(String name, String type, byte[] body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(MediaType.parseMediaType(type))
                .body(body);
    }
}
