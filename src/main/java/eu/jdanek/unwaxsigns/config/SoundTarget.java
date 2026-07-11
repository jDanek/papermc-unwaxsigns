package eu.jdanek.unwaxsigns.config;

public enum SoundTarget {
    WORLD,
    PLAYER;

    public static SoundTarget fromString(String value) {
        if (value == null) return WORLD;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return WORLD;
        }
    }
}
