package com.sprint.mission.discodeit.security.userdetails;

import com.sprint.mission.discodeit.dto.user.UserDto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
// Spring Security의 formLogin 인증 과정에서 사용되는 사용자 정보 객체
// 사용자가 인증에 성공 시, 이 객체가 Authentication의 principal로 저장됨
public class DiscodeitUserDetails implements UserDetails {

    private final UserDto userDto;
    private final String password;

    @Override
    public String getUsername() {
        return userDto.username();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    // 인증된 사용자가 가진 권한 목록 반환
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    // 계정 만료 여부
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    // 계정 잠금 여부
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    // 계정 비밀번호 만료 여부
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    // 계정 활성화 여부
    public boolean isEnabled() {
        return true;
    }
}
