
 package com.smart.instapreneur.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import com.smart.instapreneur.Model.User;
import com.smart.instapreneur.Service.UserService;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Public registration endpoint. UserService hashes the password and
    // only accepts SELLER or CUSTOMER; clients cannot register as ADMIN.
    // POST /api/users husajili akaunti; UserService huhash password kabla ya kuhifadhi.
    @PostMapping({"/users", "/auth/register"})
    public ResponseEntity<User> register(@RequestBody User user) {
        User created = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

}