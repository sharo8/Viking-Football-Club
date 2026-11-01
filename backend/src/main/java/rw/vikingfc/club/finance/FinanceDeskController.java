package rw.vikingfc.club.finance;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.vikingfc.club.common.api.ApiResponse;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EXECUTIVE')")
public class FinanceDeskController {

    private final FinanceOfficeService office;
    private final FinanceService financeService;

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.ok("Finance overview", office.overview());
    }

    @GetMapping("/analytics/{chartId}")
    public ApiResponse<Map<String, Object>> chart(@PathVariable String chartId, @RequestParam(required = false) String group, @RequestParam(required = false) String groupBy) {
        return ApiResponse.ok("Finance chart", office.chart(chartId, group, groupBy));
    }

    @GetMapping("/expenses")
    public ApiResponse<List<Map<String, Object>>> expenses() {
        return ApiResponse.ok("Expenses", office.expenses());
    }

    @PostMapping("/expenses")
    public ApiResponse<Map<String, Object>> saveExpense(@Valid @RequestBody FinanceDtos.ExpenseRequest request) {
        return ApiResponse.ok("Expense recorded", office.saveExpense(request));
    }

    @PostMapping("/expenses/{id}/reverse")
    public ApiResponse<Map<String, Object>> reverseExpense(@PathVariable Long id, @Valid @RequestBody FinanceDtos.ReverseRequest request) {
        return ApiResponse.ok("Expense reversed", office.reverseExpense(id, request.reason()));
    }

    @GetMapping("/people/{type}/{id}")
    public ApiResponse<Map<String, Object>> dossier(@PathVariable String type, @PathVariable Long id) {
        return ApiResponse.ok("Person finance", office.dossier(type, id));
    }

    @PostMapping("/people/{type}/{id}/contract")
    public ApiResponse<Map<String, Object>> contract(@PathVariable String type, @PathVariable Long id, @Valid @RequestBody FinanceDtos.ContractPayRequest request) {
        return ApiResponse.ok("Contract pay saved", financeService.updatePay(type, id, request.seasonalPay(), request.advance(), request.sfExempt(), request.reason()));
    }

    @PostMapping("/receipts")
    public ApiResponse<Map<String, Object>> receipt(@Valid @RequestBody FinanceDtos.ReceiptRequest request) {
        return ApiResponse.ok("Receipt created", office.receipt(request));
    }

    @GetMapping("/receipts/{id}/pdf")
    public ResponseEntity<byte[]> receiptPdf(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"receipt-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(office.receiptPdf(id));
    }

    @GetMapping("/receipts/verify/{number}")
    public ApiResponse<Map<String, Object>> verify(@PathVariable String number) {
        return ApiResponse.ok("Receipt", office.verify(number));
    }

    @GetMapping("/reports")
    public ApiResponse<List<Map<String, Object>>> reports() {
        return ApiResponse.ok("Reports", office.reports());
    }

    @PostMapping("/exports")
    public ResponseEntity<byte[]> export(@RequestBody FinanceDtos.ExportRequest request) {
        FinanceOfficeService.ExportFile file = office.export(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.parseMediaType(file.type()))
                .body(file.bytes());
    }
}
