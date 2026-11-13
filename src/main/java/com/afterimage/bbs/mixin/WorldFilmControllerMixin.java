package com.afterimage.bbs.mixin;

import com.afterimage.bbs.client.AfterimageManager;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.WorldFilmController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** WorldFilmController overrides shutdown() WITHOUT calling super, so it needs its own hook. */
@Mixin(WorldFilmController.class)
public abstract class WorldFilmControllerMixin
{
    @Inject(method = "shutdown()V", at = @At("HEAD"))
    private void afterimage$shutdown(CallbackInfo ci)
    {
        AfterimageManager.onShutdown((BaseFilmController) (Object) this);
    }
}
