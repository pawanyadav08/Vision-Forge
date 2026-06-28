package com.visionforge.security;

import com.visionforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserDetailsServiceImpl — Bridge between Spring Security and our database.
 *
 * Spring Security calls loadUserByUsername() during authentication to fetch
 * the user. Our User entity already implements UserDetails, so we return
 * it directly — no extra mapping required.
 *
 * @Transactional(readOnly = true): Optimises the DB read — no write
 * transaction overhead for a simple lookup.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with email: " + email)
                );
    }
}
