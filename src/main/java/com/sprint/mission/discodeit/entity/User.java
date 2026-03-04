package com.sprint.mission.discodeit.entity;

import com.sprint.mission.discodeit.entity.base.BaseEntity;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

@Getter
public class User extends BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String email;
    private String username;
    private String password;
    private String birthday;

    private UUID profileId; // 프로필 이미지

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
        updateTime();
    }

    public void updateUserName(String username) {
        this.username = username;
        updateTime();
    }

    // 해시??
    public void updatePassword(String password) {
        this.password = password;
        updateTime();
    }

    public void updateBirthday(String birthday) {
        this.birthday = birthday;
        updateTime();
    }

    public void updateProfileId(UUID profileId) { // BinaryContent의 id
        this.profileId = profileId;
        updateTime();
    }
}
