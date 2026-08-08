package com.sedmotors.security;

import com.sedmotors.model.AuthProvider;
import com.sedmotors.model.Role;
import com.sedmotors.model.User;
import com.sedmotors.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            // Update name if changed
            if (name != null && !name.equals(user.getName())) {
                user.setName(name);
                userRepository.save(user);
            }
        } else {
            // Register new OAuth2 user automatically
            user = new User();
            user.setEmail(email);
            user.setName(name != null ? name : "Google User");
            user.setRole(Role.ROLE_USER); // default role
            user.setAuthProvider(AuthProvider.GOOGLE);
            user = userRepository.save(user);
        }

        return new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())),
                oAuth2User.getAttributes(),
                "email" // the key identifying the username in the attributes map
        );
    }
}
