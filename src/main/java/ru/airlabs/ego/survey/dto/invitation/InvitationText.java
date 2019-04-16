package ru.airlabs.ego.survey.dto.invitation;

/**
 * Текст приглашения
 *
 * @author Roman Kochergin
 */
public class InvitationText {

    /**
     * Идентификатор
     */
    private final Long id;

    /**
     * Текст
     */
    private final String text;

    /**
     * Конструктор
     *
     * @param id идентификатор
     * @param text текст
     */
    public InvitationText(Long id, String text) {
        this.id = id;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }
}
