package com.smart.instapreneur.Controller;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import java.util.Map;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.smart.instapreneur.Dto.AuthResponse;
import com.smart.instapreneur.Dto.LoginRequest;
import com.smart.instapreneur.Model.User;
import com.smart.instapreneur.Repository.UserRepository;
import com.smart.instapreneur.Security.JwtService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final long expirationMs;
    private final UserRepository userRepository;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository,
            @Value("${app.security.jwt.expiration-ms:3600000}") long expirationMs) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.expirationMs = expirationMs;
    }

    // POST /api/auth/login hupokea email/password, huzithibitisha na kurudisha JWT.
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtService.generateToken(userDetails);
            String role = userDetails.getAuthorities().stream()
                    .findFirst().map(authority -> authority.getAuthority()).orElse("ROLE_SELLER");

            // Kitambulisho cha mtumiaji kinasaidia frontend kutambua duka la muuzaji bila kutumia email ya umma.
            Long userId = userRepository.findByEmail(email).map(User::getId).orElse(null);
            return ResponseEntity.ok(new AuthResponse(
                    token, expirationMs / 1000, userDetails.getUsername(), role, userId));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Email au password si sahihi."));
        }
    }

    // GET /api/auth/me restores the authenticated frontend session after a reload.
    @GetMapping("/me")
    public ResponseEntity<User> currentUser(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Akaunti haijapatikana."));
        return ResponseEntity.ok(user);
    }
}
