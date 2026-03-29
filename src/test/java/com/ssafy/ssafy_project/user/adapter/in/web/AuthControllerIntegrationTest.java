package com.ssafy.ssafy_project.user.adapter.in.web;

import com.ssafy.ssafy_project.global.domain.entity.TokenType;
import com.ssafy.ssafy_project.support.ControllerIntegrationTestSupport;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends ControllerIntegrationTestSupport {

    @BeforeEach
    void setUp() {
        clearPersistence();
    }

    @Test
    void signup_persists_user_and_returns_id() throws Exception {
        String requestBody = """
                {
                  "email": "signup@test.com",
                  "password": "password123!",
                  "nickname": "signup-user",
                  "name": "Signup User"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        UserJpaEntity savedUser = userJpaRepository.findByEmail("signup@test.com").orElseThrow();

        assertThat(savedUser.getEmail()).isEqualTo("signup@test.com");
        assertThat(savedUser.getNickname()).isEqualTo("signup-user");
        assertThat(passwordEncoder.matches("password123!", savedUser.getPassword())).isTrue();
    }

    @Test
    void login_returns_access_token_and_refresh_cookie() throws Exception {
        saveUser("login@test.com", "password123!", "login-user", "Login User");

        String requestBody = """
                {
                  "email": "login@test.com",
                  "password": "password123!"
                }
                """;

        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refreshToken=")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(responseBody).get("accessToken").asText();
        assertThat(jwtTokenProvider.validateToken(accessToken, TokenType.ACCESS_TOKEN)).isTrue();
    }

    @Test
    void reissue_returns_new_access_token_when_refresh_cookie_is_present() throws Exception {
        saveUser("reissue@test.com", "password123!", "reissue-user", "Reissue User");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "reissue@test.com",
                                  "password": "password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);

        String refreshToken = extractCookieValue(loginResponse, "refreshToken");

        String responseBody = mockMvc.perform(post("/api/auth/reissue")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refreshToken=")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = objectMapper.readTree(responseBody).get("accessToken").asText();
        assertThat(jwtTokenProvider.validateToken(accessToken, TokenType.ACCESS_TOKEN)).isTrue();
    }

    @Test
    void logout_deletes_refresh_token_and_expires_cookie() throws Exception {
        UserJpaEntity savedUser = saveUser("logout@test.com", "password123!", "logout-user", "Logout User");

        String setCookieHeader = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "logout@test.com",
                                  "password": "password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);

        String refreshToken = extractCookieValue(setCookieHeader, "refreshToken");
        assertThat(refreshTokenPortOut.find(savedUser.getId())).contains(refreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("refreshToken", 0));

        assertThat(refreshTokenPortOut.find(savedUser.getId())).isEmpty();
    }
}
