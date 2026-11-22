package com.smart.faidahalisi.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smart.faidahalisi.Exception.IllegalArgumentException;
import com.smart.faidahalisi.Exception.ResourceNotFoundException;
import com.smart.faidahalisi.Model.Customer;
import com.smart.faidahalisi.Model.User;
import com.smart.faidahalisi.Repository.CustomerRepository;
import com.smart.faidahalisi.Repository.UserRepository;

@Service
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getMyCustomer(String email) {
        return customerRepository.findByUserEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer profile haijaundwa. Tumia POST /api/customers/me kwanza."));
    }

    @Override
    @Transactional
    public Customer createMyCustomer(Customer input, String email) {
        validateCustomer(input);
        String normalizedEmail = normalizeEmail(email);

        // Never accept a userId from JSON. The account is taken from the verified JWT.
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Akaunti haijapatikana."));

        if (!"CUSTOMER".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("Akaunti hii si ya CUSTOMER. Sajili akaunti ya mteja kwanza.");
        }
        if (customerRepository.existsByUserEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Akaunti hii tayari ina customer profile.");
        }

        String phone = normalizePhone(input.getPhone());
        if (customerRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("Namba hii tayari imeshasajiliwa.");
        }

        Customer customer = new Customer();
        customer.setFirstName(user.getFirstName());
        customer.setLastName(user.getLastName());
        customer.setPhone(phone);
        customer.setLocation(clean(input.getLocation()));
        customer.setUser(user);
        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public Customer updateMyCustomer(Customer input, String email) {
        validateCustomer(input);
        Customer existing = getMyCustomer(email);
        String phone = normalizePhone(input.getPhone());

        // Allow keeping the current number, but prevent taking another customer's number.
        if (!existing.getPhone().equals(phone) && customerRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("Namba hii tayari imeshasajiliwa.");
        }
        existing.setPhone(phone);
        existing.setLocation(clean(input.getLocation()));
        return customerRepository.save(existing);
    }

    private void validateCustomer(Customer customer) {
        if (customer == null) throw new IllegalArgumentException("Taarifa za mteja zinahitajika.");
        normalizePhone(customer.getPhone());
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email inahitajika.");
        return email.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("Namba ya simu inahitajika.");
        String value = phone.trim();
        if (!value.matches("^(?:\\+255[67]\\d{8}|0[67]\\d{8})$")) {
            throw new IllegalArgumentException("Tumia 06xxxxxxxx, 07xxxxxxxx, +2556xxxxxxxx au +2557xxxxxxxx.");
        }
        return value.startsWith("0") ? "+255" + value.substring(1) : value;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
