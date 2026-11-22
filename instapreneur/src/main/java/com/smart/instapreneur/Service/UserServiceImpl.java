package com.smart.instapreneur.Service;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.smart.instapreneur.Exception.IllegalArgumentException;
import com.smart.instapreneur.Exception.ResourceNotFoundException;
import com.smart.instapreneur.Model.User;
import com.smart.instapreneur.Repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {
    // UserRepository is the database access layer for User records.
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor injection is used so Spring can provide the repository.
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Finds one user by email.
    @Override
    public User findUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("email hii: " + normalizedEmail + " haijasajiliwa"));
    }

    // Finds one user by id.
    @Override
    public User findUserById(Long id) {
        validateId(id, "User id is required");

        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    // Deletes one user by id.
    @Override
    public void deleteUserById(Long id) {
        User user = findUserById(id);
        userRepository.delete(user);
    }

    // Updates an existing user.
    @Override
    public User updateUser(Long id, User updatedUser) {
        validateId(id, "User id is required");
        validateUser(updatedUser, false);

        User existingUser = findUserById(id);
        String normalizedEmail = normalizeEmail(updatedUser.getEmail());

        // If the email is changing, make sure the new email is not already used.
        if (!existingUser.getEmail().equals(normalizedEmail) && userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email hii tayari imesajiliwa: " + normalizedEmail);
        }
        
        existingUser.setFirstName(updatedUser.getFirstName().trim());
        existingUser.setLastName(updatedUser.getLastName().trim());
        existingUser.setEmail(normalizedEmail);
        // Role is not changed through this general update endpoint; this prevents self-promotion.
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
        }

        return userRepository.save(existingUser);
    }
    // Creates a new user.
    @Override
    public User createUser(User newUser) {
        validateUser(newUser, true);

        String normalizedEmail = normalizeEmail(newUser.getEmail());

        // Check if the email already exists in the database.
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email hii tayari imesajiliwa: " + normalizedEmail);
        }

        newUser.setFirstName(newUser.getFirstName().trim());
        newUser.setLastName(newUser.getLastName().trim());
        newUser.setEmail(normalizedEmail);
        // Registration may choose a normal account type, but never ADMIN.
        // This keeps seller and customer sign-up possible without self-promotion.
        String requestedRole = newUser.getRole();
        if (requestedRole == null || requestedRole.isBlank()) {
            requestedRole = "SELLER"; // Existing frontend forms default to seller accounts.
        }
        requestedRole = requestedRole.trim().toUpperCase().replace("ROLE_", "");
        if (!requestedRole.equals("SELLER") && !requestedRole.equals("CUSTOMER")) {
            throw new IllegalArgumentException("Role inayoruhusiwa ni SELLER au CUSTOMER pekee.");
        }
        newUser.setRole(requestedRole);

        // BCrypt stores a one-way hash, never the raw password.
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        return userRepository.save(newUser);
    }
    // Checks required user fields before saving.
    private void validateUser(User user, boolean passwordRequired) {
        if (user == null) {
            throw new IllegalArgumentException("User details are required");
        }
        validateText(user.getFirstName(), "First name is required");
        validateText(user.getLastName(), "Last name is required");
        normalizeEmail(user.getEmail());

        if (passwordRequired && (user.getPassword() == null || user.getPassword().isBlank())) {
            throw new IllegalArgumentException("Password is required");
        }
    }
    // Converts email to a clean lowercase form after validating it.
    private String normalizeEmail(String email) {
        validateText(email, "Email is required");

        String normalizedEmail = email.trim().toLowerCase();
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

        if (!normalizedEmail.matches(emailRegex)) {
            throw new IllegalArgumentException("Email sio sahihi: " + email);
        }
        return normalizedEmail;
    }
    // Trims optional text and returns null when it is empty.
    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
    // Common id validation used by public methods in this service.
    private void validateId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
    // Common text validation used by public and private methods.
    private void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
