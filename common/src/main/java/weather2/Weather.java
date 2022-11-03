package weather2;

import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Weather {
    public static final String MOD_ID = "weather2";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public void init() {
    }

    public void clientInit() {
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}
