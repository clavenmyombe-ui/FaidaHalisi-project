package com.smart.instapreneur.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.smart.instapreneur.Model.Customer;
import com.smart.instapreneur.Service.CustomerService;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // Create a profile for the account represented by the verified JWT.
    // The client must not send a customerId or userId to choose whose profile to create.
    // POST /api/customers/me huunda wasifu; user hutambuliwa kutoka JWT, si ID ya ombi.
    @PostMapping("/me")
    public ResponseEntity<Customer> createMyProfile(@RequestBody Customer customer, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.createMyCustomer(customer, authentication.getName()));
    }

    // Return only the profile connected to the currently logged-in account.
    // GET /api/customers/me husoma wasifu wa mwenye JWT iliyotumwa.
    @GetMapping("/me")
    public ResponseEntity<Customer> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(customerService.getMyCustomer(authentication.getName()));
    }

    // Only phone and location are updated by the service; account name/email stay tied to User.
    // PUT /api/customers/me husasisha taarifa zinazoruhusiwa za wasifu wa sasa.
    @PutMapping("/me")
    public ResponseEntity<Customer> updateMyProfile(@RequestBody Customer customer, Authentication authentication) {
        return ResponseEntity.ok(customerService.updateMyCustomer(customer, authentication.getName()));
    }
}
