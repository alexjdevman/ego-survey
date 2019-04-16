package ru.airlabs.ego.survey.service.social;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.core.entity.user.UserSocial;
import ru.airlabs.ego.core.repository.UserRepository;
import ru.airlabs.ego.core.repository.user.UserSocialRepository;
import ru.airlabs.ego.survey.dto.user.UserRegistration;
import ru.airlabs.ego.survey.utils.MapUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис для работы с пользователями Telegram
 *
 * @author Aleksey Gorbachev
 */
@Service("telegramUserService")
public class TelegramUserServiceImpl implements TelegramUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSocialRepository userSocialRepository;

    /**
     * Добавить нового владельца аккаунта в ЛК из Telegram
     *
     * @param registration данные регистрации пользователя
     * @return новый пользователь
     */
    @Transactional
    @Override
    public User registerOwnerUser(UserRegistration registration) {
        registration.validateRegistration();
        User user = userRepository.findByEmail(registration.getEmail());
        if (user != null) { // найден пользователь с таким E-mail
            // проверяем введенный пароль при регистрации
            if (user.getPassword().equals(new Md5PasswordEncoder().encodePassword(registration.getPassword(), null))) {
                UserSocial telegramUser = userSocialRepository.findByPassword(registration.getPassword());
                if (telegramUser == null) { // создаем нового пользователя Telegram
                    telegramUser = createTelegramUser(user.getId(), registration);
                    userSocialRepository.save(telegramUser);
                } else {
                    throw new RuntimeException("Такой пользователь уже зарегистрирован");
                }
            } else {    // введен неверный пароль
                throw new RuntimeException("Такой пользователь уже зарегистрирован, неверный пароль");
            }
        } else {    // создаем нового пользователя и пользователя Telegram
            user = userRepository.save(createNewUser(registration));
            UserSocial telegramUser = createTelegramUser(user.getId(), registration);
            userSocialRepository.save(telegramUser);
        }
        return user;
    }

    /**
     * Получить список пользователей, доступных текущему пользователю-владельцу аккаунта Telegram
     *
     * @param ownerId идентификатор владельца аккаунта Telegram
     * @param active  признак активности
     * @return список данных по пользователям
     */
    @Transactional(readOnly = true)
    @Override
    public List<Map<String, Object>> getTelegramUsersForOwner(Long ownerId, Boolean active) {
        List<UserSocial> users = userSocialRepository.findAllByOwnerId(ownerId);
        return users.stream()
                .filter(u -> u.getActive().equals(active))
                .filter(u -> u.getRole().equals(UserSocial.UserRole.MANAGER))
                .map(u -> MapUtils.<String, Object>builder()
                        .add("socialUserId", u.getSocialUserId())
                        .add("ownerId", u.getOwnerId())
                        .add("name", u.getName())
                        .add("password", u.getPassword())
                        .add("role", u.getRole())
                        .add("active", u.getActive())
                        .add("dateCreate", new SimpleDateFormat("dd.MM.yyyy").format(u.getDateCreate()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Добавление нового пользователя из Telegram (с ролью MANAGER)
     *
     * @param ownerId  идентификатор владельца аккаунта в ЛК
     * @param name     имя пользователя в Telegram
     * @param password пароль пользователя для авторизации в Telegram
     * @return данные по новому пользователю (с ролью MANAGER)
     */
    @Transactional
    @Override
    public UserSocial addTelegramUser(Long ownerId, String name, String password) {
        UserSocial userSocial = new UserSocial();
        userSocial.setSocialType(UserSocial.SocialType.TG);
        userSocial.setName(name);
        userSocial.setOwnerId(ownerId);
        userSocial.setRole(UserSocial.UserRole.MANAGER);
        userSocial.setPassword(password);
        userSocial.setDateCreate(new Date());
        userSocial.setActive(Boolean.TRUE);

        return userSocialRepository.save(userSocial);
    }

    /**
     * Генерация пароля для пользователя Telegram
     *
     * @param length длинна пароля
     * @return пароль
     */
    @Override
    public String generateTelegramPassword(int length) {
        boolean useLetters = false;
        boolean useNumbers = true;
        String password = RandomStringUtils.random(length, useLetters, useNumbers);
        while (passwordExists(password)) {
            password = RandomStringUtils.random(length, useLetters, useNumbers);
        }
        return password;
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

    /**
     * Создание нового внешнего пользователя для Telegram
     *
     * @param ownerId      идентификатор владельца аккаунта ЛК
     * @param registration данные регистрации
     * @return внешний пользователь
     */
    private UserSocial createTelegramUser(Long ownerId, UserRegistration registration) {
        UserSocial userSocial = new UserSocial();
        userSocial.setSocialType(UserSocial.SocialType.TG);
        userSocial.setOwnerId(ownerId);
        userSocial.setSocialUserId(registration.getSocialUserId());
        userSocial.setPassword(registration.getPassword());
        userSocial.setDateCreate(new Date());
        userSocial.setActive(Boolean.TRUE);
        userSocial.setRole(UserSocial.UserRole.OWNER);
        userSocial.setName(registration.getName());
        return userSocial;
    }

    /**
     * Проверка, используется ли пароль в базе
     *
     * @param password пароль
     * @return true - если пароль уже используется
     */
    private boolean passwordExists(String password) {
        UserSocial userSocial = userSocialRepository.findByPassword(password);
        return userSocial != null;
    }

}
