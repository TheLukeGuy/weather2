package weather2;

import net.minecraft.sound.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import weather2.registry.WeatherRegistry;
import weather2.registry.WeatherSounds;

@Mod(Weather.MOD_ID)
public class ForgeWeather extends Weather {
    public ForgeWeather() {
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
    }

    @SubscribeEvent
    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(this::init);
    }

    @SubscribeEvent
    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(this::clientInit);
    }

    @SubscribeEvent
    private void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        register(event.getRegistry(), WeatherSounds.REGISTRY);
    }

    private <T extends IForgeRegistryEntry<T>> void register(
            IForgeRegistry<T> forgeRegistry,
            WeatherRegistry<T> weatherRegistry
    ) {
        weatherRegistry.forEach((id, entry) -> {
            entry.setRegistryName(id);
            forgeRegistry.register(entry);
        });
    }
}
