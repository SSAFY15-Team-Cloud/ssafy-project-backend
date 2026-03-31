package com.ssafy.ssafy_project.user.application.service;

import com.ssafy.ssafy_project.global.application.port.out.JwtPortOut;
import com.ssafy.ssafy_project.global.domain.entity.Tokens;
import com.ssafy.ssafy_project.user.application.port.in.GetMyInfoPortIn;
import com.ssafy.ssafy_project.user.application.port.in.LoginPortIn;
import com.ssafy.ssafy_project.user.application.port.in.MyInfoResult;
import com.ssafy.ssafy_project.user.application.port.out.LoadUserPortOut;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements GetMyInfoPortIn {
    private final LoadUserPortOut loadUserPortOut;


    @Override
    public MyInfoResult getMyInfo(Long userId) {
        User user = loadUserPortOut.loadById(userId);

        if(user.isDeleted()) {
            throw new RuntimeException("삭제된 유저입니다.");
        }

        return new MyInfoResult(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getName(),
                user.getProfileImageUrl()
        );
    }
}
