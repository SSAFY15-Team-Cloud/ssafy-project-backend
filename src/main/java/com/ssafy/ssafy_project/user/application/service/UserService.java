package com.ssafy.ssafy_project.user.application.service;

import com.ssafy.ssafy_project.global.application.port.out.JwtPortOut;
import com.ssafy.ssafy_project.user.application.port.in.*;
import com.ssafy.ssafy_project.user.application.port.out.ProfileImageStoragePortOut;
import com.ssafy.ssafy_project.user.application.port.out.LoadUserPortOut;
import com.ssafy.ssafy_project.user.application.port.out.UpdateUserPortOut;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements GetMyInfoPortIn, UpdateNicknamePortIn, ChangePasswordPortIn, WithdrawUserPortIn, GenerateProfileImageUploadUrlPortIn {
    private final LoadUserPortOut loadUserPortOut;
    private final UpdateUserPortOut updateUserPortOut;
    private final ProfileImageStoragePortOut profileImageStoragePortOut;
    private final JwtPortOut jwtPortOut;
    private final UserWithdrawalProcessor userWithdrawalProcessor;
    private final PasswordEncoder passwordEncoder;

    @Override
    public MyInfoResult getMyInfo(GetMyInfoCommand command) {
        User user = getActiveUser(command.userId());

        return new MyInfoResult(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getName(),
                user.getProfileImageKey()
        );
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        User user = getActiveUser(command.userId());

        if(!passwordEncoder.matches(command.currentPassword(), user.getPassword())) {
            throw new RuntimeException("사용자 비밀번호가 일치하지 않습니다.");
        }

        if(passwordEncoder.matches(command.newPassword(), user.getPassword())) {
            throw new RuntimeException("동일한 비밀번호로는 변경이 불가합니다.");
        }

        String encodedPassword = passwordEncoder.encode(command.newPassword());

        updateUserPortOut.changePassword(command.userId(), encodedPassword);
    }

    @Override
    @Transactional
    public void updateNickname(UpdateNicknameCommand command) {
        User user = getActiveUser(command.userId());

        if(user.getNickname().equals(command.nickname())) {
            return;
        }

        if(loadUserPortOut.existsActiveByNickname(command.nickname())) {
            throw new RuntimeException("이미 사용하고 있는 닉네임입니다.");
        }

        updateUserPortOut.updateNickname(command.userId(), command.nickname());
    }

    private User getActiveUser(Long userId) {
        User user = loadUserPortOut.loadById(userId);

        if(user.isDeleted()) {
            throw new RuntimeException("삭제된 사용자입니다.");
        }

        return user;
    }

    @Override
    @Transactional
    public void withdraw(WithdrawUserCommand command) {
        User user = getActiveUser(command.userId());

        if(!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new RuntimeException("사용자 비밀번호가 일치하지 않습니다.");
        }

        userWithdrawalProcessor.process(command.userId());
        updateUserPortOut.withdraw(command.userId());
        jwtPortOut.deleteRefreshToken(command.userId());
    }

    @Override
    public GenerateProfileImageUploadUrlResult generateUrl(GenerateProfileImageUploadUrlCommand command) {
        User user = getActiveUser(command.userId());

        String objectKey = generateProfileImageKey(user.getId());
        String uploadUrl = profileImageStoragePortOut.generateUploadUrl(objectKey);

        return new GenerateProfileImageUploadUrlResult(
                uploadUrl,
                objectKey
        );
    }

    private String generateProfileImageKey(Long userId) {
        return userId + "/" + UUID.randomUUID() + ".jpg";
    }
}
