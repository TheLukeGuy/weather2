package weather2.player;

import weather2.util.WeatherUtilFile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class PlayerData {
    public static HashMap<String, NbtCompound> playerNBT = new HashMap<>();

    public static NbtCompound getPlayerNBT(String username) {
        if (!playerNBT.containsKey(username)) {
            tryLoadPlayerNBT(username);
        }
        return playerNBT.get(username);
    }

    public static void tryLoadPlayerNBT(String username) {
        //try read from hw/playerdata/player.dat
        //init with data, if fail, init default blank

        NbtCompound playerData = new NbtCompound();

        try {
            String fileURL = WeatherUtilFile.getWorldSaveFolderPath() + WeatherUtilFile.getWorldFolderName() + File.separator + "weather2" + File.separator + "PlayerData" + File.separator + username + ".dat";

            if ((new File(fileURL)).exists()) {
                playerData = NbtIo.readCompressed(Files.newInputStream(Paths.get(fileURL)));
            }
        } catch (Exception ex) {
            //Weather.dbg("no saved data found for " + username);
        }

        playerNBT.put(username, playerData);
    }
}
