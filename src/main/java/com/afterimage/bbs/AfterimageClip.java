package com.afterimage.bbs;

import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

/**
 * A clip of the film's timeline that switches afterimages on for the length of the clip, with all
 * their settings. It changes nothing about the camera.
 *
 * <p>It works on the replay named in {@link #target}, or on every replay while that is empty. Where
 * a replay has such clips, afterimages are left only while one of them is playing (the one on the
 * highest layer decides the look).</p>
 */
public class AfterimageClip extends CameraClip
{
    /** The id of the replay; empty means all replays. */
    public final ValueString target = new ValueString("target", "");

    /** False: only while the character moves. True: all the time. */
    public final ValueBoolean always = new ValueBoolean("always", false);
    public final ValueInt interval = new ValueInt("interval", 3, 1, 40);
    public final ValueFloat spacing = new ValueFloat("spacing", 0.35F, 0.05F, 4F);

    public final ValueInt hold = new ValueInt("hold", 6, 0, 200);
    public final ValueInt fade = new ValueInt("fade", 30, 1, 400);
    public final ValueFloat startAlpha = new ValueFloat("startAlpha", 0.5F, 0.05F, 1F);

    public final ValueInt colorMode = new ValueInt("colorMode", 0, 0, 4);
    public final ValueInt color1 = new ValueInt("color1", 0x4FC3FF, 0, 0xFFFFFF);
    public final ValueInt color2 = new ValueInt("color2", 0xFF4DA6, 0, 0xFFFFFF);
    public final ValueFloat strength = new ValueFloat("strength", 0.85F, 0F, 1F);
    public final ValueFloat rainbowStep = new ValueFloat("rainbowStep", 0.06F, 0F, 1F);
    public final ValueFloat rainbowLife = new ValueFloat("rainbowLife", 0F, 0F, 5F);

    public final ValueInt driftDir = new ValueInt("driftDir", 0, 0, 6);
    public final ValueFloat driftSpeed = new ValueFloat("driftSpeed", 1.5F, 0F, 20F);

    public AfterimageClip()
    {
        super();

        this.add(this.target);
        this.add(this.always);
        this.add(this.interval);
        this.add(this.spacing);
        this.add(this.hold);
        this.add(this.fade);
        this.add(this.startAlpha);
        this.add(this.colorMode);
        this.add(this.color1);
        this.add(this.color2);
        this.add(this.strength);
        this.add(this.rainbowStep);
        this.add(this.rainbowLife);
        this.add(this.driftDir);
        this.add(this.driftSpeed);
    }

    /** Does not touch the camera: the clip only marks a stretch of the film. */
    @Override
    protected void applyClip(ClipContext context, Position position)
    {}

    @Override
    protected Clip create()
    {
        return new AfterimageClip();
    }

    /** Whether this clip is for the replay with this id. */
    public boolean targets(String replayId)
    {
        String id = this.target.get();

        return id == null || id.isEmpty() || id.equals(replayId);
    }
}
