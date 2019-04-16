package ru.airlabs.ego.survey.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.airlabs.ego.core.entity.user.UserSocial;
import ru.airlabs.ego.core.repository.user.UserSocialRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Кастомизированный сервис для аутентификации пользователей через внешние сервисы (Telegram)
 *
 * @author Aleksey Gorbachev
 */
@Component("socialUserAuthenticationProvider")
public class SocialUserAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private SocialUserAuthenticationService authenticationService;

    @Autowired
    private UserSocialRepository userSocialRepository;

    @Transactional
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String name = authentication.getName();
        String password = authentication.getCredentials().toString();
        UserDetails authUserDetails = authenticationService.loadUserByUsername(name);
        if (authUserDetails == null) {  // логика для первичной авторизации пользователей с ролью MANAGER
            UserSocial userSocial = userSocialRepository.findByPassword(password);
            if (userSocial != null && userSocial.getActive()) {   // логика по замене временного идентификатора пользователя на Telegram ID
                String userSocialTempId = userSocial.getSocialUserId();
                if (userSocialTempId.startsWith("TEMP_USER_")) {    // меняем временный идентификатор
                    userSocial.setSocialUserId(name);
                    List<GrantedAuthority> auths = new ArrayList<>();
                    auths.add(new SimpleGrantedAuthority(userSocial.getRole().name()));
                    return new UsernamePasswordAuthenticationToken(new SocialAuthentication(userSocial), password, auths);
                }
            } else {
                throw new UsernameNotFoundException("User '" + name + "' not found.");
            }
        } else {    // стандартная проверка пароля
            SocialAuthentication socialAuthentication = (SocialAuthentication) authUserDetails;
            if (socialAuthentication.getPassword().equals(password)) {
                return new UsernamePasswordAuthenticationToken(socialAuthentication, socialAuthentication.getPassword(), socialAuthentication.getAuthorities());
            } else {
                throw new BadCredentialsException("Wrong password.");
            }
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
