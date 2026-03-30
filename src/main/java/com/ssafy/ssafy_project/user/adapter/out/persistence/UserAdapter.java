package com.ssafy.ssafy_project.user.adapter.out.persistence;

import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import com.ssafy.ssafy_project.user.adapter.out.persistence.repository.UserJpaRepository;
import com.ssafy.ssafy_project.user.application.port.out.LoadUserPortOut;
import com.ssafy.ssafy_project.user.application.port.out.RegisterUserPortOut;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAdapter implements LoadUserPortOut, RegisterUserPortOut {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User loadByEmail(String email) {
        UserJpaEntity userJpaEntity = userJpaRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return toDomain(userJpaEntity);
    }

    @Override
    public User registerUser(User user) {
        UserJpaEntity userJpaEntity = userJpaRepository.save(
                new UserJpaEntity(
                        user.getEmail(),
                        user.getPassword(),
                        user.getRole(),
                        user.getNickname(),
                        user.getName()
                )
        );

        return toDomain(userJpaEntity);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    private User toDomain(UserJpaEntity userJpaEntity) {
        return new User(
                userJpaEntity.getId(),
                userJpaEntity.getEmail(),
                userJpaEntity.getPassword(),
                userJpaEntity.getRole(),
                userJpaEntity.getNickname(),
                userJpaEntity.getName(),
                userJpaEntity.getProfileImageUrl(),
                userJpaEntity.isDeleted()
        );
    }

    @Override
    public User loadById(Long userId) {
        UserJpaEntity userJpaEntity = userJpaRepository.findById(userId)
                .orElseThrow(()-> new RuntimeException("유저를 찾을 수 없습니다."));

//        public User(String email, String password, String nickname, String name, UserRole role, String profileImageUrl) {
//        this.id = null;
//        this.email = email;
//        this.password = password;
//        this.nickname = nickname;
//        this.name = name;
//        this.role = role;
//        this.profileImageUrl = profileImageUrl;
//    }

//    private Long id;
//
//    @Column(name = "email", nullable = false, unique = true)
//    private String email;
//
//    @Column(name = "password", nullable = false)
//    private String password;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "role", nullable = false)
//    private UserRole role;
//
//    @Column(name = "nickname")
//    private String nickname;
//
//    @Column(name = "name", nullable = false)
//    private String name;
//
//    @Column(name = "profile_image_url")
//    private String profileImageUrl;
//
//    @Column(name = "is_deleted", nullable = false)
//    private boolean deleted;
//
//    @CreationTimestamp
//    @Column(name = "created_time", nullable = false, updatable = false)
//    private LocalDateTime createdTime;
//
//    @UpdateTimestamp
//    @Column(name = "updated_time", nullable = false)
//    private LocalDateTime updatedTime;
        return new User(
                userJpaEntity.getId(),
                userJpaEntity.getEmail(),
                userJpaEntity.getPassword(),
                userJpaEntity.getRole(),
                userJpaEntity.getNickname(),
                userJpaEntity.getName(),
                userJpaEntity.getProfileImageUrl(),
                userJpaEntity.isDeleted()
        );
    }
}
