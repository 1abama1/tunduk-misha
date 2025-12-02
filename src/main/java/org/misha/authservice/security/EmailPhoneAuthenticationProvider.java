package org.misha.authservice.security;

import lombok.RequiredArgsConstructor;
import org.misha.authservice.entity.User;
import org.misha.authservice.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EmailPhoneAuthenticationProvider implements AuthenticationProvider {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String principal = String.valueOf(authentication.getPrincipal());
        String password = String.valueOf(authentication.getCredentials());

        User user = null;
        if (principal.contains("@")) {
            user = userRepository.findByEmail(principal).orElse(null);
        }
        if (user == null && principal.matches("[+0-9().\u0020-]{5,}")) {
            user = userRepository.findByPhone(principal).orElse(null);
        }
        if (user == null) {
            // try email/phone fallback (client may send in either field to controller then pass here)
            user = userRepository.findByEmail(principal).orElse(userRepository.findByPhone(principal).orElse(null));
        }
        if (user == null) {
            throw new BadCredentialsException("User not found");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        // set principal as userId string for downstream controllers
        return new UsernamePasswordAuthenticationToken(String.valueOf(user.getId()), null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}


