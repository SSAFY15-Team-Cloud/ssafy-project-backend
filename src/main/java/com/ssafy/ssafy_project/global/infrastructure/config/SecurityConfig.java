package com.ssafy.ssafy_project.global.infrastructure.config;


import com.ssafy.ssafy_project.global.application.port.out.JwtPortOut;
import com.ssafy.ssafy_project.global.infrastructure.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
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

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    private static final String[] whiteList = {
            "/api/auth/**",
            "/api/shared/**",
            "/ws",
            "/ws/**"
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
                exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, authException)->{
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        })
                        .accessDeniedHandler((req,res,authException)->{
                            res.sendError(HttpServletResponse.SC_FORBIDDEN);
                        }))
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration ccf = new CorsConfiguration();
        ccf.setAllowedOrigins(List.of(frontendBaseUrl));
        ccf.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        ccf.setAllowCredentials(true);
        ccf.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
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
