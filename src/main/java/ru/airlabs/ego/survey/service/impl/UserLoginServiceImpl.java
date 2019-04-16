package ru.airlabs.ego.survey.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.core.repository.UserRepository;
import ru.airlabs.ego.survey.dto.user.UserPasswordChange;
import ru.airlabs.ego.survey.dto.user.UserRegistration;
import ru.airlabs.ego.survey.exception.EmailExistsException;
import ru.airlabs.ego.survey.exception.UserNotFoundException;
import ru.airlabs.ego.survey.service.UserLoginService;

import java.util.Date;

@Service("userLoginService")
@Transactional
public class UserLoginServiceImpl implements UserLoginService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User registerUser(UserRegistration registration) {
        registration.validateRegistration();
        if (emailExist(registration.getEmail())) {
            throw new EmailExistsException("Пользователь уже зарегистрирован с таким email " + registration.getEmail());
        }
        User user = createNewUser(registration);
        return userRepository.save(user);
    }

    @Override
    public void confirmUserRegistration(String email, String password) {
        User user = userRepository.findByEmailAndPassword(email, password);
        if (user == null) {
            throw new UserNotFoundException("Не найден пользователь с email " + email);
        }
        user.setVerified(Boolean.TRUE);
    }

    @Override
    public void changePassword(UserPasswordChange passwordChange) {
        passwordChange.validatePasswordChange();
        User user = userRepository.findByEmailAndPassword(passwordChange.getEmail(), passwordChange.getKey());
        if (user == null) {
            throw new UserNotFoundException("Не найден пользователь с email " + passwordChange.getEmail());
        }
        user.setPassword(new Md5PasswordEncoder().encodePassword(passwordChange.getPassword(), null));
    }

    private User createNewUser(UserRegistration registration) {
        User user = new User();
        user.setName(registration.getName());
        user.setEmail(registration.getEmail());
        user.setPassword(new Md5PasswordEncoder().encodePassword(registration.getPassword(), null));
        user.setDateCreate(new Date());
        user.setLocale(LocaleContextHolder.getLocale().getLanguage().toUpperCase());
        return user;
    }

    private boolean emailExist(String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            return true;
        }
        return false;
    }
}
