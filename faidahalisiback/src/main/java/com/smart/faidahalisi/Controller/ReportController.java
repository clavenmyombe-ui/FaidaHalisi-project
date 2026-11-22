package com.smart.faidahalisi.Controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smart.faidahalisi.Dto.ReportDtos;
import com.smart.faidahalisi.Model.AuditLog;
import com.smart.faidahalisi.Model.Discrepancy;
import com.smart.faidahalisi.Repository.AuditLogRepository;
import com.smart.faidahalisi.Service.DiscrepancyService;
import com.smart.faidahalisi.Service.ReportService;

/**
 * Ripoti za kifedha, gharama, audit na tofauti. Zote ni za mmiliki wa duka (au admin) tu;
 * mteja kamwe haziioni.
 */
@RestController
@RequestMapping("/api")
public class ReportController {
    private static final String OWNER_OF_STORE = "hasRole('ADMIN') or (hasRole('SELLER') and @ownership.isStoreOwner(#p0, authentication))";

    private final ReportService reports;
    private final AuditLogRepository auditRepository;
    private final DiscrepancyService discrepancies;

    public ReportController(ReportService reports, AuditLogRepository auditRepository, DiscrepancyService discrepancies) {
        this.reports = reports;
        this.auditRepository = auditRepository;
        this.discrepancies = discrepancies;
    }

    // from/to ni tarehe (yyyy-MM-dd). Chaguo-msingi: mwezi wa sasa.
    @GetMapping("/reports/store/{storeId}/profit")
    @PreAuthorize(OWNER_OF_STORE)
    public ReportDtos.ProfitReport profit(@PathVariable Long storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.withDayOfMonth(1);
        return reports.profit(storeId, start, end);
    }

    @GetMapping("/reports/store/{storeId}/exceptions")
    @PreAuthorize(OWNER_OF_STORE)
    public ReportDtos.ExceptionsReport exceptions(@PathVariable Long storeId) {
        return reports.exceptions(storeId);
    }

    @PostMapping("/expenses/store/{storeId}")
    @PreAuthorize(OWNER_OF_STORE)
    public ReportDtos.ExpenseResponse addExpense(@PathVariable Long storeId,
            @RequestBody ReportDtos.ExpenseRequest request, Authentication authentication) {
        return reports.addExpense(storeId, request, authentication.getName());
    }

    @GetMapping("/expenses/store/{storeId}")
    @PreAuthorize(OWNER_OF_STORE)
    public List<ReportDtos.ExpenseResponse> expenses(@PathVariable Long storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.withDayOfMonth(1);
        return reports.listExpenses(storeId, start, end);
    }

    // Audit trail: rekodi 200 za hivi karibuni za duka. Hakuna njia ya kuhariri au kufuta kupitia API.
    @GetMapping("/audit/store/{storeId}")
    @PreAuthorize(OWNER_OF_STORE)
    public List<AuditLog> audit(@PathVariable Long storeId) {
        return auditRepository.findTop200ByStoreIdOrderByCreatedAtDesc(storeId);
    }

    @GetMapping("/discrepancies/store/{storeId}")
    @PreAuthorize(OWNER_OF_STORE)
    public List<Discrepancy> discrepancyList(@PathVariable Long storeId) {
        return discrepancies.list(storeId);
    }

    @PostMapping("/discrepancies/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SELLER') and @ownership.isDiscrepancyOwner(#p0, authentication))")
    public Discrepancy resolve(@PathVariable Long id, @RequestBody ResolveRequest request, Authentication authentication) {
        return discrepancies.resolve(id, request.note(), authentication.getName());
    }

    public record ResolveRequest(String note) {}
}
