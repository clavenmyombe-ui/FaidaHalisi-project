package com.smart.faidahalisi.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smart.faidahalisi.Dto.ReportDtos;
import com.smart.faidahalisi.Exception.IllegalArgumentException;
import com.smart.faidahalisi.Exception.ResourceNotFoundException;
import com.smart.faidahalisi.Model.Discrepancy;
import com.smart.faidahalisi.Model.Expense;
import com.smart.faidahalisi.Model.InventoryMovement;
import com.smart.faidahalisi.Model.MovementType;
import com.smart.faidahalisi.Model.OrderItems;
import com.smart.faidahalisi.Model.OrderReturn;
import com.smart.faidahalisi.Model.Orders;
import com.smart.faidahalisi.Model.Payment;
import com.smart.faidahalisi.Model.Store;
import com.smart.faidahalisi.Repository.DiscrepancyRepository;
import com.smart.faidahalisi.Repository.ExpenseRepository;
import com.smart.faidahalisi.Repository.InventoryMovementRepository;
import com.smart.faidahalisi.Repository.OrderReturnRepository;
import com.smart.faidahalisi.Repository.OrdersRepository;
import com.smart.faidahalisi.Repository.PaymentRepository;
import com.smart.faidahalisi.Repository.StoreRepository;

/**
 * Ripoti za mmiliki. Kila namba ina fomula iliyoandikwa wazi (angalia ProfitReport.formulas).
 * Tunatenganisha kwa makusudi: mapato (revenue) != pesa iliyopokelewa (cash) != faida (profit).
 */
@Service
public class ReportService {
    private final OrdersRepository ordersRepository;
    private final OrderReturnRepository returnRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryMovementRepository movementRepository;
    private final ExpenseRepository expenseRepository;
    private final DiscrepancyRepository discrepancyRepository;
    private final StoreRepository storeRepository;
    private final OrderFinanceService finance;
    private final AuditService audit;
    private final int overdueDays;

    public ReportService(OrdersRepository ordersRepository, OrderReturnRepository returnRepository,
            PaymentRepository paymentRepository, InventoryMovementRepository movementRepository,
            ExpenseRepository expenseRepository, DiscrepancyRepository discrepancyRepository,
            StoreRepository storeRepository, OrderFinanceService finance, AuditService audit,
            @Value("${app.business.overdue-days:7}") int overdueDays) {
        this.ordersRepository = ordersRepository;
        this.returnRepository = returnRepository;
        this.paymentRepository = paymentRepository;
        this.movementRepository = movementRepository;
        this.expenseRepository = expenseRepository;
        this.discrepancyRepository = discrepancyRepository;
        this.storeRepository = storeRepository;
        this.finance = finance;
        this.audit = audit;
        this.overdueDays = overdueDays;
    }

    // ------------------------------------------------------------------
    // Gharama za uendeshaji
    // ------------------------------------------------------------------

    @Transactional
    public ReportDtos.ExpenseResponse addExpense(Long storeId, ReportDtos.ExpenseRequest req, String actor) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Duka halijapatikana."));
        if (req == null || req.amount() == null || req.amount().signum() <= 0) {
            throw new IllegalArgumentException("Kiasi cha gharama kinapaswa kuwa zaidi ya sifuri.");
        }
        if (req.category() == null || req.category().isBlank()) {
            throw new IllegalArgumentException("Weka aina ya gharama (mfano usafiri, kodi).");
        }
        Expense e = new Expense();
        e.setStore(store);
        e.setCategory(req.category().trim().length() > 40 ? req.category().trim().substring(0, 40) : req.category().trim());
        e.setAmount(req.amount());
        String d = req.description() == null ? null : req.description().trim();
        e.setDescription(d == null || d.isEmpty() ? null : (d.length() > 255 ? d.substring(0, 255) : d));
        e.setExpenseDate(req.expenseDate() == null ? LocalDate.now() : req.expenseDate());
        e.setRecordedBy(actor);
        e.setCreatedAt(LocalDateTime.now());
        Expense saved = expenseRepository.save(e);
        audit.record(storeId, actor, "EXPENSE_ADDED", "EXPENSE", saved.getId(), null,
                saved.getCategory() + " " + saved.getAmount().toPlainString(), saved.getDescription());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.ExpenseResponse> listExpenses(Long storeId, LocalDate from, LocalDate to) {
        List<ReportDtos.ExpenseResponse> out = new ArrayList<>();
        for (Expense e : expenseRepository.findByStoreIdAndExpenseDateBetweenOrderByExpenseDateDesc(storeId, from, to)) {
            out.add(toResponse(e));
        }
        return out;
    }

    private ReportDtos.ExpenseResponse toResponse(Expense e) {
        return new ReportDtos.ExpenseResponse(e.getId(), e.getCategory(), e.getAmount(), e.getDescription(),
                e.getExpenseDate(), e.getRecordedBy(), e.getCreatedAt());
    }

    // ------------------------------------------------------------------
    // Faida
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ReportDtos.ProfitReport profit(Long storeId, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("Kipindi si sahihi.");
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay().minusNanos(1);

        // 1) Mapato na COGS: oda zilizofikishwa kwenye kipindi (si oda tu zilizowekwa).
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal cogs = BigDecimal.ZERO;
        int missingCost = 0;
        for (Orders o : ordersRepository.findByStoreIdAndDeliveredAtBetween(storeId, start, end)) {
            String status = OrderStatus.effective(o);
            if (!OrderStatus.SOLD.contains(status)) continue;
            revenue = revenue.add(nz(o.getTotalAmount()));
            for (OrderItems i : o.getOrderItems()) {
                if (i.getUnitCost() == null) {
                    missingCost++;
                } else {
                    cogs = cogs.add(i.getUnitCost().multiply(BigDecimal.valueOf(i.getQuantity())));
                }
            }
        }

        // 2) Returns: mkopo kwa mteja hupunguza mapato; bidhaa nzima iliyorudi kwenye stock hupunguza COGS.
        BigDecimal returnsCredit = BigDecimal.ZERO;
        BigDecimal restockedCost = BigDecimal.ZERO;
        for (OrderReturn r : returnRepository.findByOrderStoreIdAndCreatedAtBetween(storeId, start, end)) {
            returnsCredit = returnsCredit.add(r.getCreditAmount());
            if (r.isRestocked() && r.getUnitCost() != null) {
                restockedCost = restockedCost.add(r.getUnitCost().multiply(BigDecimal.valueOf(r.getQuantity())));
            }
        }
        BigDecimal netCogs = cogs.subtract(restockedCost);
        BigDecimal gross = revenue.subtract(returnsCredit).subtract(netCogs);

        // 3) Hasara za stock: bidhaa zilizoharibika, kupotea, tofauti za hesabu na marekebisho (zenye ishara).
        BigDecimal losses = BigDecimal.ZERO;
        int lossMissingCost = 0;
        List<String> lossTypes = List.of(MovementType.STOCK_DAMAGED, MovementType.STOCK_LOST,
                MovementType.COUNT_DIFFERENCE, MovementType.STOCK_ADJUSTMENT);
        for (InventoryMovement m : movementRepository
                .findByProductStoreIdAndMovementTypeInAndCreatedAtBetween(storeId, lossTypes, start, end)) {
            if (m.getUnitCost() == null) {
                lossMissingCost++;
                continue;
            }
            // quantityChange hasi = upungufu = hasara; chanya = ongezeko (inapunguza hasara).
            losses = losses.subtract(m.getUnitCost().multiply(BigDecimal.valueOf(m.getQuantityChange())));
        }

        // 4) Gharama za uendeshaji.
        BigDecimal expenses = BigDecimal.ZERO;
        for (Expense e : expenseRepository.findByStoreIdAndExpenseDateBetweenOrderByExpenseDateDesc(storeId, from, to)) {
            expenses = expenses.add(e.getAmount());
        }
        BigDecimal net = gross.subtract(losses).subtract(expenses);

        // 5) Pesa: malipo yaliyohakikiwa kwenye kipindi; madeni ya leo; malipo yasiyohakikiwa.
        BigDecimal cash = BigDecimal.ZERO;
        for (Payment p : paymentRepository.findByOrderStoreIdAndVerificationStatusAndVerifiedAtBetween(
                storeId, OrderFinanceService.VERIFIED, start, end)) {
            if (!"FAILED".equals(p.getStutus())) cash = cash.add(p.getAmount());
        }
        BigDecimal receivables = BigDecimal.ZERO;
        for (Orders o : ordersRepository.findByStoreIdAndStatusIn(storeId, List.of(OrderStatus.DELIVERED))) {
            BigDecimal b = finance.balance(o);
            if (b.signum() > 0) receivables = receivables.add(b);
        }
        BigDecimal unverified = BigDecimal.ZERO;
        for (Payment p : paymentRepository.findByOrderStoreIdAndVerificationStatus(storeId, "UNVERIFIED")) {
            if ("RECEIVED".equals(p.getStutus())) unverified = unverified.add(p.getAmount());
        }

        boolean complete = missingCost == 0 && lossMissingCost == 0;
        List<String> formulas = List.of(
                "Mapato (salesRevenue) = jumla ya oda zilizofikishwa (DELIVERED/COMPLETED) kwenye kipindi, kwa tarehe ya kufikishwa.",
                "COGS = jumla ya (idadi x gharama iliyorekodiwa wakati wa mauzo) - gharama ya bidhaa nzima zilizorudi kwenye stock.",
                "Faida ghafi (grossProfit) = Mapato - Returns - COGS.",
                "Hasara za stock = gharama ya bidhaa zilizoharibika/kupotea + tofauti za hesabu + marekebisho hasi (ongezeko hupunguza hasara).",
                "Faida halisi (netProfit) = Faida ghafi - Hasara za stock - Gharama za uendeshaji.",
                "Pesa iliyopokelewa (cashReceived) = malipo YALIYOHAKIKIWA kwenye kipindi; si sawa na mapato.",
                "Madeni (outstandingReceivables) = jumla ya salio la oda zilizofikishwa lakini hazijafungwa (leo).",
                complete
                        ? "Gharama za bidhaa zote zinajulikana: namba ni HALISI (ACTUAL)."
                        : "Baadhi ya bidhaa hazina gharama iliyorekodiwa: namba ni MAKADIRIO (ESTIMATE); faida inaweza kuonekana kubwa kuliko halisi.");

        return new ReportDtos.ProfitReport(from, to, complete ? "ACTUAL" : "ESTIMATE", revenue, returnsCredit, netCogs,
                gross, losses, expenses, net, cash, receivables, unverified, missingCost, lossMissingCost, formulas);
    }

    // ------------------------------------------------------------------
    // Mambo ya kuchunguza (si ushahidi wa makosa; ni vikumbusho)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ReportDtos.ExceptionsReport exceptions(Long storeId) {
        List<ReportDtos.ExceptionItem> items = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String key : List.of("ORDERS_NEED_REVIEW", "AWAITING_DELIVERY", "UNVERIFIED_PAYMENTS", "OVERDUE_BALANCES",
                "STOCK_DISCREPANCIES", "PAYMENT_DISCREPANCIES", "CANCELLED_AFTER_RESERVATION")) {
            counts.put(key, 0);
        }

        // Oda zinazosubiri ukaguzi (NEW, STOCK_REVIEW) na zinazosubiri kufikishwa.
        List<String> review = new ArrayList<>(OrderStatus.AWAITING_REVIEW);
        review.addAll(List.of("PENDING", "PAYMENT_PENDING")); // majina ya zamani
        for (Orders o : ordersRepository.findByStoreIdAndStatusIn(storeId, review)) {
            String status = OrderStatus.effective(o);
            if (OrderStatus.AWAITING_REVIEW.contains(status)) {
                add(items, counts, "ORDERS_NEED_REVIEW", o.getOrderNumber(),
                        OrderStatus.STOCK_REVIEW.equals(status)
                                ? "Kiasi kinazidi stock; thibitisha upatikanaji kabla ya kuidhinisha."
                                : "Oda mpya inasubiri kukaguliwa.", o.getTotalAmount(), o.getCreatedAt());
            }
        }
        for (Orders o : ordersRepository.findByStoreIdAndStatusIn(storeId, OrderStatus.AWAITING_DELIVERY)) {
            add(items, counts, "AWAITING_DELIVERY", o.getOrderNumber(), "Hali: " + OrderStatus.effective(o),
                    o.getTotalAmount(), o.getUpdatedAt());
        }

        // Malipo yaliyorekodiwa lakini hayajahakikiwa.
        for (Payment p : paymentRepository.findByOrderStoreIdAndVerificationStatus(storeId, "UNVERIFIED")) {
            if ("RECEIVED".equals(p.getStutus())) {
                add(items, counts, "UNVERIFIED_PAYMENTS", p.getOrder().getOrderNumber(),
                        "Malipo ya " + p.getMethod() + " yanasubiri uthibitisho.", p.getAmount(), p.getPaidAt());
            }
        }

        // Madeni yaliyochelewa: oda iliyofikishwa zaidi ya siku overdueDays na bado kuna salio.
        LocalDateTime limit = LocalDateTime.now().minusDays(overdueDays);
        for (Orders o : ordersRepository.findByStoreIdAndStatusIn(storeId, List.of(OrderStatus.DELIVERED))) {
            BigDecimal b = finance.balance(o);
            if (b.signum() > 0 && o.getDeliveredAt() != null && o.getDeliveredAt().isBefore(limit)) {
                add(items, counts, "OVERDUE_BALANCES", o.getOrderNumber(),
                        "Deni limepita siku " + overdueDays + " tangu kufikishwa.", b, o.getDeliveredAt());
            }
        }

        // Tofauti zilizo wazi (stock na malipo).
        for (Discrepancy d : discrepancyRepository.findByStoreIdAndStatus(storeId, "OPEN")) {
            String key = DiscrepancyService.STOCK.equals(d.getKind()) ? "STOCK_DISCREPANCIES" : "PAYMENT_DISCREPANCIES";
            add(items, counts, key, d.getReference(), d.getDescription(), null, d.getCreatedAt());
        }

        // Oda zilizoghairiwa baada ya stock kushikiliwa.
        for (Orders o : ordersRepository.findByStoreIdAndCancelledAfterReservationTrue(storeId)) {
            add(items, counts, "CANCELLED_AFTER_RESERVATION", o.getOrderNumber(),
                    "Imeghairiwa baada ya stock kushikiliwa: " + (o.getCancelReason() == null ? "" : o.getCancelReason()),
                    o.getTotalAmount(), o.getCancelledAt());
        }
        return new ReportDtos.ExceptionsReport(counts, items);
    }

    private void add(List<ReportDtos.ExceptionItem> items, Map<String, Integer> counts, String type, String ref,
            String description, BigDecimal amount, LocalDateTime date) {
        items.add(new ReportDtos.ExceptionItem(type, ref, description, amount, date));
        counts.merge(type, 1, Integer::sum);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
