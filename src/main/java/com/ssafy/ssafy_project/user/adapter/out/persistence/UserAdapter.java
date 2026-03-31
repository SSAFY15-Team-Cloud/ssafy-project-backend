package com.ssafy.ssafy_project.user.adapter.out.persistence;

import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import com.ssafy.ssafy_project.user.adapter.out.persistence.repository.UserJpaRepository;
import com.ssafy.ssafy_project.user.application.port.out.LoadUserPortOut;
import com.ssafy.ssafy_project.user.application.port.out.RegisterUserPortOut;
import com.ssafy.ssafy_project.user.application.port.out.UpdateUserPortOut;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAdapter implements LoadUserPortOut, RegisterUserPortOut, UpdateUserPortOut {

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

    @Override
    public User loadById(Long userId) {
        UserJpaEntity userJpaEntity = userJpaRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        return toDomain(userJpaEntity);
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
    public void updateNickname(Long userId, String nickname) {
        UserJpaEntity userJpaEntity = userJpaRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        userJpaEntity.updateNickname(nickname);
    }

    @Override
    public void changePassword(Long userId, String password) {
        UserJpaEntity userJpaEntity = userJpaRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        userJpaEntity.changePassword(password);
    }
}
