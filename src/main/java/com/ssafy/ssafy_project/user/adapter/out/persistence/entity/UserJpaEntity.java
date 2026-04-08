package com.ssafy.ssafy_project.user.adapter.out.persistence.entity;

import com.ssafy.ssafy_project.user.domain.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "app_user")
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "nickname", unique = true)
    private String nickname;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "profile_image_key")
    private String profileImageKey;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @CreationTimestamp
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @UpdateTimestamp
    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    public UserJpaEntity(String email, String password, UserRole role, String nickname, String name) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.nickname = nickname;
        this.name = name;
        this.deleted = false;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }

    public void softDelete() {
        this.deleted = true;
    }
}
