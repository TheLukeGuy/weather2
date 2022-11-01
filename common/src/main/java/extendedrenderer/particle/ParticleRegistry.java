package extendedrenderer.particle;

import extendedrenderer.ExtendedRenderer;
import extendedrenderer.render.RotatingParticleManager;
import extendedrenderer.shader.MeshBufferManagerFoliage;
import extendedrenderer.shader.MeshBufferManagerParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;

import java.util.ArrayList;
import java.util.List;

public class ParticleRegistry {

    public static TextureAtlasSprite squareGrey;
    public static TextureAtlasSprite smoke;
    public static TextureAtlasSprite smokeTest;
    public static TextureAtlasSprite cloud;
    public static TextureAtlasSprite cloud256;
    public static TextureAtlasSprite cloud256_fire;
    public static TextureAtlasSprite cloud256_test;
    public static TextureAtlasSprite cloud256_2;
    public static TextureAtlasSprite cloud256_6;
    public static TextureAtlasSprite downfall2;
    public static TextureAtlasSprite downfall3;
    public static TextureAtlasSprite downfall4;
    public static TextureAtlasSprite cloud256_7;
    public static TextureAtlasSprite chicken;
    public static TextureAtlasSprite potato;
    public static TextureAtlasSprite leaf;
    public static TextureAtlasSprite rain;
    public static TextureAtlasSprite rain_white;
    public static TextureAtlasSprite rain_white_trans;
    public static TextureAtlasSprite rain_white_2;
    //public static TextureAtlasSprite rain_10;
    //public static TextureAtlasSprite rain_vanilla;
    //public static TextureAtlasSprite snow_vanilla;
    public static TextureAtlasSprite snow;
    //public static TextureAtlasSprite test;
    public static TextureAtlasSprite cloud256dark;
    public static TextureAtlasSprite cloudDownfall;
    public static TextureAtlasSprite tumbleweed;
    public static TextureAtlasSprite debris_1;
    public static TextureAtlasSprite debris_2;
    public static TextureAtlasSprite debris_3;
    public static TextureAtlasSprite test_texture;
    public static TextureAtlasSprite white_square;
    public static List<TextureAtlasSprite> listFish = new ArrayList<>();
    public static List<TextureAtlasSprite> listSeaweed = new ArrayList<>();
    public static TextureAtlasSprite grass;

    public static void init(TextureStitchEvent.Pre event) {

        //optifine breaks (removes) forge added method setTextureEntry, dont use it

        MeshBufferManagerParticle.cleanup();
        MeshBufferManagerFoliage.cleanup();

        squareGrey = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/white"));
        //smoke = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/smoke_00"));
        //smokeTest = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/smoke_2"));
        //cloud = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/cloud64"));
        cloud256 = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/cloud256"));
        cloud256_fire = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/cloud256_fire"));
        cloud256_test = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/cloud256_test"));
        //cloud256_2 = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/cloud256_5"));
        //ground splash
        cloud256_6 = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/cloud256_6"));
        //cloud256_7 = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/cloud256_7"));
        //downfall2 = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/downfall2"));
        downfall3 = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/downfall3"));
        //downfall4 = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/downfall4"));
        chicken = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/chicken"));
        potato = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/potato"));
        leaf = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/leaf"));
        //rain = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/rain"));
        test_texture = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/test_texture"));
        white_square = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/white_square"));
        rain_white = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/rain_white"));
        //rain_white_trans = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/rain_white_trans"));
        //rain_white_2 = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/rain_white_2"));
        //rain_10 = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/rain_10"));
        //rain_vanilla = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/vanilla/rain"));
        //snow_vanilla = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/vanilla/snow"));
        snow = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/snow"));
        //cloud256dark = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/cloud256dark"));
        //cloudDownfall = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/downfall"));
        tumbleweed = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/tumbleweed"));
        debris_1 = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/debris_1"));
        debris_2 = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/debris_2"));
        debris_3 = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/debris_3"));
		/*for (int i = 1; i <= 9; i++) {
			listFish.add(event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/fish_" + i)));
		}
		for (int i = 1; i <= 7; i++) {
			listSeaweed.add(event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/seaweed_section_" + i)));
		}*/
        //used indirectly not via reference
        grass = event.getMap().registerSprite(new ResourceLocation(ExtendedRenderer.modid + ":particles/grass"));
    }

    public static void initPost(TextureStitchEvent.Post event) {
        if (RotatingParticleManager.useShaders) {
            RotatingParticleManager.forceShaderReset = true;
        }
    }

    public static class TextureAtlasSpriteImpl extends TextureAtlasSprite {

        public TextureAtlasSpriteImpl(String spriteName) {
            super(spriteName);
        }

    }
}
