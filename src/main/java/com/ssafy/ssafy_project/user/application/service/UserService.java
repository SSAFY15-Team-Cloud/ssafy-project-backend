package com.ssafy.ssafy_project.user.application.service;

import com.ssafy.ssafy_project.global.application.port.out.JwtPortOut;
import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.global.infrastructure.config.ProfileImageProperties;
import com.ssafy.ssafy_project.user.application.port.in.*;
import com.ssafy.ssafy_project.user.application.port.out.ProfileImageStoragePortOut;
import com.ssafy.ssafy_project.user.application.port.out.LoadUserPortOut;
import com.ssafy.ssafy_project.user.application.port.out.UpdateUserPortOut;
import com.ssafy.ssafy_project.user.application.support.ProfileImageUrlResolver;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements GetMyInfoPortIn, UpdateNicknamePortIn, ChangePasswordPortIn, WithdrawUserPortIn, GenerateProfileImageUploadUrlPortIn, ChangeProfileImagePortIn, DeleteProfileImagePortIn {

    private final LoadUserPortOut loadUserPortOut;
    private final UpdateUserPortOut updateUserPortOut;
    private final ProfileImageStoragePortOut profileImageStoragePortOut;
    private final JwtPortOut jwtPortOut;

    private final UserWithdrawalProcessor userWithdrawalProcessor;

    private final PasswordEncoder passwordEncoder;
    private final ProfileImageUrlResolver profileImageUrlResolver;

    private final ProfileImageProperties profileImageProperties;

    @Override
    public MyInfoResult getMyInfo(GetMyInfoCommand command) {
        User user = getActiveUser(command.userId());

        return new MyInfoResult(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getName(),
                profileImageUrlResolver.resolve(user.getProfileImageKey())
        );
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        User user = getActiveUser(command.userId());

        if (!passwordEncoder.matches(command.currentPassword(), user.getPassword())) {
            throw new CustomException(CommonErrorCode.PASSWORD_MISMATCH);
        }

        if (passwordEncoder.matches(command.newPassword(), user.getPassword())) {
            throw new CustomException(CommonErrorCode.SAME_PASSWORD);
        }

        String encodedPassword = passwordEncoder.encode(command.newPassword());

        updateUserPortOut.changePassword(command.userId(), encodedPassword);
    }

    @Override
    @Transactional
    public void updateNickname(UpdateNicknameCommand command) {
        User user = getActiveUser(command.userId());

        if (user.getNickname().equals(command.nickname())) {
            return;
        }

        if (loadUserPortOut.existsActiveByNickname(command.nickname())) {
            throw new CustomException(CommonErrorCode.DUPLICATE_NICKNAME);
        }

        updateUserPortOut.updateNickname(command.userId(), command.nickname());
    }

    private User getActiveUser(Long userId) {
        User user = loadUserPortOut.loadById(userId);

        if (user.isDeleted()) {
            throw new CustomException(CommonErrorCode.USER_DELETED);
        }

        return user;
    }

    @Override
    @Transactional
    public void withdraw(WithdrawUserCommand command) {
        User user = getActiveUser(command.userId());

        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new CustomException(CommonErrorCode.PASSWORD_MISMATCH);
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
        return profileImageProperties.prefix() + "/" + userId + "/" + UUID.randomUUID() + ".jpg";
    }

    @Override
    @Transactional
    public void changeProfileImage(ChangeProfileImageCommand command) {
        User user = getActiveUser(command.userId());

        validateProfileImageKey(user.getId(), command.objectKey());

        String currentProfileImageKey = user.getProfileImageKey();

        if(command.objectKey().equals(currentProfileImageKey)) {
            return;
        }

        updateUserPortOut.changeProfileImage(user.getId(), command.objectKey());

        if(currentProfileImageKey != null && !currentProfileImageKey.isBlank()) {
            profileImageStoragePortOut.delete(currentProfileImageKey);
        }
    }

    private void validateProfileImageKey(Long userId, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new CustomException(CommonErrorCode.INVALID_OBJECT_KEY);
        }

        String prefix = profileImageProperties.prefix() + "/" + userId + "/";

        if (!objectKey.startsWith(prefix)) {
            throw new CustomException(CommonErrorCode.INVALID_OBJECT_KEY);
        }
    }

    @Override
    @Transactional
    public void deleteProfileImage(DeleteProfileImageCommand command) {
        User user = getActiveUser(command.userId());

        String profileImageKey = user.getProfileImageKey();

        if(profileImageKey == null || profileImageKey.isBlank()) {
            return;
        }

        updateUserPortOut.deleteProfileImage(user.getId());
        profileImageStoragePortOut.delete(profileImageKey);
    }
}
