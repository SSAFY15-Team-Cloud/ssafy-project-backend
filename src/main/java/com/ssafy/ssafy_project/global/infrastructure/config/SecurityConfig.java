package com.ssafy.ssafy_project.global.infrastructure.config;


import com.ssafy.ssafy_project.global.application.port.out.JwtPortOut;
import com.ssafy.ssafy_project.global.infrastructure.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/*
인증 없이 접근할 수 있는 URL과
인증 있이 접근할 수 있는 URL

웹에 접근했을 때, 내가 만든 AutenticationFilter를 적용
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtPortOut jwtPortOut;

    private static final String[] whiteList = {
            "/api/auth/**"
    };

    // 작업 순위1. CSRF
    // Security filter chain을 통해서 처리
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // csrf란? 위조방지토큰. form태그에서 악성으로 건드릴 수 있으므로.
        // 프론트가 csrf를 무작위로 생성해서 보낸다.
        // 우리는 API서버이므로 form을 안쓰니까 CSRF를 disable
        // formLogin은 Security의 기본 로그인폼 비활성화
        // httpBasic은 .... 나중에
        // sessionManagement token으로 관리할거임
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        .requestMatchers(whiteList).permitAll()
                        .anyRequest().authenticated()
                ).
                addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration ccf = new CorsConfiguration();
        // TODO : EndPoint 설정 필요 [ Front ]
        ccf.setAllowedOrigins(List.of("http://localhost:8081/front"));
        ccf.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        ccf.setAllowCredentials(true);
//        ccf.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
        // TODO : 개발용 임시 Allow
        ccf.setAllowedOriginPatterns(List.of("*"));
        ccf.setAllowedHeaders(List.of("*"));
        ccf.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 모든 URL에 대해 위의 CORS 적용
        source.registerCorsConfiguration("/**", ccf);

        return source;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtPortOut);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
