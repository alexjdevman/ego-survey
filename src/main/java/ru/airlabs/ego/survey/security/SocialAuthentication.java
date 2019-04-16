package ru.airlabs.ego.survey.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ru.airlabs.ego.core.entity.user.UserSocial;

import java.util.Collection;
import java.util.Collections;

/**
 * Модель для аутентификации через внешние сервисы
 *
 * @author Aleksey Gorbachev
 */
public class SocialAuthentication implements UserDetails {

    private UserSocial user;

    public SocialAuthentication(UserSocial user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (user.getRole() == UserSocial.UserRole.OWNER) {
            return Collections.singletonList(new SimpleGrantedAuthority("OWNER"));
        } else if (user.getRole() == UserSocial.UserRole.MANAGER) {
            return Collections.singletonList(new SimpleGrantedAuthority("MANAGER"));
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getSocialUserId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return user.getActive();
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return user.getActive();
    }

    @Override
    public boolean isEnabled() {
        return user.getActive();
    }

    public UserSocial getUser() {
        return user;
    }
}
