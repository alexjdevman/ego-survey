package ru.airlabs.ego.survey.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.core.repository.UserRepository;
import ru.airlabs.ego.survey.exception.UserNotVerifiedException;

/**
 * @author Roman Kochergin
 */
@Service("userDetailsService")
public class RestUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
        User user = userRepository.findByEmailAndActive(username.toLowerCase(), true);
        if (user == null) {
            throw new UsernameNotFoundException("User '" + username + "' not found.");
        } else if (!user.getVerified()) {
            throw new UserNotVerifiedException("User '" + username + "' has not confirmed registration");
        }
        return new Authentication(user);
    }
}

