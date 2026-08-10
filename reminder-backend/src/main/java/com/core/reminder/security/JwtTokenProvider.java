package com.core.reminder.security;

import com.common.reminder.model.AppUser; // Assuming AppUser is your user model
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationInMs}")
    private long jwtExpirationInMs;

    private Key key;

    @PostConstruct
    public void init() {
        // Ensure the secret key is strong enough for the algorithm (HS512 requires at least 512 bits)
        // For production, use a securely generated key stored outside the codebase.
        if (jwtSecret == null || jwtSecret.length() < 64) {
             logger.warn("JWT Secret is not set or too short in properties. Using a default insecure key. THIS IS NOT SAFE FOR PRODUCTION!");
             // Generate a temporary, insecure key for development ONLY if not properly configured
             this.key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
             // Log the temporary key for dev purposes if needed, BUT NEVER IN PRODUCTION LOGS
             // logger.info("Using temporary JWT key: {}", java.util.Base64.getEncoder().encodeToString(this.key.getEncoded()));
        } else {
            byte[] keyBytes = java.util.Base64.getDecoder().decode(jwtSecret);
            this.key = Keys.hmacShaKeyFor(keyBytes);
        }
    }

    public String generateToken(Authentication authentication) {
        // In a real app, you'd likely get the UserDetails implementation from authentication.getPrincipal()
        // For simplicity, let's assume the principal is the user's email (or username)
        // Or better, if UserDetailsServiceImpl returns a custom UserDetails object holding the AppUser
        String username = authentication.getName(); // Or get user ID if preferred
        // You might want to cast principal to your custom UserDetails to get more info like user ID
        // UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(username) // Usually user ID or email
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
     
    // Overload for direct user info if Authentication object isn't readily available
    public String generateTokenFromUserDetails(AppUser user) {
         if (user == null) {
             throw new IllegalArgumentException("用户对象不能为空");
         }
         
         if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
             throw new IllegalArgumentException("用户名不能为空");
         }
         
         if (user.getId() == null) {
             throw new IllegalArgumentException("用户ID不能为空");
         }
         
         Date now = new Date();
         Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
 
         return Jwts.builder()
                 .setSubject(user.getUsername()) // 使用 username 作为 subject
                 // Add custom claims if needed, e.g., user ID, email, nickname
                 .claim("userId", user.getId())
                 .claim("nickname", user.getNickname() != null ? user.getNickname() : "")
                 .claim("email", user.getEmail() != null ? user.getEmail() : "") // 可以将 email 作为额外 claim 添加
                 .setIssuedAt(now)
                 .setExpiration(expiryDate)
                 .signWith(key, SignatureAlgorithm.HS512)
                 .compact();
     }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }
    
    public Long getUserIdFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // Assuming you added userId as a claim during generation
        return claims.get("userId", Long.class); 
    }


    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty.");
        }
        return false;
    }
} 