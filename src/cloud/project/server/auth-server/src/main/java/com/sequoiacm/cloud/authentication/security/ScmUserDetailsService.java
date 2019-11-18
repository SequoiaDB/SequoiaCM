package com.sequoiacm.cloud.authentication.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.sequoiacm.infrastructrue.security.core.ScmUser;
import com.sequoiacm.infrastructrue.security.core.ScmUserRoleRepository;

public class ScmUserDetailsService implements UserDetailsService {
    private final ScmUserRoleRepository repository;

    public ScmUserDetailsService(ScmUserRoleRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ScmUser user = repository.findUserByName(username);
        if (user == null) {
            throw new UsernameNotFoundException("User: " + username);
        }
        return user;
    }
}
