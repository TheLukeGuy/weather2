package weather2;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.registry.Registry;
import weather2.registry.WeatherRegistry;
import weather2.registry.WeatherSounds;

public class FabricWeather extends Weather implements ModInitializer, ClientModInitializer {
    @Override
    public void onInitialize() {
        init();

        register(Registry.SOUND_EVENT, WeatherSounds.REGISTRY);
    }

    @Override
    public void onInitializeClient() {
        clientInit();
    }

    private <T> void register(Registry<T> vanillaRegistry, WeatherRegistry<T> weatherRegistry) {
        weatherRegistry.forEach((id, entry) -> Registry.register(vanillaRegistry, id, entry));
    }
}
