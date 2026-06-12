package com.meet.meetingRoomDemo.auth;

import com.meet.meetingRoomDemo.domain.user.AuthProvider;
import com.meet.meetingRoomDemo.domain.user.UserRepository;
import com.meet.meetingRoomDemo.domain.user.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(request);
        String registrationId = request.getClientRegistration().getRegistrationId();

        String email = extractEmail(oauth2User, registrationId);
        String name = extractName(oauth2User);

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not available from OAuth2 provider: " + registrationId);
        }

        UserVO user = userRepository.findUserByEmail(email.toLowerCase());
        if (user == null) {
            user = createOAuth2User(email, name, registrationId);
        } else {
            if (name != null) {
                user.setUserName(name);
                userRepository.save(user);
            }
        }

        return new CustomOAuth2User(oauth2User, user.getUserId(), user.getEmail(), user.getRole());
    }

    private UserVO createOAuth2User(String email, String name, String registrationId) {
        AuthProvider provider = "google".equalsIgnoreCase(registrationId)
            ? AuthProvider.GOOGLE : AuthProvider.AZURE_AD;
        UserVO user = UserVO.builder()
            .email(email.toLowerCase())
            .userName(name != null ? name : email)
            .role(0)
            .status(1)
            .authProvider(provider)
            .emailVerified(true)
            .build();
        return userRepository.save(user);
    }

    private String extractEmail(OAuth2User user, String registrationId) {
        if ("azure".equalsIgnoreCase(registrationId)) {
            String email = user.getAttribute("email");
            return email != null ? email : user.getAttribute("preferred_username");
        }
        return user.getAttribute("email");
    }

    private String extractName(OAuth2User user) {
        String name = user.getAttribute("name");
        return name != null ? name : user.getAttribute("given_name");
    }
}
