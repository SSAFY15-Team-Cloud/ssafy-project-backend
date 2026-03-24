package com.ssafy.ssafy_project.user.domain;

import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class User {
    @Id
    private Long id;
    private String username;
    private String password;



    public static User toDomain(Long id, String username, String password){
        return new User(id, username, password);
    }

}