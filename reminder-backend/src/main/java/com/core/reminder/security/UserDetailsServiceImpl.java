package com.core.reminder.security;

import com.common.reminder.model.AppUser;
import com.core.reminder.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList; // For granting authorities/roles if needed

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private AppUserRepository appUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with username: " + username)
                );

        // Spring Security's User requires username, password, and authorities.
        // We'll use username as the username for Spring Security context.
        // If you implement roles/permissions, you'd load them here and pass to the User constructor.
        return new User(appUser.getUsername(), appUser.getPassword(), new ArrayList<>());
        
        // Alternative: Create a custom UserDetails implementation (e.g., UserPrincipal)
        // that holds the AppUser object itself for easier access later.
        // return UserPrincipal.create(appUser);
    }
    
    // Optional: Helper method used by JWT filter to load user by ID
    // This might be useful if your JWT subject is the user ID instead of email
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
         AppUser appUser = appUserRepository.findById(id).orElseThrow(
             () -> new UsernameNotFoundException("User not found with id : " + id)
         );
         return new User(appUser.getUsername(), appUser.getPassword(), new ArrayList<>());
         // return UserPrincipal.create(appUser); // If using custom UserPrincipal
     }
} 