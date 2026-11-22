package com.smart.instapreneur.Service;

import com.smart.instapreneur.Model.Customer;

public interface CustomerService {
    // Return or create the profile belonging to the authenticated account.
    Customer getMyCustomer(String email);
    Customer createMyCustomer(Customer customer, String email);
    Customer updateMyCustomer(Customer customer, String email);
}
