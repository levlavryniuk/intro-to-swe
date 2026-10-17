package edu.auk.admissions.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocalUserDetailsService implements UserDetailsService {

    private final LocalAccountRepository accounts;

    public LocalUserDetailsService(LocalAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        LocalAccount account = accounts.findByUserEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + email));
        return User.withUsername(account.getUser().getEmail())
                .password(account.getPasswordHash())
                .roles(Roles.of(account.getUser()))
                .build();
    }
}
