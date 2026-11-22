package com.smart.instapreneur.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smart.instapreneur.Dto.GuestOrderRequest;
import com.smart.instapreneur.Dto.GuestOrderResponse;
import com.smart.instapreneur.Dto.GuestOrderTrackingResponse;
import com.smart.instapreneur.Exception.IllegalArgumentException;
import com.smart.instapreneur.Exception.ResourceNotFoundException;
import com.smart.instapreneur.Model.Customer;
import com.smart.instapreneur.Model.OrderItems;
import com.smart.instapreneur.Model.Orders;
import com.smart.instapreneur.Model.Products;
import com.smart.instapreneur.Model.Store;
import com.smart.instapreneur.Repository.CustomerRepository;
import com.smart.instapreneur.Repository.OrdersRepository;
import com.smart.instapreneur.Repository.ProductRepository;

/**
 * Oda ya mteja asiye na akaunti na ufuatiliaji wake.
 *
 * MUHIMU: kuweka oda HAIPUNGUZI wala HAISHIKILII stock. Stock inashikiliwa tu mmiliki anapoidhinisha
 * (OrderWorkflowService.approve). Sababu: njia hii iko wazi kwa kila mtu; kama kila oda ingeshikilia stock,
 * mtu mmoja angeweza kutuma oda nyingi bandia na kuzuia wateja halisi kununua.
 */
@Service
public class GuestOrderService {
    private static final int MAX_TOTAL_QUANTITY_PER_PRODUCT = 100_000;

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrdersRepository ordersRepository;
    private final OrderFinanceService finance;

    public GuestOrderService(ProductRepository productRepository, CustomerRepository customerRepository,
            OrdersRepository ordersRepository, OrderFinanceService finance) {
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.ordersRepository = ordersRepository;
        this.finance = finance;
    }

    @Transactional
    public GuestOrderResponse placeOrder(GuestOrderRequest request) {
        // Unganisha mistari ya bidhaa moja iliyorudiwa.
        Map<Long, Integer> wanted = new LinkedHashMap<>();
        for (GuestOrderRequest.Item item : request.items()) {
            wanted.merge(item.productId(), item.quantity(), Integer::sum);
        }

        Orders order = new Orders();
        List<OrderItems> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        Store store = null;
        boolean stockNeedsReview = false;

        for (Map.Entry<Long, Integer> entry : wanted.entrySet()) {
            Products product = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Bidhaa haipo: " + entry.getKey()));
            int quantity = entry.getValue();
            if (quantity > MAX_TOTAL_QUANTITY_PER_PRODUCT) {
                throw new IllegalArgumentException("Kiasi ni kikubwa mno kwa bidhaa: " + product.getName());
            }
            if ("INACTIVE".equalsIgnoreCase(product.getStatus())) {
                throw new IllegalArgumentException("Bidhaa haipatikani kwa sasa: " + product.getName());
            }
            // Bei lazima iwe halali; usikubali bidhaa isiyo na bei.
            if (product.getPrice() == null || product.getPrice().signum() <= 0) {
                throw new IllegalArgumentException("Bei ya bidhaa si sahihi: " + product.getName());
            }
            // Mteja anaweza kuomba kiasi kinachozidi stock. Hatuonyeshi stock halisi; tunaweka oda
            // kwenye ukaguzi (STOCK_REVIEW) badala ya kuikataa au kuahidi bidhaa ambazo huenda hazipo.
            if (product.getStockQuantity() < quantity) {
                stockNeedsReview = true;
            }
            // Oda moja ni ya duka moja.
            if (store == null) {
                store = product.getStore();
            } else if (!store.getId().equals(product.getStore().getId())) {
                throw new IllegalArgumentException("Weka oda tofauti kwa kila duka.");
            }

            // Bei inachukuliwa kutoka database, si kutoka kwa mteja.
            BigDecimal subTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            total = total.add(subTotal);
            OrderItems line = new OrderItems();
            line.setOrder(order);
            line.setProduct(product);
            line.setQuantity(quantity);
            line.setUnitPrice(product.getPrice());
            line.setSubTotal(subTotal);
            lines.add(line);
        }

        order.setCustomer(findOrCreateCustomer(request));
        order.setStore(store);
        order.setDeliveryAddress(request.deliveryAddress().trim());
        order.setStatus(stockNeedsReview ? OrderStatus.STOCK_REVIEW : OrderStatus.NEW);
        order.setStockReserved(false);
        // Namba ya oda inaonekana kwenye risiti; token ya ufuatiliaji ni siri tofauti, isiyotabirika.
        order.setOrderNumber(OrderIdentifiers.newOrderNumber());
        order.setTrackingToken(OrderIdentifiers.newTrackingToken());
        order.setTotalAmount(total);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setOrderItems(lines); // cascade ALL huhifadhi mistari pia

        Orders saved = ordersRepository.save(order);
        String message = stockNeedsReview
                ? "Oda imepokelewa, lakini kiasi ulichoomba kinahitaji kuthibitishwa na kampuni. Hii si uthibitisho wa upatikanaji."
                : "Oda imepokelewa. Kampuni itathibitisha upatikanaji na hatua inayofuata.";
        return new GuestOrderResponse(saved.getId(), saved.getOrderNumber(), saved.getTrackingToken(),
                saved.getStatus(), saved.getTotalAmount(), saved.getCreatedAt(), stockNeedsReview, message);
    }

    /** Token ni siri isiyotabirika; inaonyesha oda MOJA tu na inafungwa oda ikikamilika au ikighairiwa. */
    @Transactional(readOnly = true)
    public GuestOrderTrackingResponse trackOrder(String token) {
        String notFound = "Oda haikupatikana au kiungo chake kimeisha muda.";
        if (token == null || token.length() < 30 || token.length() > 64) {
            throw new ResourceNotFoundException(notFound);
        }
        Orders order = ordersRepository.findByTrackingToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(notFound));

        String status = OrderStatus.effective(order);
        if (OrderStatus.trackingClosed(status)) {
            // Ujumbe ule ule kwa oda iliyokamilika au iliyoghairiwa: haidokezi chochote zaidi.
            throw new ResourceNotFoundException("Ufuatiliaji wa oda hii umefungwa kwa sababu oda imekamilika au imeghairiwa.");
        }
        List<GuestOrderTrackingResponse.Item> items = order.getOrderItems().stream()
                .map(item -> new GuestOrderTrackingResponse.Item(
                        item.getProduct().getName(), item.getQuantity(), item.getUnitPrice(), item.getSubTotal()))
                .toList();

        BigDecimal paid = finance.verifiedPaid(order.getId());
        BigDecimal balance = finance.balance(order);
        boolean needsConfirmation = OrderStatus.STOCK_REVIEW.equals(status);
        Store store = order.getStore();
        return new GuestOrderTrackingResponse(order.getOrderNumber(), status, OrderStatus.labelSw(status),
                order.getTotalAmount(), finance.paymentState(paid, balance), order.getCreatedAt(), items, true,
                needsConfirmation, store == null ? null : store.getWhatsappNumber(),
                store == null ? null : store.getStoreName(), OrderStatus.labelSw(status) + ".");
    }

    /** Namba ya simu ndiyo kitambulisho: mteja yule yule akirudi, taarifa zake zinatumika tena. */
    private Customer findOrCreateCustomer(GuestOrderRequest request) {
        String phone = normalizePhone(request.phone());
        Customer customer = customerRepository.findByPhone(phone).orElseGet(Customer::new);

        // Usibadilishe wasifu wa mteja mwenye akaunti kupitia ombi la wazi.
        if (customer.getId() == null || customer.getUser() == null) {
            customer.setPhone(phone);
            customer.setFirstName(request.firstName().trim());
            customer.setLastName(request.lastName().trim());
            customer.setLocation(request.location() == null ? null : request.location().trim());
            customer = customerRepository.save(customer);
        }
        return customer;
    }

    /** 0712 345 678 na +255712345678 vyote vinakuwa 255712345678. */
    private String normalizePhone(String raw) {
        String digits = raw.replaceAll("^(?://+255|0)[67]//d{8}$", "");
        if (digits.startsWith("0")) {
            digits = "255" + digits.substring(1);
        }
        return digits;
    }
}
