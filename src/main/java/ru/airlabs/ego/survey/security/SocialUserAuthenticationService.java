package ru.airlabs.ego.survey.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.airlabs.ego.core.entity.user.UserSocial;
import ru.airlabs.ego.core.repository.user.UserSocialRepository;

/**
 * @author Aleksey Gorbachev
 */
@Service("socialUserAuthService")
public class SocialUserAuthenticationService implements UserDetailsService {

    @Autowired
    private UserSocialRepository userSocialRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        UserSocial userSocial = userSocialRepository.findBySocialUserIdAndActive(userId, true);
        if (userSocial == null) {
            return null;
        } else {
            return new SocialAuthentication(userSocial);
        }
    }
}
