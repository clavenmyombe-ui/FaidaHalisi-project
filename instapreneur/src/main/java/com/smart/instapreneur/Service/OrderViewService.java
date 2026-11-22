package com.smart.instapreneur.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smart.instapreneur.Dto.OrderResponse;
import com.smart.instapreneur.Model.Customer;
import com.smart.instapreneur.Model.OrderItems;
import com.smart.instapreneur.Model.Orders;
import com.smart.instapreneur.Model.Payment;
import com.smart.instapreneur.Repository.PaymentRepository;

/** Hubadilisha entity ya Orders kuwa OrderResponse (data salama tu) pamoja na hesabu za malipo. */
@Service
public class OrderViewService {
    private final OrderFinanceService finance;
    private final PaymentRepository paymentRepository;

    public OrderViewService(OrderFinanceService finance, PaymentRepository paymentRepository) {
        this.finance = finance;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public OrderResponse toResponse(Orders o) {
        BigDecimal credit = finance.returnsCredit(o.getId());
        BigDecimal paid = finance.verifiedPaid(o.getId());
        BigDecimal total = o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount();
        BigDecimal balance = total.subtract(credit).subtract(paid);

        Customer c = o.getCustomer();
        OrderResponse.CustomerInfo customer = c == null ? null
                : new OrderResponse.CustomerInfo(c.getFirstName(), c.getLastName(), c.getPhone(), c.getLocation());

        List<OrderResponse.Line> lines = new ArrayList<>();
        if (o.getOrderItems() != null) {
            for (OrderItems i : o.getOrderItems()) {
                lines.add(new OrderResponse.Line(i.getProduct().getId(), i.getProduct().getName(),
                        i.getQuantity(), i.getUnitPrice(), i.getSubTotal()));
            }
        }
        List<OrderResponse.PaymentLine> payments = new ArrayList<>();
        for (Payment p : paymentRepository.findByOrderIdOrderByCreatedAtAsc(o.getId())) {
            payments.add(new OrderResponse.PaymentLine(p.getId(), p.getMethod(), p.getAmount(), p.getStutus(),
                    p.getVerificationStatus(), p.getTransactionalRefference(), p.getPaidAt(), p.getRecordedBy(),
                    p.getNote()));
        }
        String status = OrderStatus.effective(o);
        return new OrderResponse(o.getId(), o.getOrderNumber(), status, total, credit, paid, balance,
                finance.paymentState(paid, balance), o.getCreatedAt(), o.getDeliveredAt(), o.getDeliveryAddress(),
                o.getStore() == null ? null : o.getStore().getId(), o.isStockReserved(),
                OrderStatus.STOCK_REVIEW.equals(status), o.getCancelReason(), customer, lines, payments);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> toResponses(List<Orders> orders) {
        List<OrderResponse> out = new ArrayList<>();
        for (Orders o : orders) {
            out.add(toResponse(o));
        }
        return out;
    }
}
