package com.ssafy.ssafy_project.user.adapter.out.persistence;

import com.ssafy.ssafy_project.user.adapter.out.persistence.repository.UserJpaRepository;
import com.ssafy.ssafy_project.user.application.port.out.LoadUserPortOut;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAdapter implements LoadUserPortOut {

    private final UserJpaRepository userJpaRepository;


    @Override
    public User findByUsername(String username) {
//        UserJpaEntity jpaEntity = userJpaRepository.findByUsername(username)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        return User.toDomain(jpaEntity.getUserId(), jpaEntity.getUsername(), jpaEntity.getPassword());
        return new User(1L,"dbstjdqls14","1234");
    }
}
