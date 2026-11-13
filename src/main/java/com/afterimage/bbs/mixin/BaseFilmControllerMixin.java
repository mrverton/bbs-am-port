package com.afterimage.bbs.mixin;

import com.afterimage.bbs.client.AfterimageManager;
import mchorse.bbs_mod.film.BaseFilmController;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * BBS FS 2.7 has film events; 2.4 does not, so the same three moments are hooked here:
 * after the entities of the film were updated, after the film was drawn, and on shutdown.
 * The world controller calls super for update and render, the editor controller calls super
 * for updateEntities, so hooking the base class covers all of them.
 */
@Mixin(BaseFilmController.class)
public abstract class BaseFilmControllerMixin
{
    @Inject(method = "updateEntities(I)V", at = @At("RETURN"))
    private void afterimage$tickAfter(int ticks, CallbackInfo ci)
    {
        AfterimageManager.onTick((BaseFilmController) (Object) this, ticks);
    }

    @Inject(method = "render(Lnet/fabricmc/fabric/api/client/rendering/v1/WorldRenderContext;)V", at = @At("RETURN"))
    private void afterimage$renderAfter(WorldRenderContext context, CallbackInfo ci)
    {
        AfterimageManager.onRender((BaseFilmController) (Object) this, context);
    }

    @Inject(method = "shutdown()V", at = @At("HEAD"))
    private void afterimage$shutdown(CallbackInfo ci)
    {
        AfterimageManager.onShutdown((BaseFilmController) (Object) this);
    }
}
