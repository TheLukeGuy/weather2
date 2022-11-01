package weather2.weathersystem.storm;

public enum WeatherObjectType {
    CLOUD,
    SAND;

    private static final WeatherObjectType[] values = values();

    public static WeatherObjectType get(int ordinal) {
        return values[ordinal];
    }
}
