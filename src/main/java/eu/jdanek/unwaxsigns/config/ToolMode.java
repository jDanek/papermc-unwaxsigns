package eu.jdanek.unwaxsigns.config;

public enum ToolMode {
    COMBI,
    LIST,
    TAG;


    public static ToolMode fromString(String value) {
        if (value == null) return TAG;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TAG;
        }
    }
}
