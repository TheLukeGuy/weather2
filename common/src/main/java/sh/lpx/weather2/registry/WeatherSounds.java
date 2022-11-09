package sh.lpx.weather2.registry;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import sh.lpx.weather2.Weather;

public class WeatherSounds {
    public static final WeatherRegistry<SoundEvent> REGISTRY = new WeatherRegistry<>();

    // TODO: Clean up sound names

    public static final SoundEvent WATERFALL = add("env.waterfall");

    public static final SoundEvent WIND_CALM = add("env.wind_calm");
    public static final SoundEvent WIND_CALM_FADE = add("env.wind_calmfade");
    public static final SoundEvent WIND_CLOSE = add("streaming.wind_close");
    public static final SoundEvent WIND_CLOSE_0 = add("streaming.wind_close_0_");
    public static final SoundEvent WIND_CLOSE_1 = add("streaming.wind_close_1_");
    public static final SoundEvent WIND_CLOSE_2 = add("streaming.wind_close_2_");
    public static final SoundEvent WIND_FAR = add("streaming.wind_far");
    public static final SoundEvent WIND_FAR_0 = add("streaming.wind_far_0_");
    public static final SoundEvent WIND_FAR_1 = add("streaming.wind_far_1_");
    public static final SoundEvent WIND_FAR_2 = add("streaming.wind_far_2_");

    public static final SoundEvent DESTRUCTION = add("streaming.destruction");
    public static final SoundEvent DESTRUCTION_0 = add("streaming.destruction_0_");
    public static final SoundEvent DESTRUCTION_1 = add("streaming.destruction_1_");
    public static final SoundEvent DESTRUCTION_2 = add("streaming.destruction_2_");
    public static final SoundEvent DESTRUCTION_S = add("streaming.destruction_s");
    public static final SoundEvent DESTRUCTION_B = add("streaming.destructionb");

    public static final SoundEvent SIREN = add("streaming.siren");
    public static final SoundEvent SIREN_SANDSTORM_1 = add("streaming.siren_sandstorm_1");
    public static final SoundEvent SIREN_SANDSTORM_2 = add("streaming.siren_sandstorm_2");
    public static final SoundEvent SIREN_SANDSTORM_3 = add("streaming.siren_sandstorm_3");
    public static final SoundEvent SIREN_SANDSTORM_4 = add("streaming.siren_sandstorm_4");
    public static final SoundEvent SIREN_SANDSTORM_5_EXTRA = add("streaming.siren_sandstorm_5_extra");
    public static final SoundEvent SIREN_SANDSTORM_6_EXTRA = add("streaming.siren_sandstorm_6_extra");

    public static final SoundEvent SANDSTORM_HIGH_1 = add("streaming.sandstorm_high1");
    public static final SoundEvent SANDSTORM_MED_1 = add("streaming.sandstorm_med1");
    public static final SoundEvent SANDSTORM_MED_2 = add("streaming.sandstorm_med2");
    public static final SoundEvent SANDSTORM_LOW_1 = add("streaming.sandstorm_low1");
    public static final SoundEvent SANDSTORM_LOW_2 = add("streaming.sandstorm_low2");

    private static SoundEvent add(String name) {
        Identifier id = Weather.id(name);
        SoundEvent sound = new SoundEvent(id);

        REGISTRY.add(id, sound);
        return sound;
    }
}
