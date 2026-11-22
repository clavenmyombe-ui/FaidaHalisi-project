package com.smart.instapreneur.Dto;

public class AuthResponse {
    private final String tokenType = "Bearer";
    private final String accessToken;
    private final long expiresInSeconds;
    private final String email;
    private final String role;
    private final Long userId;

    public AuthResponse(String accessToken, long expiresInSeconds, String email, String role, Long userId) {
        this.accessToken = accessToken;
        this.expiresInSeconds = expiresInSeconds;
        this.email = email;
        this.role = role;
        this.userId = userId;
    }

    public String getTokenType() { return tokenType; }
    public String getAccessToken() { return accessToken; }
    public long getExpiresInSeconds() { return expiresInSeconds; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Long getUserId() { return userId; }
}
