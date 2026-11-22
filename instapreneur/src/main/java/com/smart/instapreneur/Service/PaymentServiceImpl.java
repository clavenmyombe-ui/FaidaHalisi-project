package com.smart.instapreneur.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smart.instapreneur.Dto.PaymentRequestDto;
import com.smart.instapreneur.Exception.IllegalArgumentException;
import com.smart.instapreneur.Exception.ResourceNotFoundException;
import com.smart.instapreneur.Model.Orders;
import com.smart.instapreneur.Model.Payment;
import com.smart.instapreneur.Repository.OrdersRepository;
import com.smart.instapreneur.Repository.PaymentRepository;

/**
 * Mzunguko wa malipo:
 *   1. (hiari) Mteja anachagua njia -> Payment PENDING / UNVERIFIED. Hakuna pesa iliyohesabiwa.
 *   2. Muuzaji anarekodi pesa aliyopokea (+ namba ya muamala) -> RECEIVED / UNVERIFIED.
 *   3. Muuzaji anahakiki -> VERIFIED. Hapa tu ndipo salio la oda linapungua.
 * Malipo ya sehemu yanaruhusiwa: oda moja inaweza kuwa na malipo mengi. Kiasi kamwe hakizidi deni lililobaki.
 * Malipo HAYABADILISHI hali ya oda (utoaji na malipo ni mambo mawili tofauti).
 */
@Service
public class PaymentServiceImpl implements PaymentService {
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RECEIVED = "RECEIVED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String UNVERIFIED = "UNVERIFIED";
    private static final String REJECTED = "REJECTED";
    private static final Set<String> METHODS = Set.of("MPESA", "MIXX", "AIRTEL", "HALOPESA", "COD", "CASH", "BANK");

    private final PaymentRepository paymentRepository;
    private final OrdersRepository ordersRepository;
    private final OrderFinanceService finance;
    private final DiscrepancyService discrepancies;
    private final AuditService audit;

    public PaymentServiceImpl(PaymentRepository paymentRepository, OrdersRepository ordersRepository,
            OrderFinanceService finance, DiscrepancyService discrepancies, AuditService audit) {
        this.paymentRepository = paymentRepository;
        this.ordersRepository = ordersRepository;
        this.finance = finance;
        this.discrepancies = discrepancies;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentById(Long paymentId) {
        validateId(paymentId, "Payment id is required");
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Malipo hayakupatikana."));
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentByTransaction(String transactionalRefference) {
        validateText(transactionalRefference, "Transaction reference is required");
        return paymentRepository.findByTransactionalRefference(transactionalRefference.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Malipo hayakupatikana."));
    }

    /** Oda inayoweza kupokea malipo: haijaghairiwa. */
    private Orders payableOrder(Long orderId) {
        // Tunafunga oda: malipo mawili ya wakati mmoja yasipite ukaguzi wa salio kwa pamoja.
        Orders order = ordersRepository.lockById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Oda haikupatikana."));
        if (OrderStatus.CANCELLED.equals(OrderStatus.effective(order))) {
            throw new IllegalArgumentException("Oda hii imeghairiwa; haipokei malipo.");
        }
        return order;
    }

    @Override
    @Transactional
    public Payment createPayment(Long orderId, PaymentRequestDto dto) {
        validateId(orderId, "Order id is required");
        if (dto == null) {
            throw new IllegalArgumentException("Payment details are required");
        }
        String method = normalizeMethod(dto.getMethod());
        Orders order = payableOrder(orderId);

        // Ikiwa mteja tayari ana chaguo la malipo lililo wazi, litumike tena (hakuna rekodi marudufu).
        for (Payment existing : paymentRepository.findByOrderIdOrderByCreatedAtAsc(orderId)) {
            if (STATUS_PENDING.equals(existing.getStutus())) {
                existing.setMethod(method);
                return paymentRepository.save(existing);
            }
        }

        BigDecimal balance = finance.balance(order);
        if (balance.signum() <= 0) {
            throw new IllegalArgumentException("Oda hii haina deni lililobaki.");
        }
        // Kiasi cha mteja (ikitolewa) kinaweza kuwa cha sehemu, lakini si zaidi ya deni. Kamwe hatuamini zaidi ya hapo.
        BigDecimal amount = balance;
        if (dto.getAmount() != null) {
            if (dto.getAmount().signum() <= 0 || dto.getAmount().compareTo(balance) > 0) {
                throw new IllegalArgumentException("Kiasi cha malipo si sahihi.");
            }
            amount = dto.getAmount();
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(amount);
        payment.setCurrency(resolveCurrency(dto.getCurrency()));
        payment.setMethod(method);
        payment.setStutus(STATUS_PENDING);
        payment.setVerificationStatus(UNVERIFIED);
        payment.setRecordedBy("customer");
        payment.setCreatedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public Payment markPaymentAsPaid(Long paymentId, String reference, BigDecimal amount, String actor) {
        validateText(reference, "Transaction reference is required");
        Payment payment = getPaymentById(paymentId);
        Orders order = payableOrder(orderOf(payment).getId());
        if (!STATUS_PENDING.equals(payment.getStutus())) {
            throw new IllegalArgumentException("Malipo haya yameshashughulikiwa.");
        }
        String cleanReference = reference.trim();
        if (paymentRepository.existsByTransactionalRefference(cleanReference)) {
            discrepancies.openIndependent(order.getStore().getId(), DiscrepancyService.PAYMENT, order.getOrderNumber(),
                    "Namba ya muamala " + cleanReference + " imejaribiwa kutumika mara ya pili kwa oda "
                            + order.getOrderNumber() + ".", actor);
            throw new IllegalArgumentException("Namba ya muamala tayari imetumika.");
        }
        BigDecimal received = amount != null ? amount : payment.getAmount();
        checkAmountAgainstBalance(order, received, actor, payment.getId());

        payment.setAmount(received);
        payment.setStutus(STATUS_RECEIVED);
        payment.setTransactionalRefference(cleanReference);
        payment.setPaidAt(LocalDateTime.now());
        payment.setRecordedBy(actor);
        Payment saved = paymentRepository.save(payment);
        audit.record(order.getStore().getId(), actor, "PAYMENT_RECORDED", "PAYMENT", saved.getId(), STATUS_PENDING,
                received.toPlainString() + " " + cleanReference, null);
        return saved;
    }

    @Override
    @Transactional
    public Payment recordPayment(Long orderId, BigDecimal amount, String method, String reference, String note, String actor) {
        validateId(orderId, "Order id is required");
        String cleanMethod = normalizeMethod(method);
        Orders order = payableOrder(orderId);
        String cleanReference = reference == null || reference.isBlank() ? null : reference.trim();

        // Pesa taslimu (CASH/COD) inaweza kukosa namba ya muamala; mobile money na benki zinahitaji.
        if (cleanReference == null && !(cleanMethod.equals("CASH") || cleanMethod.equals("COD"))) {
            throw new IllegalArgumentException("Weka namba ya muamala.");
        }
        if (cleanReference != null && paymentRepository.existsByTransactionalRefference(cleanReference)) {
            discrepancies.openIndependent(order.getStore().getId(), DiscrepancyService.PAYMENT, order.getOrderNumber(),
                    "Namba ya muamala " + cleanReference + " imejaribiwa kutumika mara ya pili kwa oda "
                            + order.getOrderNumber() + ".", actor);
            throw new IllegalArgumentException("Namba ya muamala tayari imetumika.");
        }
        checkAmountAgainstBalance(order, amount, actor, null);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(amount);
        payment.setCurrency("TZS");
        payment.setMethod(cleanMethod);
        payment.setStutus(STATUS_RECEIVED);
        payment.setVerificationStatus(UNVERIFIED);
        payment.setTransactionalRefference(cleanReference);
        payment.setPaidAt(LocalDateTime.now());
        payment.setRecordedBy(actor);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setNote(note == null || note.isBlank() ? null : note.trim());
        Payment saved = paymentRepository.save(payment);
        audit.record(order.getStore().getId(), actor, "PAYMENT_RECORDED", "PAYMENT", saved.getId(), null,
                amount.toPlainString() + " " + cleanMethod, note);
        return saved;
    }

    @Override
    @Transactional
    public Payment verifyPayment(Long paymentId, String actor) {
        Payment payment = getPaymentById(paymentId);
        Orders order = payableOrder(orderOf(payment).getId());
        if (!STATUS_RECEIVED.equals(payment.getStutus()) || !UNVERIFIED.equals(payment.getVerificationStatus())) {
            throw new IllegalArgumentException("Malipo haya hayasubiri uthibitisho.");
        }
        // Angalia tena salio: huenda malipo mengine yamehakikiwa tangu yalipoandikwa.
        checkAmountAgainstBalance(order, payment.getAmount(), actor, paymentId);

        payment.setVerificationStatus(OrderFinanceService.VERIFIED);
        payment.setVerifiedBy(actor);
        payment.setVerifiedAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        audit.record(order.getStore().getId(), actor, "PAYMENT_VERIFIED", "PAYMENT", paymentId, UNVERIFIED,
                OrderFinanceService.VERIFIED, payment.getAmount().toPlainString());
        return saved;
    }

    @Override
    @Transactional
    public Payment rejectPayment(Long paymentId, String reason, String actor) {
        Payment payment = getPaymentById(paymentId);
        Orders order = orderOf(payment);
        if (OrderFinanceService.VERIFIED.equals(payment.getVerificationStatus())) {
            throw new IllegalArgumentException("Malipo yaliyohakikiwa hayawezi kukataliwa; yarekebishwe kwa kumbukumbu ya marekebisho.");
        }
        if (REJECTED.equals(payment.getVerificationStatus())) {
            throw new IllegalArgumentException("Malipo haya tayari yamekataliwa.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Andika sababu ya kukataa.");
        }
        String clean = reason.trim().length() > 255 ? reason.trim().substring(0, 255) : reason.trim();
        String before = payment.getStutus() + "/" + payment.getVerificationStatus();
        payment.setVerificationStatus(REJECTED);
        payment.setStutus(STATUS_FAILED);
        payment.setVerifiedBy(actor);
        payment.setVerifiedAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        discrepancies.open(order.getStore().getId(), DiscrepancyService.PAYMENT, order.getOrderNumber(),
                "Malipo ya TSh " + payment.getAmount().toPlainString() + " ya oda " + order.getOrderNumber()
                        + " yamekataliwa: " + clean, actor);
        audit.record(order.getStore().getId(), actor, "PAYMENT_REJECTED", "PAYMENT", paymentId, before, REJECTED, clean);
        return saved;
    }

    @Override
    @Transactional
    public Payment markPaymentAsFailed(Long paymentId, String actor) {
        Payment payment = getPaymentById(paymentId);
        if (OrderFinanceService.VERIFIED.equals(payment.getVerificationStatus())) {
            throw new IllegalArgumentException("Malipo yaliyohakikiwa hayawezi kuwekwa kuwa yameshindikana.");
        }
        payment.setStutus(STATUS_FAILED);
        Payment saved = paymentRepository.save(payment);
        Orders order = orderOf(payment);
        audit.record(order.getStore().getId(), actor, "PAYMENT_FAILED", "PAYMENT", paymentId, null, STATUS_FAILED, null);
        return saved;
    }

    private Orders orderOf(Payment payment) {
        if (payment.getOrder() == null) {
            throw new IllegalArgumentException("Malipo haya ya zamani hayajaunganishwa na oda; wasiliana na msimamizi.");
        }
        return payment.getOrder();
    }

    /**
     * Kiasi lazima kiwe chanya na kisizidi deni lililobaki. Kikizidi: tunakataa NA kufungua kesi ya uchunguzi
     * (huenda ni kosa la kuandika, malipo mara mbili, au mteja amelipa zaidi).
     * "excludePaymentId" ni kwa ajili ya malipo yanayohakikiwa ambayo yenyewe bado hayajahesabiwa kwenye salio.
     */
    private void checkAmountAgainstBalance(Orders order, BigDecimal amount, String actor, Long excludePaymentId) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Kiasi lazima kiwe zaidi ya sifuri.");
        }
        BigDecimal balance = finance.balance(order);
        if (amount.compareTo(balance) > 0) {
            discrepancies.openIndependent(order.getStore().getId(), DiscrepancyService.PAYMENT, order.getOrderNumber(),
                    "Malipo ya TSh " + amount.toPlainString() + " yanazidi deni lililobaki (TSh "
                            + balance.toPlainString() + ") la oda " + order.getOrderNumber() + ". Tafadhali chunguza.", actor);
            throw new IllegalArgumentException("Kiasi kinazidi deni lililobaki la oda (TSh " + balance.toPlainString() + ").");
        }
    }

    private String normalizeMethod(String method) {
        validateText(method, "Payment method is required");
        String m = method.trim().toUpperCase();
        if (!METHODS.contains(m)) {
            throw new IllegalArgumentException("Njia ya malipo haikubaliki.");
        }
        return m;
    }

    private String resolveCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "TZS";
        }
        return currency.trim().toUpperCase();
    }

    private void validateId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
