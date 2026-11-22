package com.smart.faidahalisi.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smart.faidahalisi.Model.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByPhone(String phone);
    List<Customer> findByLocation(String location);
    boolean existsByPhone(String phone);

    // Finds the customer profile connected to the logged-in account's email.
    Optional<Customer> findByUserEmailIgnoreCase(String email);
    boolean existsByUserEmailIgnoreCase(String email);
  
}
