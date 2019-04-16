package ru.airlabs.ego.survey.service.mail;

/**
 * Интерфейс сервиса для отправки писем
 *
 * @author Aleksey Gorbachev
 */
public interface MailSenderService {

    /**
     * Отправить письмо
     *
     * @param email    email адрес
     * @param name     имя пользователя
     * @param subject  тема письма
     * @param text     текст письма
     * @param fileName имя файла
     * @param filePath путь к файлу
     * @param senderId идентификатор пользователя-отправителя
     */
    void sendMail(String email,
                  String name,
                  String subject,
                  String text,
                  String fileName,
                  String filePath,
                  Long senderId);
}
