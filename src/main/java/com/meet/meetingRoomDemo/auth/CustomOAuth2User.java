package com.meet.meetingRoomDemo.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User delegate;
    private final UUID userId;
    private final String email;
    private final Integer role;

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role == 1 ? "ROLE_ADMIN" : "ROLE_USER"));
    }

    @Override
    public String getName() {
        return userId.toString();
    }
}
