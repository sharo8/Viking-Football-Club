package rw.vikingfc.club.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import rw.vikingfc.club.user.Role;
import rw.vikingfc.club.user.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String fullName;
    private final Role role;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final boolean mustChangePassword;

    public UserPrincipal(User user, boolean accountNonLocked) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.role = user.getRole();
        this.enabled = user.isActive() && user.getDeletedAt() == null;
        this.accountNonLocked = accountNonLocked;
        this.mustChangePassword = user.isMustChangePassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        if (role.isExecutive()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_EXECUTIVE"));
        }
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
