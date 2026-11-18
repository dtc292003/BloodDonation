package com.example.blooddonation.service;


import com.example.blooddonation.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {
    private final JwtEncoder jwtEncoder;
    private final long accessTokenMinutes;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${security.jwt.access-token-minutes:15}")
            long accessTokenMinutes
    ) {
        if (accessTokenMinutes <= 0) {
            throw new IllegalArgumentException(
                    "Thời hạn Access Token phải lớn hơn 0"
            );
        }

        this.jwtEncoder = jwtEncoder;
        this.accessTokenMinutes = accessTokenMinutes;
    }
    public String generateAccessToken(User user){
        Instant now = Instant.now();

        // Thời gian hết hạn
        Instant expireAt = now.plus(Duration.ofMinutes(accessTokenMinutes));

        String role = user.getRole()
                .getRoleName()
                .name();

        //Tạo playload JWT
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("blooddonation")
                .subject(String.valueOf(user.getUserId()))
                .issuedAt(now)
                .expiresAt(expireAt)
                .claim("role",role)
                .claim("token_type", "access")
                .claim("token_version", user.getTokenVersion())
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header,claims)
        ).getTokenValue();

    }

    public long getAccessTokenLifetimeSeconds(){
        return Duration.ofMinutes(accessTokenMinutes).toSeconds();
    }
}
