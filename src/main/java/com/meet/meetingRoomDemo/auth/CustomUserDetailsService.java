package com.meet.meetingRoomDemo.auth;

import com.meet.meetingRoomDemo.domain.user.UserRepository;
import com.meet.meetingRoomDemo.domain.user.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserVO user = userRepository.findUserByEmail(email.toLowerCase());
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + email);
        }
        return UserPrincipal.from(user);
    }
}
