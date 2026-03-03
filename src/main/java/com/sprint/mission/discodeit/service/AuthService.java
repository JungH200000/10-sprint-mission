package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.auth.LoginRequest;
import com.sprint.mission.discodeit.dto.user.response.UserResponse;

public interface AuthService {

    UserResponse login(LoginRequest loginRequest);
}
