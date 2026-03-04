package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.entity.base.BaseUpdatableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

@Getter
@NoArgsConstructor
public class User extends BaseUpdatableEntity {
    private String email;
    private String username;
    private String password;
    private String birthday;

    private UUID profileId; // 삭제 예정

    private BinaryContent profile; // 프로필 이미지
    private UserStatus status;

    // 생성자
    public User(String email, String username, String password, String birthday) {
        this.email = email;
        this.username = username;
        this.password = password; // 해싱?
        this.birthday = birthday;
        this.profileId = null;
    }

    @Override
    public String toString() {
        return "User{" +
                "id = " + getId() + ", " +
//                "createdAt = " + getCreatedAt() + ", " +
//                "updatedAt = " + getUpdatedAt() + ", " +
                "newEmail = " + email + ", " +
                "newUsername = " + username + ", " +
//                "newPassword = " + newPassword + ", " +
                "newBirthday = " + birthday + ", " +
                "}";
    }

    // update
    public void updateEmail(String email) {
        this.email = email;
    }

    public void updateUserName(String username) {
        this.username = username;
    }

    // 해시??
    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateBirthday(String birthday) {
        this.birthday = birthday;
    }

    public void updateProfileId(UUID profileId) { // BinaryContent의 id
        this.profileId = profileId;
    }
}
