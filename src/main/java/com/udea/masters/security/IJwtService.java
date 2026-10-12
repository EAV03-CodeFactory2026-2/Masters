package com.udea.masters.security;

import java.util.List;

public interface IJwtService {
    String extractUserId(String token);
    String extractEmail(String token);
    List<String> extractRoles(String token);
    boolean isTokenValid(String token);
}
