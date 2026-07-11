package eu.jdanek.unwaxsigns.config;

public enum NotificationType {
    ACTIONBAR,
    CHAT,
    NONE;

    public static NotificationType fromString(String value) {
        if (value == null) return CHAT;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CHAT;
        }
    }
}
