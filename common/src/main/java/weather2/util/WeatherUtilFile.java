package weather2.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class WeatherUtilFile {
    public static String lastWorldFolder = "";

    //this must be used while server is active
    public static String getWorldFolderName() {
        World world = DimensionManager.getWorld(0);

        if (world != null) {
            lastWorldFolder = ((ServerWorld) world).getChunkSaveLocation().getName();
            return lastWorldFolder + File.separator;
        }

        return lastWorldFolder + File.separator;
    }

    public static String getMinecraftSaveFolderPath() {
        if (FMLCommonHandler.instance().getMinecraftServerInstance() == null || FMLCommonHandler.instance().getMinecraftServerInstance().isSinglePlayer()) {
            return getClientSidePath() + File.separator + "config" + File.separator;
        } else {
            return new File(".").getAbsolutePath() + File.separator + "config" + File.separator;
        }
    }

    public static String getWorldSaveFolderPath() {
        if (FMLCommonHandler.instance().getMinecraftServerInstance() == null || FMLCommonHandler.instance().getMinecraftServerInstance().isSinglePlayer()) {
            return getClientSidePath() + File.separator + "saves" + File.separator;
        } else {
            return new File(".").getAbsolutePath() + File.separator;
        }
    }

    public static String getClientSidePath() {
        return FMLClientHandler.instance().getClient().mcDataDir/*getAppDir("minecraft")*/.getPath();
    }

    public static String getContentsFromResourceLocation(Identifier resourceLocation) {
        try {
            IResourceManager resourceManager = MinecraftClient.getInstance().entityRenderer.resourceManager;
            IResource iresource = resourceManager.getResource(resourceLocation);
            return IOUtils.toString(iresource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }
}
