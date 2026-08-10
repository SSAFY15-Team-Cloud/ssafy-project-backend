package com.ssafy.ssafy_project.user.adapter.in.web;

import com.ssafy.ssafy_project.support.ControllerIntegrationTestSupport;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class UserProfileImageControllerIntegrationTest extends ControllerIntegrationTestSupport {

    @BeforeEach
    void setUp() {
        clearPersistence();
    }

    @Test
    void generateProfileImageUploadUrl_returns_upload_url_and_object_key() throws Exception {
        UserJpaEntity user = saveUser("profile-upload@test.com", "password123!", "profile-user", "Profile User");

        mockMvc.perform(get("/api/users/me/profile-image/upload-url")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").value(org.hamcrest.Matchers.containsString("https://test-bucket.s3.amazonaws.com/")))
                .andExpect(jsonPath("$.objectKey").value(org.hamcrest.Matchers.startsWith(profileImageProperties.prefix() + "/" + user.getId() + "/")))
                .andExpect(jsonPath("$.objectKey").value(org.hamcrest.Matchers.endsWith(".jpg")));
    }

    @Test
    void generateProfileImageUploadUrl_requires_authentication() throws Exception {
        mockMvc.perform(get("/api/users/me/profile-image/upload-url"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changeUserProfileImage_updates_profile_image_key() throws Exception {
        UserJpaEntity user = saveUser("profile-change@test.com", "password123!", "profile-change", "Profile Change");

        mockMvc.perform(post("/api/users/me/profile-image")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                              {
                                "objectKey": "%s/%d/test-image.jpg"
                              }
                              """.formatted(profileImageProperties.prefix(), user.getId())))
                .andExpect(status().isNoContent());

        UserJpaEntity updatedUser = userJpaRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getProfileImageKey()).isEqualTo(profileImageProperties.prefix() + "/" + user.getId() + "/test-image.jpg");
    }

    @Test
    void changeUserProfileImage_rejects_blank_object_key() throws Exception {
        UserJpaEntity user = saveUser("profile-blank@test.com", "password123!", "profile-blank", "Profile Blank");

        mockMvc.perform(post("/api/users/me/profile-image")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                              {
                                "objectKey": ""
                              }
                              """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeUserProfileImage_rejects_other_users_object_key() throws Exception {
        UserJpaEntity user = saveUser("profile-invalid@test.com", "password123!", "profile-invalid", "Profile Invalid");

        mockMvc.perform(post("/api/users/me/profile-image")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                          {
                            "objectKey": "%s/999/test-image.jpg"
                          }
                          """.formatted(profileImageProperties.prefix())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("40012"));
    }

    @Test
    void deleteUserProfileImage_clears_profile_image_key() throws Exception {
        UserJpaEntity user = saveUser("profile-delete@test.com", "password123!", "profile-delete", "Profile Delete");
        user.changeProfileImageKey(profileImageProperties.prefix() + "/" + user.getId() + "/existing-image.jpg");
        userJpaRepository.save(user);

        mockMvc.perform(delete("/api/users/me/profile-image")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(user.getId())))
                .andExpect(status().isNoContent());

        UserJpaEntity updatedUser = userJpaRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getProfileImageKey()).isNull();
        assertThat(profileImageStoragePortOut.deletedKeys()).contains(profileImageProperties.prefix() + "/" + user.getId() + "/existing-image.jpg");
    }

    @Test
    void deleteUserProfileImage_returns_no_content_when_profile_image_does_not_exist() throws Exception {
        UserJpaEntity user = saveUser("profile-delete-none@test.com", "password123!", "profile-delete-none", "Profile Delete None");

        mockMvc.perform(delete("/api/users/me/profile-image")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(user.getId())))
                .andExpect(status().isNoContent());

        assertThat(profileImageStoragePortOut.deletedKeys()).isEmpty();
    }

    @Test
    void deleteUserProfileImage_requires_authentication() throws Exception {
        mockMvc.perform(delete("/api/users/me/profile-image"))
                .andExpect(status().isUnauthorized());
    }
}
