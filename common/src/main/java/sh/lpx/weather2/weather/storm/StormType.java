package sh.lpx.weather2.weather.storm;

public enum StormType {
    CLOUD,
    SAND;

    private static final StormType[] values = values();

    public static StormType get(int ordinal) {
        return values[ordinal];
    }
}
