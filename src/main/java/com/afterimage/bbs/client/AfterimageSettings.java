package com.afterimage.bbs.client;

import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.utils.icons.Icons;

/**
 * The addon's settings, shown in BBS's settings panel as their own category. Every getter falls
 * back to the default when the settings were not registered (a failed registration must not
 * take the render loop down with it).
 *
 * <p>Times are in game ticks: 20 ticks = 1 second.</p>
 */
public final class AfterimageSettings
{
    private static ValueBoolean enabled;
    private static ValueFloat spacing;
    private static ValueInt hold;
    private static ValueInt fade;
    private static ValueFloat startAlpha;
    private static ValueFloat minAlpha;
    private static ValueInt maxGhosts;
    private static ValueInt trigger;
    private static ValueInt interval;
    private static ValueInt colorMode;
    private static ValueInt color1;
    private static ValueInt color2;
    private static ValueFloat strength;
    private static ValueFloat rainbowStep;
    private static ValueFloat rainbowLife;
    private static ValueInt driftDirection;
    private static ValueFloat driftSpeed;

    private AfterimageSettings()
    {}

    public static void register(SettingsBuilder builder)
    {
        builder.category("general", Icons.GEAR);

        enabled = builder.getBoolean("enabled", false);
        spacing = builder.getFloat("spacing", 0.35F, 0.05F, 4F);
        hold = builder.getInt("hold", 6, 0, 200);
        fade = builder.getInt("fade", 30, 1, 400);
        startAlpha = builder.getFloat("start_alpha", 0.5F, 0.05F, 1F);
        minAlpha = builder.getFloat("min_alpha", 0.05F, 0F, 0.5F);
        maxGhosts = builder.getInt("max_ghosts", 24, 1, 128);
        trigger = builder.getInt("trigger", 0, 0, 1).modes(
            L10n.lang("afterimage.mode.moving"), L10n.lang("afterimage.mode.always"));
        interval = builder.getInt("interval", 3, 1, 40);

        builder.category("color", Icons.COLOR);

        colorMode = builder.getInt("color_mode", 0, 0, 4).modes(
            L10n.lang("afterimage.color.original"), L10n.lang("afterimage.color.solid"),
            L10n.lang("afterimage.color.gradient"), L10n.lang("afterimage.color.alternate"),
            L10n.lang("afterimage.color.rainbow"));
        color1 = builder.getInt("color1", 0x4FC3FF).color();
        color2 = builder.getInt("color2", 0xFF4DA6).color();
        strength = builder.getFloat("strength", 0.85F, 0F, 1F);
        rainbowStep = builder.getFloat("rainbow_step", 0.06F, 0F, 1F);
        rainbowLife = builder.getFloat("rainbow_life", 0F, 0F, 5F);

        builder.category("motion", Icons.MOVE_RIGHT);

        driftDirection = builder.getInt("drift_direction", 0, 0, 6).modes(
            L10n.lang("afterimage.drift.none"), L10n.lang("afterimage.drift.right"),
            L10n.lang("afterimage.drift.left"), L10n.lang("afterimage.drift.up"),
            L10n.lang("afterimage.drift.down"), L10n.lang("afterimage.drift.forward"),
            L10n.lang("afterimage.drift.back"));
        driftSpeed = builder.getFloat("drift_speed", 1.5F, 0F, 20F);
    }

    /** 0: afterimages are left while the character moves, 1: all the time. */
    public static int trigger()
    {
        return trigger == null ? 0 : trigger.get();
    }

    /** Ticks between two afterimages when they are left all the time. */
    public static int interval()
    {
        return interval == null ? 3 : Math.max(1, interval.get());
    }

    public static int colorMode()
    {
        return colorMode == null ? 0 : colorMode.get();
    }

    public static int color1()
    {
        return color1 == null ? 0x4FC3FF : color1.get();
    }

    public static int color2()
    {
        return color2 == null ? 0xFF4DA6 : color2.get();
    }

    /** How much the colour replaces the character's own, 0..1. */
    public static float strength()
    {
        return strength == null ? 0.85F : strength.get();
    }

    /** Rainbow: how far the hue (a fraction of the circle) moves on with every new afterimage. */
    public static float rainbowStep()
    {
        return rainbowStep == null ? 0.06F : rainbowStep.get();
    }

    /** Rainbow: full hue turns per second over the life of each afterimage. */
    public static float rainbowLife()
    {
        return rainbowLife == null ? 0F : rainbowLife.get();
    }

    public static int driftDirection()
    {
        return driftDirection == null ? 0 : driftDirection.get();
    }

    /** Blocks per second. */
    public static float driftSpeed()
    {
        return driftSpeed == null ? 1.5F : driftSpeed.get();
    }

    /** Master switch. */
    public static boolean enabled()
    {
        return enabled != null && enabled.get();
    }

    /** How far a character has to move (in blocks) before a new afterimage is left. */
    public static double spacing()
    {
        return spacing == null ? 0.35D : spacing.get();
    }

    /** Ticks an afterimage stays at its starting opacity before it starts to fade. */
    public static int hold()
    {
        return hold == null ? 6 : hold.get();
    }

    /** Ticks the fade itself takes. */
    public static int fade()
    {
        return fade == null ? 30 : Math.max(1, fade.get());
    }

    /** Opacity of a fresh afterimage, 0..1. */
    public static float startAlpha()
    {
        return startAlpha == null ? 0.5F : startAlpha.get();
    }

    /** An afterimage fainter than this is deleted. */
    public static float minAlpha()
    {
        return minAlpha == null ? 0.05F : minAlpha.get();
    }

    /** Most afterimages alive at once per character. */
    public static int maxGhosts()
    {
        return maxGhosts == null ? 24 : maxGhosts.get();
    }

    /** Opacity at the given age (in ticks, fractions allowed). May go below zero. */
    public static float alphaAt(float age)
    {
        float start = startAlpha();
        float holdTicks = hold();

        if (age <= holdTicks)
        {
            return start;
        }

        return start * (1F - (age - holdTicks) / fade());
    }
}
