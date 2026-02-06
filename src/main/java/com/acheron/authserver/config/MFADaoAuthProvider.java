package com.acheron.authserver.config;

import com.acheron.authserver.entity.User;
import org.jboss.aerogear.security.otp.Totp;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class MFADaoAuthProvider extends DaoAuthenticationProvider {
    public MFADaoAuthProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        super(userDetailsService);
        setPasswordEncoder(passwordEncoder);
    }

    @Override
    protected Authentication createSuccessAuthentication(Object principal, Authentication authentication, UserDetails user) {
        return new UsernamePasswordAuthenticationToken(
                principal,
                authentication.getCredentials(),
                user.getAuthorities()
        );
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        super.additionalAuthenticationChecks(userDetails, authentication);
        CustomWebAuthenticationDetails details =
                (CustomWebAuthenticationDetails) authentication.getDetails();
        User user = (User) userDetails;

        if (user.isMfaEnabled()) {
            String code = details.getVerificationCode();
            if (code == null || code.isBlank()) {
                throw new BadCredentialsException("2FA code is missing");
            }

            Totp totp = new Totp(user.getMfaSecret());
            if (!isValidLong(code) || !totp.verify(code)) {
                throw new BadCredentialsException("Invalid verification code");
            }
        }
    }

    private boolean isValidLong(String code) {
        try {
            Long.parseLong(code);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}
