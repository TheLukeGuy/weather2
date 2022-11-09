package sh.lpx.weather2.weather;

import CoroUtil.config.ConfigCoroUtil;
import sh.lpx.weather2.config.ConfigMisc;
import sh.lpx.weather2.config.ConfigParticle;
import sh.lpx.weather2.client.render.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.resources.IResourceManager;

public class EntityRendererProxyWeather2Mini extends EntityRenderer {

    public EntityRendererProxyWeather2Mini(Minecraft var1, IResourceManager resMan) {
        super(var1, resMan);
    }

    @Override
    protected void renderRainSnow(float par1) {

        boolean overrideOn = ConfigMisc.Misc_proxyRenderOverrideEnabled;

        /**
         * why render here? because renderRainSnow provides better context, solves issues:
         * - translucent blocks rendered after
         * -- shaders are color adjusted when rendering on other side of
         * --- water
         * --- stained glass, etc
         */
        if (ConfigCoroUtil.useEntityRenderHookForShaders) {
            EventHandler.hookRenderShaders(par1);
        }

        if (!overrideOn) {
            super.renderRainSnow(par1);
            return;
        } else {

            //note, the overcast effect change will effect vanilla non particle rain distance too, particle rain for life!
            if (!ConfigParticle.Particle_RainSnow) {
                super.renderRainSnow(par1);
            }

        }
    }
}
