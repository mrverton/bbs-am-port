package com.afterimage.bbs.mixin;

import com.afterimage.bbs.client.PoseCapture;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The stand-in for 2.7's FormPoseEvents.MODEL_POSE: in 2.4 renderModel() applies IK, then
 * physics, then constraints, and applyPhysicsOnce() is called exactly once per render, so its
 * start is "after the animation and IK, before physics".
 */
@Mixin(ModelFormRenderer.class)
public abstract class ModelFormRendererMixin
{
    @Inject(method = "applyPhysicsOnce", at = @At("HEAD"))
    private void afterimage$pose(IEntity target, ModelInstance model, float transition, Matrix4f baseTransform, CallbackInfo ci)
    {
        PoseCapture.onModelPose(((ModelFormRenderer) (Object) this).getForm(), target, model);
    }
}
