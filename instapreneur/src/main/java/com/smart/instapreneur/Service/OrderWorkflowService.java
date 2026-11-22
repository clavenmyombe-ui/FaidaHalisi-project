package com.smart.instapreneur.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smart.instapreneur.Exception.IllegalArgumentException;
import com.smart.instapreneur.Exception.ResourceNotFoundException;
import com.smart.instapreneur.Model.OrderItems;
import com.smart.instapreneur.Model.OrderReturn;
import com.smart.instapreneur.Model.Orders;
import com.smart.instapreneur.Repository.OrderReturnRepository;
import com.smart.instapreneur.Repository.OrdersRepository;

/**
 * Sheria zote za kubadilisha hali ya oda ziko hapa. Hakuna sehemu nyingine ya app inayoruhusiwa
 * kuandika order.status moja kwa moja.
 *
 * Kila njia ni @Transactional na inaanza kwa kufunga mstari wa oda (lockById): maombi mawili
 * ya wakati mmoja kwa oda ile ile yanafuatana, hayapiti ukaguzi kwa pamoja. Hii ndiyo inazuia
 * "kuidhinisha mara mbili" na "kurudisha stock mara mbili".
 */
@Service
public class OrderWorkflowService {
    private final OrdersRepository ordersRepository;
    private final OrderReturnRepository returnRepository;
    private final InventoryService inventory;
    private final OrderFinanceService finance;
    private final DiscrepancyService discrepancies;
    private final AuditService audit;

    public OrderWorkflowService(OrdersRepository ordersRepository, OrderReturnRepository returnRepository,
            InventoryService inventory, OrderFinanceService finance, DiscrepancyService discrepancies,
            AuditService audit) {
        this.ordersRepository = ordersRepository;
        this.returnRepository = returnRepository;
        this.inventory = inventory;
        this.finance = finance;
        this.discrepancies = discrepancies;
        this.audit = audit;
    }

    private Orders load(Long orderId) {
        return ordersRepository.lockById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Oda haikupatikana."));
    }

    private void touch(Orders order, String newStatus) {
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
    }

    private Long storeId(Orders order) {
        return order.getStore() == null ? null : order.getStore().getId();
    }

    /** Mmiliki anakagua upatikanaji na kuidhinisha: stock inashikiliwa (ikitosha) na oda inakuwa APPROVED. */
    @Transactional
    public Orders approve(Long orderId, String actor) {
        Orders order = load(orderId);
        String current = OrderStatus.effective(order);
        if (!OrderStatus.AWAITING_REVIEW.contains(current)) {
            throw new IllegalArgumentException("Oda hii haiwezi kuidhinishwa (hali ya sasa: " + current + ").");
        }
        // Oda za zamani ziliweka stock wakati wa kuundwa; hazishikilii mara ya pili.
        if (!order.isStockReserved()) {
            inventory.reserveForOrder(order, actor); // inakataa kwa ujumbe wazi ikiwa stock haitoshi
        }
        touch(order, OrderStatus.APPROVED);
        Orders saved = ordersRepository.save(order);
        audit.record(storeId(order), actor, "ORDER_APPROVED", "ORDER", orderId, current, OrderStatus.APPROVED,
                "Stock imeshikiliwa");
        return saved;
    }

    /** Mmiliki anakataa oda ambayo bado haijaidhinishwa (mfano stock haitoshi). Hakuna stock ya kurudisha. */
    @Transactional
    public Orders reject(Long orderId, String reason, String actor) {
        Orders order = load(orderId);
        String current = OrderStatus.effective(order);
        if (!OrderStatus.AWAITING_REVIEW.contains(current)) {
            throw new IllegalArgumentException("Oda hii imeshaidhinishwa; tumia 'Ghairi oda' badala ya kukataa.");
        }
        return doCancel(order, current, requireReason(reason), actor);
    }

    /** Hatua za mbele: PROCESSING, READY_FOR_DELIVERY, DELIVERED, COMPLETED. */
    @Transactional
    public Orders advance(Long orderId, String target, String actor) {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("Hali mpya inahitajika.");
        }
        String wanted = target.trim().toUpperCase();
        Orders order = load(orderId);
        String current = OrderStatus.effective(order);

        switch (wanted) {
            case OrderStatus.PROCESSING:
                require(current, List.of(OrderStatus.APPROVED), wanted);
                break;
            case OrderStatus.READY_FOR_DELIVERY:
                require(current, List.of(OrderStatus.APPROVED, OrderStatus.PROCESSING), wanted);
                break;
            case OrderStatus.DELIVERED:
                require(current, List.of(OrderStatus.READY_FOR_DELIVERY), wanted);
                // Bidhaa zinatoka ghalani hapa (mara moja tu: issueForOrder inakataa mara ya pili).
                inventory.issueForOrder(order, actor);
                order.setDeliveredAt(LocalDateTime.now());
                break;
            case OrderStatus.COMPLETED:
                require(current, List.of(OrderStatus.DELIVERED), wanted);
                BigDecimal balance = finance.balance(order);
                if (balance.signum() > 0) {
                    throw new IllegalArgumentException("Oda haiwezi kufungwa: bado kuna deni la TSh " + balance.toPlainString()
                            + ". Rekodi na uhakiki malipo kwanza.");
                }
                break;
            case OrderStatus.APPROVED:
                throw new IllegalArgumentException("Tumia kitufe cha 'Idhinisha' ili kuidhinisha oda.");
            case OrderStatus.CANCELLED:
                throw new IllegalArgumentException("Tumia 'Ghairi oda' na andika sababu.");
            default:
                throw new IllegalArgumentException("Hali isiyojulikana: " + wanted);
        }
        touch(order, wanted);
        Orders saved = ordersRepository.save(order);
        audit.record(storeId(order), actor, "ORDER_STATUS", "ORDER", orderId, current, wanted, null);
        return saved;
    }

    private void require(String current, List<String> allowedFrom, String target) {
        if (!allowedFrom.contains(current)) {
            throw new IllegalArgumentException("Huwezi kuhamisha oda kutoka " + current + " kwenda " + target + ".");
        }
    }

    /**
     * Kughairi oda.
     *  - Mteja: anaweza tu kabla ya kuidhinishwa.
     *  - Muuzaji: kabla ya DELIVERED, na lazima aandike sababu.
     * Oda iliyofikishwa haighairiwi; tumia returns.
     */
    @Transactional
    public Orders cancel(Long orderId, String reason, String actor, boolean requestedByCustomer) {
        Orders order = load(orderId);
        String current = OrderStatus.effective(order);
        if (OrderStatus.SOLD.contains(current)) {
            throw new IllegalArgumentException("Oda iliyofikishwa haiwezi kughairiwa. Rekodi bidhaa zilizorudishwa badala yake.");
        }
        if (OrderStatus.CANCELLED.equals(current)) {
            throw new IllegalArgumentException("Oda hii tayari imeghairiwa.");
        }
        if (requestedByCustomer && !OrderStatus.AWAITING_REVIEW.contains(current)) {
            throw new IllegalArgumentException("Oda imeshaidhinishwa. Wasiliana na muuzaji kupitia WhatsApp.");
        }
        String clean = requestedByCustomer && (reason == null || reason.isBlank())
                ? "Imeghairiwa na mteja" : requireReason(reason);
        return doCancel(order, current, clean, actor);
    }

    private Orders doCancel(Orders order, String previousStatus, String reason, String actor) {
        boolean wasReserved = order.isStockReserved();
        if (wasReserved) {
            inventory.releaseForOrder(order, actor); // inarudisha stock; bendera inazuia kurudisha mara mbili
        }
        order.setCancelledAfterReservation(wasReserved);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(reason);
        touch(order, OrderStatus.CANCELLED);
        Orders saved = ordersRepository.save(order);

        // Pesa iliyohakikiwa kwa oda iliyoghairiwa inahitaji kurejeshwa/kuchunguzwa; usiiache kimya.
        BigDecimal paid = finance.verifiedPaid(order.getId());
        if (paid.signum() > 0 && order.getStore() != null) {
            discrepancies.open(order.getStore().getId(), DiscrepancyService.PAYMENT, order.getOrderNumber(),
                    "Oda " + order.getOrderNumber() + " imeghairiwa ikiwa na malipo yaliyohakikiwa ya TSh "
                            + paid.toPlainString() + ". Kagua urejeshaji wa pesa.", actor);
        }
        audit.record(storeId(order), actor, "ORDER_CANCELLED", "ORDER", order.getId(), previousStatus,
                OrderStatus.CANCELLED, reason);
        return saved;
    }

    private String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Andika sababu.");
        }
        String clean = reason.trim();
        if (clean.length() > 255) {
            throw new IllegalArgumentException("Sababu isiwe zaidi ya herufi 255.");
        }
        return clean;
    }

    /**
     * Mteja amerudisha bidhaa baada ya oda kufikishwa. Hii inapunguza mapato (mkopo kwa mteja) na,
     * ikiwa bidhaa ni nzima, inairudisha kwenye stock. Haibadilishi hali ya oda.
     */
    @Transactional
    public OrderReturn recordReturn(Long orderId, Long productId, int quantity, String reason, boolean restock, String actor) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Kiasi kinapaswa kuwa zaidi ya sifuri.");
        }
        Orders order = load(orderId);
        String current = OrderStatus.effective(order);
        if (!OrderStatus.SOLD.contains(current)) {
            throw new IllegalArgumentException("Bidhaa zinaweza kurudishwa tu baada ya oda kufikishwa.");
        }
        String clean = requireReason(reason);

        OrderItems line = null;
        for (OrderItems item : order.getOrderItems()) {
            if (item.getProduct().getId().equals(productId)) {
                line = item;
                break;
            }
        }
        if (line == null) {
            throw new IllegalArgumentException("Bidhaa hii haimo kwenye oda.");
        }
        int alreadyReturned = 0;
        for (OrderReturn r : returnRepository.findByOrderId(orderId)) {
            if (r.getProduct().getId().equals(productId)) alreadyReturned += r.getQuantity();
        }
        if (alreadyReturned + quantity > line.getQuantity()) {
            throw new IllegalArgumentException("Kiasi cha kurudisha kinazidi kilichonunuliwa (" + line.getQuantity()
                    + ", tayari kimerudishwa " + alreadyReturned + ").");
        }

        OrderReturn r = new OrderReturn();
        r.setOrder(order);
        r.setProduct(line.getProduct());
        r.setQuantity(quantity);
        r.setCreditAmount(line.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
        r.setUnitCost(line.getUnitCost());
        r.setRestocked(restock);
        r.setReason(clean);
        r.setRecordedBy(actor);
        r.setCreatedAt(LocalDateTime.now());
        OrderReturn saved = returnRepository.save(r);

        if (restock) {
            inventory.restockFromReturn(line.getProduct(), quantity, line.getUnitCost(), order.getOrderNumber(), clean, actor);
        } else {
            inventory.recordDamagedReturn(line.getProduct(), quantity, line.getUnitCost(), order.getOrderNumber(), clean, actor);
        }
        audit.record(storeId(order), actor, "ORDER_RETURN", "ORDER", orderId, null,
                quantity + " x " + line.getProduct().getName() + (restock ? " (stock)" : " (imeharibika)"), clean);
        return saved;
    }
}
