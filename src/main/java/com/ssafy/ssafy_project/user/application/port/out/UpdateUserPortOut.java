package com.ssafy.ssafy_project.user.application.port.out;

public interface UpdateUserPortOut {
    void updateNickname(Long userId, String nickname);
    void changePassword(Long userId, String password);
    void changeProfileImage(Long userId, String objectKey);
    void withdraw(Long userId);
}
