package sh.lpx.weather2.registry;

import net.minecraft.util.Identifier;
import sh.lpx.weather2.Weather;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class WeatherRegistry<T> {
    private final Map<Identifier, T> entries = new LinkedHashMap<>();

    public T add(Identifier id, T entry) {
        entries.put(id, entry);
        return entry;
    }

    public T add(String name, T entry) {
        Identifier id = Weather.id(name);
        return add(id, entry);
    }

    public void forEach(BiConsumer<Identifier, T> action) {
        entries.forEach(action);
    }
}
