package ru.airlabs.ego.survey.dto.invitation;

/**
 * Перечисление для типа приглашения
 *
 * @author Aleksey Gorbachev
 */
public enum InvitationType {

    AUDIO("A"),
    EMAIL("E"),
    SMS("S"),
    WHATS_APP("W"),
    VIBER("V"),
    SKYPE("C"),
    TELEGRAM("T"),
    UNIVERSAL("U");

    InvitationType(String typeId) {
        this.typeId = typeId;
    }

    private String typeId;

    public String getTypeId() {
        return typeId;
    }

    /**
     * Получение типа приглашения по его id
     *
     * @param typeId id типа приглашения
     * @return тип приглашения
     */
    public static InvitationType getInvitationTypeById(String typeId) {
        for (InvitationType invitationType : InvitationType.values()) {
            if (invitationType.typeId.equals(typeId)) {
                return invitationType;
            }
        }
        return null;
    }
}
