package com.ssafy.ssafy_project.room.adapter.out.livekit;

import com.ssafy.ssafy_project.global.infrastructure.config.LiveKitProperties;
import com.ssafy.ssafy_project.room.application.port.out.RtcTokenPortOut;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * LiveKit access token은 HS256 JWT이므로 별도 SDK 없이 jjwt로 직접 서명한다.
 * https://docs.livekit.io/concepts/authentication/
 */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(LiveKitProperties.class)
public class LiveKitTokenAdapter implements RtcTokenPortOut {

    private static final Duration TOKEN_TTL = Duration.ofHours(6);

    private final LiveKitProperties liveKitProperties;

    private SecretKey signingKey;

    @PostConstruct
    private void init() {
        this.signingKey = Keys.hmacShaKeyFor(
                liveKitProperties.apiSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public String issueToken(String roomName, String identity, String displayName) {
        Instant now = Instant.now();

        Map<String, Object> videoGrant = Map.of(
                "room", roomName,
                "roomJoin", true,
                "canPublish", true,
                "canSubscribe", true,
                "canPublishData", true
        );

        return Jwts.builder()
                .issuer(liveKitProperties.apiKey())
                .subject(identity)
                .claim("name", displayName)
                .claim("video", videoGrant)
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now.minusSeconds(10)))
                .expiration(Date.from(now.plus(TOKEN_TTL)))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public String serverUrl() {
        return liveKitProperties.url();
    }
}
