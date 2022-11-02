package weather2.weathersystem.storm;

public enum StormType {
    CLOUD,
    SAND;

    private static final StormType[] values = values();

    public static StormType get(int ordinal) {
        return values[ordinal];
    }
}
