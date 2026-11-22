package com.smart.instapreneur.Dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** DTO za ripoti za mmiliki. Zina taarifa za kifedha: HAZIFAI kuonyeshwa kwa wateja. */
public final class ReportDtos {
    private ReportDtos() {}

    /**
     * reliability = "ACTUAL" ikiwa gharama ya kila bidhaa iliyouzwa na kupotea inajulikana;
     * vinginevyo "ESTIMATE" (faida inaweza kuwa kubwa kuliko halisi kwa sababu gharama haikuhesabiwa).
     */
    public record ProfitReport(
            LocalDate from, LocalDate to,
            String reliability,
            BigDecimal salesRevenue,        // jumla ya oda zilizofikishwa kwenye kipindi
            BigDecimal returnsCredit,       // bidhaa zilizorudishwa (inapunguza mapato)
            BigDecimal costOfGoodsSold,     // gharama ya bidhaa zilizouzwa (baada ya returns nzima)
            BigDecimal grossProfit,         // mapato - returns - COGS
            BigDecimal inventoryLosses,     // bidhaa zilizoharibika/kupotea/tofauti za hesabu (kwa gharama)
            BigDecimal operatingExpenses,   // gharama za uendeshaji
            BigDecimal netProfit,           // gross - hasara - gharama za uendeshaji
            BigDecimal cashReceived,        // malipo yaliyohakikiwa kwenye kipindi (pesa halisi)
            BigDecimal outstandingReceivables, // madeni ya oda zilizofikishwa (leo)
            BigDecimal unverifiedPayments,  // malipo yaliyorekodiwa lakini hayajahakikiwa
            int linesMissingCost,
            int lossesMissingCost,
            List<String> formulas) {}

    public record ExceptionItem(String type, String reference, String description, BigDecimal amount, LocalDateTime date) {}

    public record ExceptionsReport(Map<String, Integer> counts, List<ExceptionItem> items) {}

    public record ExpenseRequest(String category, BigDecimal amount, String description, LocalDate expenseDate) {}

    public record ExpenseResponse(Long id, String category, BigDecimal amount, String description,
            LocalDate expenseDate, String recordedBy, LocalDateTime createdAt) {}
}
