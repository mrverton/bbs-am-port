package com.afterimage.bbs.client;

/**
 * The colour of an afterimage: plain arithmetic, no game classes.
 *
 * <p>Modes: 0 original (no colour, returns -1), 1 one solid colour, 2 a gradient from the first
 * colour to the second over the afterimage's life, 3 the two colours taking turns from one
 * afterimage to the next, 4 a rainbow whose hue moves on with every new afterimage (and
 * optionally with the age of each one).</p>
 */
public final class AfterimageColors
{
    public static final int ORIGINAL = 0;
    public static final int SOLID = 1;
    public static final int GRADIENT = 2;
    public static final int ALTERNATE = 3;
    public static final int RAINBOW = 4;
    public static final int MODES = 5;

    private AfterimageColors()
    {}

    /**
     * @return 0xRRGGBB, or -1 for the character's own colours
     */
    public static int rgb(int mode, int first, int second, int index, int age, int life, float hueStep, float hueSpeed)
    {
        switch (mode)
        {
            case SOLID:
                return first & 0xFFFFFF;
            case GRADIENT:
            {
                float t = Math.max(0F, Math.min(1F, age / (float) Math.max(1, life)));

                return lerp(first, second, t);
            }
            case ALTERNATE:
                return (index & 1) == 0 ? first & 0xFFFFFF : second & 0xFFFFFF;
            case RAINBOW:
            {
                float hue = index * hueStep + age / 20F * hueSpeed;

                return hsv(hue - (float) Math.floor(hue), 0.85F, 1F);
            }
            default:
                return -1;
        }
    }

    public static int lerp(int a, int b, float t)
    {
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);

        return (r << 16) | (g << 8) | bl;
    }

    /** Hue, saturation and value, all 0..1. */
    public static int hsv(float h, float s, float v)
    {
        float scaled = (h - (float) Math.floor(h)) * 6F;
        int sector = (int) scaled;
        float f = scaled - sector;
        float p = v * (1F - s);
        float q = v * (1F - s * f);
        float t = v * (1F - s * (1F - f));
        float r;
        float g;
        float b;

        switch (sector)
        {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }

        return (Math.round(r * 255F) << 16) | (Math.round(g * 255F) << 8) | Math.round(b * 255F);
    }
}
