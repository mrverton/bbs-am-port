package com.afterimage.bbs.client;

import com.afterimage.bbs.AfterimageAddon;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.utils.Anchor;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Afterimages for characters of a BBS film.
 *
 * <p>Every film tick, each replay's entity that has moved far enough since its last afterimage
 * leaves a snapshot behind: a copy of its form (so the pose of that moment is frozen in it) and
 * a stand-in entity holding the place and the facing. Every frame the snapshots are drawn through
 * BBS's own form renderer with a colour whose alpha follows their age. Once an afterimage is
 * fainter than the minimum opacity, it is deleted.</p>
 *
 * <p>All of this is on the client thread: tick, render and shutdown events of BBS all run there.</p>
 */
public final class AfterimageManager
{
    /** A jump longer than this in one tick is a cut or a teleport, not movement: no afterimage. */
    private static final double TELEPORT_DISTANCE = 16D;

    /** Weak, so a controller that never got its shutdown cannot be kept alive by its trails. */
    private static final Map<BaseFilmController, Trails> TRAILS = new WeakHashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(AfterimageAddon.MOD_ID);

    /** After this many failures the addon switches itself off for the rest of the session. */
    private static final int MAX_FAILURES = 3;

    /** Scratch for the drift of the afterimage being drawn: drawing is on the client thread only. */
    private static final float[] SHIFT = new float[3];

    private static int failures;
    private static boolean disabled;

    private AfterimageManager()
    {}

    /**
     * A failure of the addon must never take the game with it. A mismatch between this addon and
     * the BBS build it runs on shows up as a {@link LinkageError} (a method that is not there) or
     * a runtime exception: both are caught, logged, and after a few of them the addon turns itself
     * off and lets go of what it built.
     */
    private static void fail(Throwable error)
    {
        failures += 1;

        LOGGER.error("[Afterimage] failure {} of {}", failures, MAX_FAILURES, error);

        if (failures >= MAX_FAILURES)
        {
            disabled = true;
            TRAILS.clear();
            PoseCapture.clear();

            LOGGER.error("[Afterimage] too many failures, afterimages are switched off until the game restarts. Is this build of the addon made for this build of BBS?");
        }
    }

    /* ---------------------------------------------------------------- state */

    private static final class Ghost
    {
        final Form form;
        final StubEntity entity;
        final double x;
        final double y;
        final double z;
        final float bodyYaw;
        final int light;
        final int index;
        final boolean hasModel;
        final AfterimageParams params;
        int tint = 0xFFFFFF;
        int age;

        Ghost(Form form, StubEntity entity, int light, int index, boolean hasModel, AfterimageParams params)
        {
            this.params = params;
            this.index = index;
            this.hasModel = hasModel;
            this.form = form;
            this.entity = entity;
            this.x = entity.getX();
            this.y = entity.getY();
            this.z = entity.getZ();
            this.bodyYaw = entity.getBodyYaw();
            this.light = light;
        }
    }

    /** The afterimages of one replay. */
    private static final class Trail
    {
        IEntity source;
        boolean hasLast;
        int sinceSpawn;
        int count;
        double lastX;
        double lastY;
        double lastZ;
        final List<Ghost> ghosts = new ArrayList<>();

        void remember(double x, double y, double z)
        {
            this.hasLast = true;
            this.lastX = x;
            this.lastY = y;
            this.lastZ = z;
        }

        void reset()
        {
            this.ghosts.clear();
            this.hasLast = false;
        }
    }

    /** The afterimages of one film controller. */
    private static final class Trails
    {
        boolean hasTick;
        int lastTick;
        final Map<String, Trail> byReplay = new HashMap<>();

        void reset()
        {
            for (Trail trail : this.byReplay.values())
            {
                trail.reset();
            }
        }
    }

    /* ---------------------------------------------------------------- events */

    /** {@code FilmEvents.TICK_AFTER}: ages the afterimages and leaves new ones. */
    public static void onTick(BaseFilmController controller, int ticks)
    {
        if (disabled)
        {
            return;
        }

        try
        {
            tick(controller, ticks);
        }
        catch (RuntimeException | LinkageError e)
        {
            TRAILS.remove(controller);
            fail(e);
        }
    }

    private static void tick(BaseFilmController controller, int ticks)
    {
        /* Off by default: with the global switch off, only films that have an afterimage clip do anything. */
        if (!AfterimageSettings.enabled() && (controller.film == null || controller.film.camera.getClips(com.afterimage.bbs.AfterimageClip.class).isEmpty()))
        {
            TRAILS.remove(controller);

            return;
        }

        Trails trails = TRAILS.computeIfAbsent(controller, (key) -> new Trails());

        /* The film's tick did not move: it is paused (or being edited). Time stands still for the
         * afterimages too, they neither age nor multiply. */
        if (trails.hasTick && ticks == trails.lastTick)
        {
            return;
        }

        boolean sequential = trails.hasTick && ticks == trails.lastTick + 1;

        trails.hasTick = true;
        trails.lastTick = ticks;

        if (!sequential)
        {
            /* First tick, a seek, a rewind or a loop: afterimages of another moment make no sense. */
            trails.reset();
        }
        else
        {
            age(trails);
        }

        spawn(controller, trails, ticks);
    }

    /** {@code FilmEvents.RENDER_AFTER}: draws the afterimages into the world. */
    public static void onRender(BaseFilmController controller, WorldRenderContext context)
    {
        if (disabled)
        {
            return;
        }

        try
        {
            render(controller, context);
        }
        catch (RuntimeException | LinkageError e)
        {
            fail(e);
        }
    }

    private static void render(BaseFilmController controller, WorldRenderContext context)
    {
        Trails trails = TRAILS.get(controller);

        if (trails == null)
        {
            return;
        }

        /* The same rule BBS draws its own replays with: a paused film shows its tick as it is. */
        float partial = controller.paused ? 0F : context.tickDelta();
        float minAlpha = AfterimageSettings.minAlpha();
        MatrixStack stack = context.matrixStack();
        Camera camera = context.camera();
        Vec3d cameraPos = camera.getPos();
        boolean drew = false;

        for (Trail trail : trails.byReplay.values())
        {
            for (Ghost ghost : trail.ghosts)
            {
                float alpha = ghost.params.alphaAt(ghost.age + partial);

                if (alpha < minAlpha || alpha <= 0F)
                {
                    continue;
                }

                drew = true;
                renderGhost(ghost, alpha, ghost.age + partial, stack, camera, cameraPos);
            }
        }

        if (drew)
        {
            /* BBS leaves it on after drawing a form, and what runs after us expects that. */
            RenderSystem.enableDepthTest();
        }
    }

    /** {@code FilmEvents.SHUTDOWN}: lets go of everything built for this controller. */
    public static void onShutdown(BaseFilmController controller)
    {
        TRAILS.remove(controller);
    }

    /* ---------------------------------------------------------------- logic */

    private static void age(Trails trails)
    {
        float minAlpha = AfterimageSettings.minAlpha();

        for (Trail trail : trails.byReplay.values())
        {
            Iterator<Ghost> it = trail.ghosts.iterator();

            while (it.hasNext())
            {
                Ghost ghost = it.next();

                ghost.age += 1;
                colorize(ghost, true);

                /* Nearly invisible: not worth a form render any more. */
                if (ghost.params.alphaAt(ghost.age) < Math.max(minAlpha, 0.0001F))
                {
                    PoseCapture.forget(ghost.entity);
                    it.remove();
                }
            }
        }
    }

    private static void spawn(BaseFilmController controller, Trails trails, int ticks)
    {
        /* BBS FS 2.4 keeps the entities of a film in a map by the INDEX of the replay (2.7 uses
         * ids). The trail is stored by that index; the clip's target is still compared with the
         * replay's id, exactly as before. */
        List<Replay> replayList = controller.film == null ? new ArrayList<Replay>() : controller.film.replays.getList();

        int max = AfterimageSettings.maxGhosts();

        for (int index = 0; index < replayList.size(); index++)
        {
            Replay replay = replayList.get(index);
            IEntity entity = controller.entities.get(index);

            if (entity == null)
            {
                continue;
            }

            Trail trail = trails.byReplay.computeIfAbsent(String.valueOf(index), (key) -> new Trail());

            /* The film rebuilt its entities (a restart, a seek): the old afterimages belong to
             * entities that are gone. */
            if (trail.source != entity)
            {
                trail.reset();
                trail.source = entity;
            }

            Form form = entity.getForm();

            if (form == null)
            {
                continue;
            }

            AfterimageParams params = AfterimageParams.resolve(controller, replay.getId(), replay, ticks, replay.getTick(ticks));
            double spacing = params.spacing;

            double x = entity.getX();
            double y = entity.getY();
            double z = entity.getZ();

            if (!trail.hasLast)
            {
                trail.remember(x, y, z);

                continue;
            }

            double dx = x - trail.lastX;
            double dy = y - trail.lastY;
            double dz = z - trail.lastZ;
            double distance = dx * dx + dy * dy + dz * dz;
            boolean always = params.always;

            /* Hidden, switched off, or standing in for another replay's bone (its real place is
             * the anchor's, which this does not follow): leave nothing, but keep the position
             * current so that switching on does not leave one from far away. */
            if (!canLeave(form, params) || (!always && distance > TELEPORT_DISTANCE * TELEPORT_DISTANCE))
            {
                trail.remember(x, y, z);
                trail.sinceSpawn = 0;

                continue;
            }

            if (always)
            {
                trail.sinceSpawn += 1;

                if (trail.sinceSpawn < params.interval)
                {
                    continue;
                }
            }
            else if (distance < spacing * spacing)
            {
                continue;
            }

            Ghost ghost = snapshot(entity, form, trail.count, params);

            trail.sinceSpawn = 0;
            trail.remember(x, y, z);

            if (ghost != null)
            {
                trail.count += 1;
                trail.ghosts.add(ghost);

                while (trail.ghosts.size() > max)
                {
                    trail.ghosts.remove(0);
                }
            }
        }
    }

    private static boolean canLeave(Form form, AfterimageParams params)
    {
        return params.enabled && form.visible.get() && form.anchor.get().replay == Anchor.NO_ATTACHMENT;
    }

    private static Ghost snapshot(IEntity entity, Form form, int index, AfterimageParams params)
    {
        Form copy = FormUtils.copy(form);

        if (copy == null)
        {
            return null;
        }

        World world = entity.getWorld();
        StubEntity stub = new StubEntity(world);

        /* The state of the moment, then the frozen form, then no motion: the previous values equal
         * the current ones, so drawing at any partial tick lands in the same place. */
        stub.copy(entity);
        stub.setForm(copy);
        stub.setPrevX(stub.getX());
        stub.setPrevY(stub.getY());
        stub.setPrevZ(stub.getZ());
        stub.setPrevYaw(stub.getYaw());
        stub.setPrevHeadYaw(stub.getHeadYaw());
        stub.setPrevPitch(stub.getPitch());
        stub.setPrevBodyYaw(stub.getBodyYaw());
        stub.setPrevPrevBodyYaw(stub.getBodyYaw());
        stub.setHurtTimer(0);

        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        if (world != null)
        {
            BlockPos pos = BlockPos.ofFloored(stub.getX(), stub.getY() + 0.5D, stub.getZ());

            light = LightmapTextureManager.pack(world.getLightLevel(LightType.BLOCK, pos), world.getLightLevel(LightType.SKY, pos));
        }

        PoseCapture.freeze(entity, stub);

        Ghost ghost = new Ghost(copy, stub, light, index, containsModel(copy), params);

        colorize(ghost, false);

        return ghost;
    }

    private static boolean containsModel(Form form)
    {
        if (form instanceof ModelForm)
        {
            return true;
        }

        for (BodyPart part : form.parts.getAllTyped())
        {
            Form child = part.getForm();

            if (child != null && containsModel(child))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Gives the afterimage its colour for its present age. Model forms take it as BBS's own colour
     * overlay (a flat colour mixed over the texture, by the chosen strength), anything else is
     * tinted instead. {@code onlyIfChanging} skips the colour modes that stay the same for the
     * whole life of the afterimage.
     */
    private static void colorize(Ghost ghost, boolean onlyIfChanging)
    {
        AfterimageParams params = ghost.params;
        int mode = params.colorMode;

        if (onlyIfChanging && mode != AfterimageColors.GRADIENT && !(mode == AfterimageColors.RAINBOW && params.rainbowLife > 0F))
        {
            return;
        }

        int life = params.hold + params.fade;
        int rgb = AfterimageColors.rgb(mode, params.color1, params.color2, ghost.index,
            ghost.age, life, params.rainbowStep, params.rainbowLife);

        if (rgb < 0)
        {
            ghost.tint = 0xFFFFFF;

            return;
        }

        float strength = params.strength;

        /* BBS FS 2.7 paints model forms with a flat overlay colour (Form.overlayColor). 2.4 has no
         * such thing, so every form is tinted: the texture is multiplied by the colour. */
        ghost.tint = AfterimageColors.lerp(0xFFFFFF, rgb, strength);
    }

    private static void renderGhost(Ghost ghost, float alpha, float age, MatrixStack stack, Camera camera, Vec3d cameraPos)
    {
        int argb = (Math.round(Math.min(1F, alpha) * 255F) << 24) | (ghost.tint & 0xFFFFFF);

        /* A context of its own for every afterimage: a form set to render last keeps the one it
         * was given until the end of the pass. */
        FormRenderingContext formContext = new FormRenderingContext()
            .set(FormRenderType.ENTITY, ghost.entity, stack, ghost.light, OverlayTexture.DEFAULT_UV, 0F)
            .camera(camera)
            .color(argb);

        stack.push();

        try
        {
            /* Where BBS puts a replay: the stack is camera relative, then the body's yaw. */
            float[] shift = SHIFT;

            AfterimageMotion.offset(ghost.params.driftDir, ghost.params.driftSpeed, age, ghost.bodyYaw, shift);
            stack.translate(ghost.x - cameraPos.x + shift[0], ghost.y - cameraPos.y + shift[1], ghost.z - cameraPos.z + shift[2]);
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-ghost.bodyYaw));

            FormUtilsClient.render(ghost.form, formContext);
        }
        finally
        {
            stack.pop();
        }
    }
}
