package com.metrohub.security;

import com.metrohub.models.User;
import com.metrohub.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("User not found with email: {}", email);
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
        User user = userOpt.get();

        if (!user.getIsActive()) {
            log.warn("User account is disabled: {}", email);
            throw new UsernameNotFoundException("User account is disabled: " + email);
        }

        return new CustomUserDetails(user);
    }

    

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new UsernameNotFoundException("User not found with ID: " + id);
        }
        User user = userOpt.get();

        return new CustomUserDetails(user);
    }

    

    public static class CustomUserDetails implements UserDetails {
        
        private final User user;
        private final Long cachedDepartmentId; // Cached to avoid LazyInitializationException

        public CustomUserDetails(User user) {
            this.user = user;
            // Cache department ID at construction time while Hibernate session is active
            this.cachedDepartmentId = user.getDepartmentId();
        }

        public User getUser() {
            return user;
        }

        public Long getId() {
            return user.getId();
        }

        public Long getDepartmentId() {
            return cachedDepartmentId;
        }

        public User.UserRole getRole() {
            return user.getRole();
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            // Create authority based on role
            String authority = "ROLE_" + user.getRole().name();
            return Collections.singletonList(new SimpleGrantedAuthority(authority));
        }

        @Override
        public String getPassword() {
            return user.getPassword();
        }

        @Override
        public String getUsername() {
            return user.getEmail();
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return user.getIsActive();
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return user.getIsActive();
        }

        

        public boolean canUpload() {
            return user.canUpload();
        }

        

        public boolean canAcknowledge() {
            return user.canAcknowledge();
        }

        

        public boolean canManageUsers() {
            return user.canManageUsers();
        }

        

        public boolean hasGlobalAccess() {
            return user.hasGlobalAccess();
        }
    }
}
