package sh.lpx.weather2.util;

import de.androidpit.colorthief.ColorThief;
import sh.lpx.weather2.client.render.foliage.FoliageData;
import it.unimi.dsi.fastutil.ints.IntArrays;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;

import java.awt.image.BufferedImage;

public class WeatherUtilColor {
    public static int[] getColors(BlockState state) {
        BakedModel model;

        //used when foliage shader is on
        if (FoliageData.backupBakedModelStore.containsKey(state)) {
            model = FoliageData.backupBakedModelStore.get(state);
        } else {
            model = MinecraftClient.getInstance().getBlockRenderManager().getModels().getModel(state);
        }

        if (model != null && !model.isBuiltin()) {
            Sprite sprite = model.getSprite();
            if (sprite != null && sprite.getId() != MissingSprite.getMissingSpriteId()) {
                return getColors(model.getSprite());
            }
        }
        return IntArrays.EMPTY_ARRAY;
    }

    public static int[] getColors(Sprite sprite) {
        int width = sprite.getWidth();
        int height = sprite.getHeight();
        int frames = sprite.getFrameCount();

        BufferedImage img = new BufferedImage(width, height * frames, BufferedImage.TYPE_4BYTE_ABGR);
        for (int i = 0; i < frames; i++) {
            img.setRGB(0, i * height, width, height, sprite.getFrameTextureData(0)[0], 0, width);
        }

        int[][] colorData = ColorThief.getPalette(img, 6, 5, true);
        if (colorData != null) {
            int[] ret = new int[colorData.length];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = getColor(colorData[i]);
            }
            return ret;
        }
        return IntArrays.EMPTY_ARRAY;
    }

    private static int getColor(int[] colorData) {
        float mr = 1F;//((multiplier >>> 16) & 0xFF) / 255f;
        float mg = 1F;//((multiplier >>> 8) & 0xFF) / 255f;
        float mb = 1F;//(multiplier & 0xFF) / 255f;

        return 0xFF000000 | (((int) (colorData[0] * mr)) << 16) | (((int) (colorData[1] * mg)) << 8) | (int) (colorData[2] * mb);
    }
}
