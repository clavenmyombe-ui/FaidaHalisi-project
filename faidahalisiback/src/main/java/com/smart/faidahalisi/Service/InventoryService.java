package com.smart.faidahalisi.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smart.faidahalisi.Dto.InventoryDtos;
import com.smart.faidahalisi.Exception.IllegalArgumentException;
import com.smart.faidahalisi.Exception.ResourceNotFoundException;
import com.smart.faidahalisi.Model.Discrepancy;
import com.smart.faidahalisi.Model.IncomingStock;
import com.smart.faidahalisi.Model.InventoryMovement;
import com.smart.faidahalisi.Model.MovementType;
import com.smart.faidahalisi.Model.OrderItems;
import com.smart.faidahalisi.Model.Orders;
import com.smart.faidahalisi.Model.Products;
import com.smart.faidahalisi.Repository.IncomingStockRepository;
import com.smart.faidahalisi.Repository.OrderItemsRepository;
import com.smart.faidahalisi.Repository.ProductRepository;

/**
 * Kazi zote za kubadilisha stock ziko hapa. Kila njia:
 *  1) inafunga (lock) mstari wa bidhaa kwenye database,
 *  2) inakagua sheria,
 *  3) inabadilisha stock,
 *  4) inaandika historia (InventoryMovement) na audit,
 * vyote ndani ya transaction MOJA: kikishindwa kimoja, hakuna kilichobadilika.
 *
 * Maana ya namba:
 *   available (stockQuantity) = stock isiyoshikiliwa, inaweza kuahidiwa kwa oda mpya
 *   reserved                  = vipande vilivyoshikiliwa na oda zilizoidhinishwa (hazijatoka ghalani)
 *   onHand                    = available + reserved = stock halisi ghalani
 *   incoming                  = mzigo unaotarajiwa (HAUJAINGIA kwenye onHand mpaka upokelewe)
 */
@Service
public class InventoryService {
    private static final int MAX_QTY = 1_000_000;

    private final ProductRepository productRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final IncomingStockRepository incomingRepository;
    private final InventoryMovementService movements;
    private final AuditService audit;
    private final DiscrepancyService discrepancies;

    public InventoryService(ProductRepository productRepository, OrderItemsRepository orderItemsRepository,
            IncomingStockRepository incomingRepository, InventoryMovementService movements,
            AuditService audit, DiscrepancyService discrepancies) {
        this.productRepository = productRepository;
        this.orderItemsRepository = orderItemsRepository;
        this.incomingRepository = incomingRepository;
        this.movements = movements;
        this.audit = audit;
        this.discrepancies = discrepancies;
    }

    // ------------------------------------------------------------------
    // Msaada wa ndani
    // ------------------------------------------------------------------

    /** Inafunga bidhaa moja; maombi mengine ya stock ya bidhaa hiyo yanasubiri hadi transaction hii iishe. */
    private Products lockOne(Long productId) {
        List<Products> found = productRepository.lockByIds(List.of(productId));
        if (found.isEmpty()) {
            throw new ResourceNotFoundException("Bidhaa haijapatikana.");
        }
        return found.get(0);
    }

    private void requirePositive(int quantity, String what) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(what + " kinapaswa kuwa zaidi ya sifuri.");
        }
        if (quantity > MAX_QTY) {
            throw new IllegalArgumentException(what + " ni kikubwa mno.");
        }
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

    @Transactional(readOnly = true)
    public long reservedUnits(Long productId) {
        return orderItemsRepository.reservedUnits(productId);
    }
    // ------------------------------------------------------------------
    // Kupokea stock
    // ------------------------------------------------------------------
    /**
     * Mzigo umefika na umehesabiwa. Stock inayopatikana inaongezeka na gharama ya wastani
     * (weighted average) inasasishwa ikiwa gharama ya kipande imetolewa.
     */
    @Transactional
    public InventoryMovement receive(Long productId, int quantity, BigDecimal unitCost, String reason,
            String actor, String reference) {
        requirePositive(quantity, "Kiasi cha stock");
        String cleanReason = requireReason(reason);
        if (unitCost != null && unitCost.signum() < 0) {
            throw new IllegalArgumentException("Gharama haiwezi kuwa hasi.");
        }
        Products product = lockOne(productId);
        int before = product.getStockQuantity();
        long onHandBefore = before + orderItemsRepository.reservedUnits(productId);
        int after = before + quantity; // MAX_QTY na stock ya kawaida haiwezi kuzidi int

        if (unitCost != null) {
            BigDecimal oldCost = product.getCostPrice();
            BigDecimal newCost = weightedAverage(oldCost, onHandBefore, unitCost, quantity);
            product.setCostPrice(newCost);
            audit.record(product.getStore().getId(), actor, "COST_CHANGE", "PRODUCT", productId,
                    String.valueOf(oldCost), String.valueOf(newCost), "Mapokezi ya stock");
        }
        product.setStockQuantity(after);
        productRepository.save(product);

        InventoryMovement saved = movements.record(product, MovementType.STOCK_RECEIVED, quantity, before, after,
                quantity, unitCost != null ? unitCost : product.getCostPrice(), reference, cleanReason, actor);
        audit.record(product.getStore().getId(), actor, "STOCK_RECEIVED", "PRODUCT", productId,
                String.valueOf(before), String.valueOf(after), cleanReason);
        return saved;
    }

    /** Gharama mpya ya wastani = (gharama ya zamani x stock ya zamani + gharama mpya x kiasi kipya) / jumla. */
    private BigDecimal weightedAverage(BigDecimal oldCost, long onHandBefore, BigDecimal newCost, int quantity) {
        if (oldCost == null || onHandBefore <= 0) {
            return newCost.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal oldValue = oldCost.multiply(BigDecimal.valueOf(onHandBefore));
        BigDecimal newValue = newCost.multiply(BigDecimal.valueOf(quantity));
        return oldValue.add(newValue)
                .divide(BigDecimal.valueOf(onHandBefore + quantity), 2, RoundingMode.HALF_UP);
    }

    /** Stock ya mwanzo wakati bidhaa inaundwa na idadi ya kuanzia. */
    @Transactional
    public void recordOpeningStock(Products product, String actor) {
        int qty = product.getStockQuantity();
        if (qty > 0) {
            movements.record(product, MovementType.STOCK_RECEIVED, qty, 0, qty, qty, product.getCostPrice(),
                    null, "Stock ya mwanzo wakati wa kuongeza bidhaa", actor);
            audit.record(product.getStore().getId(), actor, "STOCK_OPENING", "PRODUCT", product.getId(),
                    "0", String.valueOf(qty), "Stock ya mwanzo");
        }
    }

    // ------------------------------------------------------------------
    // Mzigo unaotarajiwa (incoming)
    // ------------------------------------------------------------------

    @Transactional
    public IncomingStock createIncoming(Long productId, int quantity, BigDecimal unitCost, String supplier,
            String note, String actor) {
        requirePositive(quantity, "Kiasi kinachotarajiwa");
        if (unitCost != null && unitCost.signum() < 0) {
            throw new IllegalArgumentException("Gharama haiwezi kuwa hasi.");
        }
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Bidhaa haijapatikana."));
        IncomingStock in = new IncomingStock();
        in.setProduct(product);
        in.setExpectedQuantity(quantity);
        in.setUnitCost(unitCost);
        in.setSupplier(supplier == null || supplier.isBlank() ? null : supplier.trim());
        in.setNote(note == null || note.isBlank() ? null : note.trim());
        in.setStatus("EXPECTED");
        in.setCreatedBy(actor);
        in.setCreatedAt(LocalDateTime.now());
        IncomingStock saved = incomingRepository.save(in);
        // Hakuna badiliko la stock hapa: mzigo bado haujafika.
        audit.record(product.getStore().getId(), actor, "INCOMING_CREATED", "INCOMING", saved.getId(),
                null, quantity + " x " + product.getName(), saved.getSupplier());
        return saved;
    }

    /**
     * Mzigo umefika. receivedQuantity inaweza kuwa tofauti na kilichotarajiwa; tofauti inafunguliwa
     * kama jambo la kuchunguza (huenda msambazaji alituma pungufu, au kulikuwa na kosa la kuhesabu).
     */
    @Transactional
    public IncomingStock receiveIncoming(Long incomingId, int receivedQuantity, BigDecimal unitCostOverride, String actor) {
        IncomingStock in = incomingRepository.findById(incomingId)
                .orElseThrow(() -> new ResourceNotFoundException("Mzigo haujapatikana."));
        if (!"EXPECTED".equals(in.getStatus())) {
            throw new IllegalArgumentException("Mzigo huu tayari umeshashughulikiwa.");
        }
        BigDecimal cost = unitCostOverride != null ? unitCostOverride : in.getUnitCost();
        Long productId = in.getProduct().getId();
        Long storeId = in.getProduct().getStore().getId();
        receive(productId, receivedQuantity, cost, "Mapokezi ya mzigo #" + incomingId, actor, "IN-" + incomingId);

        in.setStatus("RECEIVED");
        in.setReceivedQuantity(receivedQuantity);
        if (cost != null) in.setUnitCost(cost);
        in.setReceivedBy(actor);
        in.setReceivedAt(LocalDateTime.now());
        IncomingStock saved = incomingRepository.save(in);

        if (receivedQuantity != in.getExpectedQuantity()) {
            discrepancies.open(storeId, DiscrepancyService.STOCK, "IN-" + incomingId,
                    "Mzigo #" + incomingId + " (" + in.getProduct().getName() + "): ulitarajiwa "
                            + in.getExpectedQuantity() + ", umepokelewa " + receivedQuantity
                            + ". Tafadhali chunguza.", actor);
        }
        return saved;
    }

    @Transactional
    public IncomingStock cancelIncoming(Long incomingId, String reason, String actor) {
        IncomingStock in = incomingRepository.findById(incomingId)
                .orElseThrow(() -> new ResourceNotFoundException("Mzigo haujapatikana."));
        if (!"EXPECTED".equals(in.getStatus())) {
            throw new IllegalArgumentException("Mzigo huu tayari umeshashughulikiwa.");
        }
        String clean = requireReason(reason);
        in.setStatus("CANCELLED");
        IncomingStock saved = incomingRepository.save(in);
        audit.record(in.getProduct().getStore().getId(), actor, "INCOMING_CANCELLED", "INCOMING", incomingId,
                "EXPECTED", "CANCELLED", clean);
        return saved;
    }
    // ------------------------------------------------------------------
    // Marekebisho, hasara, hesabu ya ghalani
    // ------------------------------------------------------------------
    /** Marekebisho ya mmiliki (yenye sababu lazima). Hayabadilishi stock iliyoshikiliwa na oda. */
    @Transactional
    public InventoryMovement adjust(Long productId, int delta, String reason, String actor) {
        if (delta == 0) {
            throw new IllegalArgumentException("Badiliko haliwezi kuwa sifuri.");
        }
        if (Math.abs((long) delta) > MAX_QTY) {
            throw new IllegalArgumentException("Badiliko ni kubwa mno.");
        }
        String clean = requireReason(reason);
        Products product = lockOne(productId);
        int before = product.getStockQuantity();
        int after = before + delta;
        if (after < 0) {
            throw new IllegalArgumentException("Marekebisho yangefanya stock inayopatikana kuwa chini ya sifuri.");
        }
        product.setStockQuantity(after);
        productRepository.save(product);
        InventoryMovement saved = movements.record(product, MovementType.STOCK_ADJUSTMENT, delta, before, after,
                Math.abs(delta), product.getCostPrice(), null, clean, actor);
        audit.record(product.getStore().getId(), actor, "STOCK_ADJUSTMENT", "PRODUCT", productId,
                String.valueOf(before), String.valueOf(after), clean);
        return saved;
    }
    /** Weka stock inayopatikana kuwa thamani fulani (inatumika na endpoint ya zamani ya /stock). */
    @Transactional
    public InventoryMovement adjustTo(Long productId, int newAvailable, String reason, String actor) {
        if (newAvailable < 0) {
            throw new IllegalArgumentException("Stock haiwezi kuwa hasi.");
        }
        Products product = lockOne(productId);
        return adjust(productId, newAvailable - product.getStockQuantity(), reason, actor);
    }

    /** Bidhaa imeharibika au imepotea ghalani: stock inapungua na hasara inarekodiwa kwa gharama ya wakati huo. */
    @Transactional
    public InventoryMovement writeOff(Long productId, String type, int quantity, String reason, String actor) {
        requirePositive(quantity, "Kiasi");
        String clean = requireReason(reason);
        String movementType;
        if ("DAMAGED".equalsIgnoreCase(type)) {
            movementType = MovementType.STOCK_DAMAGED;
        } else if ("LOST".equalsIgnoreCase(type)) {
            movementType = MovementType.STOCK_LOST;
        } else {
            throw new IllegalArgumentException("Aina lazima iwe DAMAGED au LOST.");
        }
        Products product = lockOne(productId);
        int before = product.getStockQuantity();
        if (quantity > before) {
            throw new IllegalArgumentException("Kiasi kinazidi stock inayopatikana (" + before + ").");
        }
        int after = before - quantity;
        product.setStockQuantity(after);
        productRepository.save(product);
        InventoryMovement saved = movements.record(product, movementType, -quantity, before, after,
                quantity, product.getCostPrice(), null, clean, actor);
        audit.record(product.getStore().getId(), actor, movementType, "PRODUCT", productId,
                String.valueOf(before), String.valueOf(after), clean);
        return saved;
    }

    /**
     * Hesabu ya ghalani. Tunalinganisha kilichohesabiwa na kinachotarajiwa (available + reserved).
     * Tofauti ikiwepo: stock inarekebishwa, historia inaandikwa, na kesi ya uchunguzi inafunguliwa.
     * Hii ni ukumbusho wa kuchunguza, si tuhuma dhidi ya mtu yeyote.
     */
    @Transactional
    public InventoryDtos.CountResult recordCount(Long productId, int countedQuantity, String note, String actor) {
        if (countedQuantity < 0 || countedQuantity > MAX_QTY) {
            throw new IllegalArgumentException("Idadi iliyohesabiwa si sahihi.");
        }
        Products product = lockOne(productId);
        int available = product.getStockQuantity();
        long reserved = orderItemsRepository.reservedUnits(productId);
        int expected = (int) (available + reserved);
        int difference = countedQuantity - expected;
        Long storeId = product.getStore().getId();

        if (difference == 0) {
            audit.record(storeId, actor, "STOCK_COUNT", "PRODUCT", productId, String.valueOf(expected),
                    String.valueOf(countedQuantity), "Hesabu inalingana");
            return new InventoryDtos.CountResult(expected, countedQuantity, 0, null, "Hesabu inalingana na mfumo.");
        }
        int newAvailable = available + difference;
        if (newAvailable < 0) {
            throw new IllegalArgumentException("Kilichohesabiwa (" + countedQuantity
                    + ") ni kidogo kuliko stock iliyoshikiliwa na oda (" + reserved
                    + "). Kagua oda zilizoidhinishwa kwanza.");
        }
        product.setStockQuantity(newAvailable);
        productRepository.save(product);
        String reason = "Hesabu ya ghalani: ilitarajiwa " + expected + ", imehesabiwa " + countedQuantity
                + (note == null || note.isBlank() ? "" : ". " + note.trim());
        String clean = reason.length() > 255 ? reason.substring(0, 255) : reason;
        movements.record(product, MovementType.COUNT_DIFFERENCE, difference, available, newAvailable,
                Math.abs(difference), product.getCostPrice(), null, clean, actor);
        Discrepancy d = discrepancies.open(storeId, DiscrepancyService.STOCK, "P-" + productId,
                product.getName() + ": " + clean + ". Tafadhali chunguza sababu.", actor);
        audit.record(storeId, actor, "STOCK_COUNT", "PRODUCT", productId, String.valueOf(expected),
                String.valueOf(countedQuantity), clean);
        return new InventoryDtos.CountResult(expected, countedQuantity, difference, d.getId(),
                "Tofauti imepatikana na imerekodiwa kwa uchunguzi.");
    }
    // ------------------------------------------------------------------
    // Stock ya oda: kushikilia, kuachilia, kutoa
    // ------------------------------------------------------------------
    private Map<Long, Integer> wantedByProduct(Orders order) {
        Map<Long, Integer> wanted = new TreeMap<>(); // TreeMap = mpangilio wa id (inazuia deadlock)
        if (order.getOrderItems() != null) {
            for (OrderItems item : order.getOrderItems()) {
                wanted.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
            }
        }
        return wanted;
    }
    private Map<Long, Products> lockProductsOf(Map<Long, Integer> wanted) {
        Map<Long, Products> byId = new LinkedHashMap<>();
        for (Products p : productRepository.lockByIds(wanted.keySet())) {
            byId.put(p.getId(), p);
        }
        return byId;
    }
    /**
     * Shikilia stock ya oda. Tunakagua bidhaa ZOTE kwanza; ikiwa hata moja haitoshi, hakuna kinachobadilika
     * (hatuna usafirishaji wa sehemu). Kwa kuwa bidhaa zimefungwa, oda nyingine haiwezi kuchukua stock ile ile.
     */
    @Transactional
    public void reserveForOrder(Orders order, String actor) {
        if (order.isStockReserved()) {
            throw new IllegalArgumentException("Stock ya oda hii tayari imeshikiliwa.");
        }
        Map<Long, Integer> wanted = wantedByProduct(order);
        if (wanted.isEmpty()) {
            throw new IllegalArgumentException("Oda haina bidhaa.");
        }
        Map<Long, Products> locked = lockProductsOf(wanted);

        List<String> shortages = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : wanted.entrySet()) {
            Products p = locked.get(e.getKey());
            if (p == null) {
                shortages.add("bidhaa #" + e.getKey() + " haipo");
            } else if (p.getStockQuantity() < e.getValue()) {
                shortages.add(p.getName() + " (inapatikana " + p.getStockQuantity() + ", inahitajika " + e.getValue() + ")");
            }
        }
        if (!shortages.isEmpty()) {
            throw new IllegalArgumentException("Stock haitoshi: " + String.join("; ", shortages));
        }

        for (Map.Entry<Long, Integer> e : wanted.entrySet()) {
            Products p = locked.get(e.getKey());
            int before = p.getStockQuantity();
            int after = before - e.getValue();
            p.setStockQuantity(after);
            productRepository.save(p);
            movements.record(p, MovementType.ORDER_RESERVED, -e.getValue(), before, after, e.getValue(),
                    p.getCostPrice(), order.getOrderNumber(), "Stock imeshikiliwa kwa oda " + order.getOrderNumber(), actor);
        }
        // Hifadhi gharama ya wakati huu kwenye kila mstari: faida ya baadaye haitabadilika ukibadilisha gharama.
        for (OrderItems item : order.getOrderItems()) {
            item.setUnitCost(locked.get(item.getProduct().getId()).getCostPrice());
        }
        order.setStockReserved(true);
    }
    /** Oda imeghairiwa kabla ya kutoka ghalani: rudisha stock iliyoshikiliwa (mara moja tu, kwa bendera ya stockReserved). */
    @Transactional
    public void releaseForOrder(Orders order, String actor) {
        if (!order.isStockReserved()) {
            return; // hakuna cha kurudisha; hii ndiyo inazuia kurudisha mara mbili
        }
        Map<Long, Integer> wanted = wantedByProduct(order);
        Map<Long, Products> locked = lockProductsOf(wanted);
        for (Map.Entry<Long, Integer> e : wanted.entrySet()) {
            Products p = locked.get(e.getKey());
            if (p == null) continue;
            int before = p.getStockQuantity();
            int after = before + e.getValue();
            p.setStockQuantity(after);
            productRepository.save(p);
            movements.record(p, MovementType.ORDER_RELEASED, e.getValue(), before, after, e.getValue(),
                    p.getCostPrice(), order.getOrderNumber(), "Stock imeachiliwa: oda " + order.getOrderNumber() + " imeghairiwa", actor);
        }
        order.setStockReserved(false);
    }
    /** Oda imefikishwa: bidhaa zimetoka ghalani. Stock inayopatikana haibadiliki (ilishapunguzwa wakati wa kushikilia). */
    @Transactional
    public void issueForOrder(Orders order, String actor) {
        if (order.isStockIssued()) {
            throw new IllegalArgumentException("Bidhaa za oda hii tayari zimeshatolewa.");
        }
        if (!order.isStockReserved()) {
            throw new IllegalArgumentException("Stock ya oda hii haijashikiliwa; idhinisha oda kwanza.");
        }
        Map<Long, Integer> wanted = wantedByProduct(order);
        Map<Long, Products> locked = lockProductsOf(wanted);
        for (Map.Entry<Long, Integer> e : wanted.entrySet()) {
            Products p = locked.get(e.getKey());
            if (p == null) continue;
            int available = p.getStockQuantity();
            movements.record(p, MovementType.ORDER_ISSUED, 0, available, available, e.getValue(),
                    p.getCostPrice(), order.getOrderNumber(), "Bidhaa zimetoka ghalani: oda " + order.getOrderNumber(), actor);
        }
        order.setStockReserved(false);
        order.setStockIssued(true);
    }

    /** Mteja amerudisha bidhaa nzima: inaingia tena kwenye stock inayopatikana. */
    @Transactional
    public void restockFromReturn(Products productRef, int quantity, BigDecimal unitCost, String orderNumber, String reason, String actor) {
        Products p = lockOne(productRef.getId());
        int before = p.getStockQuantity();
        int after = before + quantity;
        p.setStockQuantity(after);
        productRepository.save(p);
        movements.record(p, MovementType.CUSTOMER_RETURN, quantity, before, after, quantity, unitCost,
                orderNumber, reason, actor);
    }

    /** Mteja amerudisha bidhaa iliyoharibika: haiingii kwenye stock; inarekodiwa tu kama harakati (hasara iko kwenye COGS). */
    @Transactional
    public void recordDamagedReturn(Products productRef, int quantity, BigDecimal unitCost, String orderNumber, String reason, String actor) {
        Products p = lockOne(productRef.getId());
        int available = p.getStockQuantity();
        movements.record(p, MovementType.RETURN_DAMAGED, 0, available, available, quantity, unitCost,
                orderNumber, reason, actor);
    }
    // ------------------------------------------------------------------
    // Muhtasari kwa mmiliki
    // ------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<InventoryDtos.StockRow> overview(Long storeId) {
        List<InventoryDtos.StockRow> rows = new ArrayList<>();
        for (Products p : productRepository.findByStoreId(storeId)) {
            long reserved = orderItemsRepository.reservedUnits(p.getId());
            long incoming = 0;
            for (IncomingStock in : incomingRepository.findByProductIdAndStatus(p.getId(), "EXPECTED")) {
                incoming += in.getExpectedQuantity();
            }
            int available = p.getStockQuantity();
            long onHand = available + reserved;
            BigDecimal value = p.getCostPrice() == null ? null : p.getCostPrice().multiply(BigDecimal.valueOf(onHand));
            rows.add(new InventoryDtos.StockRow(p.getId(), p.getName(), p.getStatus(), (int) onHand, available,
                    reserved, incoming, p.getCostPrice(), value));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public List<InventoryDtos.IncomingResponse> incomingForStore(Long storeId) {
        List<InventoryDtos.IncomingResponse> out = new ArrayList<>();
        for (IncomingStock in : incomingRepository.findByProductStoreIdOrderByCreatedAtDesc(storeId)) {
            out.add(toResponse(in));
        }
        return out;
    }
    public InventoryDtos.IncomingResponse toResponse(IncomingStock in) {
        return new InventoryDtos.IncomingResponse(in.getId(), in.getProduct().getId(), in.getProduct().getName(),
                in.getExpectedQuantity(), in.getReceivedQuantity(), in.getUnitCost(), in.getSupplier(),
                in.getStatus(), in.getNote(), in.getCreatedBy(), in.getCreatedAt(), in.getReceivedBy(),
                in.getReceivedAt());
    }
}
