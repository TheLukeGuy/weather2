package sh.lpx.weather2.client.render;

import sh.lpx.weather2.client.render.render.FoliageRenderer;
import sh.lpx.weather2.client.render.render.RotatingParticleManager;

public class ExtendedRenderer {
    public static RotatingParticleManager rotEffRenderer;

    public static FoliageRenderer foliageRenderer;

    // TODO: Run post-init:
    // ExtendedRenderer.rotEffRenderer = new RotatingParticleManager(mc.world, mc.renderEngine);
    // ExtendedRenderer.foliageRenderer = new FoliageRenderer(mc.renderEngine);
    // EventHandler.foliageUseLast = ConfigCoroUtil.foliageShaders;
}
