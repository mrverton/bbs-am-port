package com.afterimage.bbs.client;

import com.afterimage.bbs.AfterimageClip;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.replays.Replay;

import java.util.List;

/**
 * What the afterimages of one replay look like at one moment of the film.
 *
 * <p>Comes from the afterimage clip that is playing on the film's clip timeline; where no clip
 * targets the replay, from the addon's own settings.</p>
 */
public final class AfterimageParams
{
    public boolean enabled = true;
    public boolean always;
    public int interval;
    public int colorMode;
    public int color1;
    public int color2;
    public float strength;
    public float rainbowStep;
    public float rainbowLife;
    public int driftDir;
    public float driftSpeed;
    public int hold;
    public int fade;
    public float startAlpha;
    public float spacing;

    /** Opacity at the given age (in ticks, fractions allowed). May go below zero. */
    public float alphaAt(float age)
    {
        if (age <= hold)
        {
            return startAlpha;
        }

        return startAlpha * (1F - (age - hold) / Math.max(1, fade));
    }

    /**
     * What the afterimages of this replay look like at this moment: from the afterimage clip that
     * is playing, and where the film has no such clip for this replay at all, from the settings.
     */
    public static AfterimageParams resolve(BaseFilmController controller, String replayId, Replay replay, int filmTick, int replayTick)
    {
        if (controller.film != null)
        {
            List<AfterimageClip> clips = controller.film.camera.getClips(AfterimageClip.class);
            AfterimageClip best = null;
            boolean any = false;

            for (AfterimageClip clip : clips)
            {
                if (!clip.targets(replayId))
                {
                    continue;
                }

                any = true;

                if (clip.enabled.get() && clip.isInside(filmTick) && (best == null || clip.layer.get() >= best.layer.get()))
                {
                    best = clip;
                }
            }

            if (any)
            {
                AfterimageParams p = defaults();

                if (best == null)
                {
                    p.enabled = false;

                    return p;
                }

                p.enabled = true;
                p.always = best.always.get();
                p.interval = Math.max(1, best.interval.get());
                p.spacing = best.spacing.get();
                p.hold = best.hold.get();
                p.fade = Math.max(1, best.fade.get());
                p.startAlpha = best.startAlpha.get();
                p.colorMode = best.colorMode.get();
                p.color1 = best.color1.get() & 0xFFFFFF;
                p.color2 = best.color2.get() & 0xFFFFFF;
                p.strength = clamp01(best.strength.get());
                p.rainbowStep = best.rainbowStep.get();
                p.rainbowLife = best.rainbowLife.get();
                p.driftDir = best.driftDir.get();
                p.driftSpeed = best.driftSpeed.get();

                return p;
            }
        }

        return defaults();
    }

    /** No clip for this replay: the addon's own settings (the settings menu) apply all the time. */
    private static AfterimageParams defaults()
    {
        AfterimageParams p = new AfterimageParams();

        p.enabled = AfterimageSettings.enabled();
        p.hold = AfterimageSettings.hold();
        p.fade = AfterimageSettings.fade();
        p.startAlpha = AfterimageSettings.startAlpha();
        p.spacing = (float) AfterimageSettings.spacing();
        p.always = AfterimageSettings.trigger() == 1;
        p.interval = Math.max(1, AfterimageSettings.interval());
        p.colorMode = AfterimageSettings.colorMode();
        p.color1 = AfterimageSettings.color1();
        p.color2 = AfterimageSettings.color2();
        p.strength = clamp01(AfterimageSettings.strength());
        p.rainbowStep = AfterimageSettings.rainbowStep();
        p.rainbowLife = AfterimageSettings.rainbowLife();
        p.driftDir = AfterimageSettings.driftDirection();
        p.driftSpeed = AfterimageSettings.driftSpeed();

        return p;
    }

    private static float clamp01(float v)
    {
        return Math.max(0F, Math.min(1F, v));
    }
}
