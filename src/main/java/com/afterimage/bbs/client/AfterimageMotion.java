package com.afterimage.bbs.client;

/**
 * Where an afterimage has drifted to: plain arithmetic, no game classes.
 *
 * <p>Directions: 0 none, 1 right, 2 left, 3 up, 4 down, 5 forward, 6 back. Right, left, forward
 * and back are the character's own (they turn with the way it faced when the afterimage was
 * left); up and down are the world's.</p>
 */
public final class AfterimageMotion
{
    public static final int NONE = 0;
    public static final int RIGHT = 1;
    public static final int LEFT = 2;
    public static final int UP = 3;
    public static final int DOWN = 4;
    public static final int FORWARD = 5;
    public static final int BACK = 6;
    public static final int DIRECTIONS = 7;

    private AfterimageMotion()
    {}

    /**
     * Writes into {@code out} the shift in blocks (x, y, z) after {@code ageTicks}.
     *
     * @param yawDegrees the body yaw of the character when the afterimage was left
     */
    public static void offset(int direction, float blocksPerSecond, float ageTicks, float yawDegrees, float[] out)
    {
        float distance = blocksPerSecond / 20F * ageTicks;
        double yaw = Math.toRadians(yawDegrees);
        float forwardX = (float) -Math.sin(yaw);
        float forwardZ = (float) Math.cos(yaw);
        float rightX = (float) -Math.cos(yaw);
        float rightZ = (float) -Math.sin(yaw);

        float x = 0F;
        float y = 0F;
        float z = 0F;

        switch (direction)
        {
            case RIGHT: x = rightX; z = rightZ; break;
            case LEFT: x = -rightX; z = -rightZ; break;
            case UP: y = 1F; break;
            case DOWN: y = -1F; break;
            case FORWARD: x = forwardX; z = forwardZ; break;
            case BACK: x = -forwardX; z = -forwardZ; break;
            default: break;
        }

        out[0] = x * distance;
        out[1] = y * distance;
        out[2] = z * distance;
    }
}
