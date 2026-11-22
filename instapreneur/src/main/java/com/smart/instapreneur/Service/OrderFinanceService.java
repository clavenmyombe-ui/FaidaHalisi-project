package com.smart.instapreneur.Service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smart.instapreneur.Model.Orders;
import com.smart.instapreneur.Model.OrderReturn;
import com.smart.instapreneur.Model.Payment;
import com.smart.instapreneur.Repository.OrderReturnRepository;
import com.smart.instapreneur.Repository.PaymentRepository;

/**
 * Hesabu za pesa za oda moja. Kanuni:
 *   deni (balance) = jumla ya oda - mkopo wa bidhaa zilizorudishwa - malipo YALIYOHAKIKIWA
 * Malipo ambayo hayajahakikiwa (au yaliyokataliwa) HAYAHESABIWI. Jumla ya oda pekee si ushahidi wa pesa kupokelewa.
 */
@Service
public class OrderFinanceService {
    public static final String VERIFIED = "VERIFIED";

    private final PaymentRepository paymentRepository;
    private final OrderReturnRepository returnRepository;

    public OrderFinanceService(PaymentRepository paymentRepository, OrderReturnRepository returnRepository) {
        this.paymentRepository = paymentRepository;
        this.returnRepository = returnRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal verifiedPaid(Long orderId) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Payment p : paymentRepository.findByOrderIdOrderByCreatedAtAsc(orderId)) {
            if (VERIFIED.equals(p.getVerificationStatus()) && !"FAILED".equals(p.getStutus())) {
                sum = sum.add(p.getAmount());
            }
        }
        return sum;
    }

    @Transactional(readOnly = true)
    public BigDecimal returnsCredit(Long orderId) {
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderReturn r : returnRepository.findByOrderId(orderId)) {
            sum = sum.add(r.getCreditAmount());
        }
        return sum;
    }

    @Transactional(readOnly = true)
    public BigDecimal balance(Orders order) {
        BigDecimal total = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
        return total.subtract(returnsCredit(order.getId())).subtract(verifiedPaid(order.getId()));
    }

    /** UNPAID | PARTIAL | PAID, kulingana na malipo yaliyohakikiwa pekee. */
    public String paymentState(BigDecimal paid, BigDecimal balance) {
        if (paid.signum() <= 0) return "UNPAID";
        return balance.signum() <= 0 ? "PAID" : "PARTIAL";
    }
}
