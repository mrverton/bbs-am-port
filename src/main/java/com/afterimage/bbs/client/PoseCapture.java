package com.afterimage.bbs.client;

import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Remembers what the limbs of a moving character look like (running, swinging), so that an
 * afterimage can show the same pose instead of a fresh standing one.
 *
 * <p>Live characters are recorded every frame through BBS's own model pose event (after the
 * animation, before physics). An afterimage's stand-in entity gets a frozen copy of that record;
 * when BBS draws the afterimage, the event puts the copy back onto the bones.</p>
 */
public final class PoseCapture
{
    /** Latest pose of every live entity: form path -> bone name -> state. */
    private static final Map<IEntity, Map<String, Map<String, BoneState>>> LIVE = new WeakHashMap<>();

    /** Frozen poses of the afterimage stand-ins. */
    private static final Map<IEntity, Map<String, Map<String, BoneState>>> FROZEN = new WeakHashMap<>();

    private PoseCapture()
    {}

    private static final class BoneState
    {
        final Transform transform = new Transform();
        Quaternionf orient;
        Vector3f offset;
    }

    public static void clear()
    {
        LIVE.clear();
        FROZEN.clear();
    }

    /** A deep copy of the live record of {@code source}, for the stand-in {@code target}. */
    public static void freeze(IEntity source, IEntity target)
    {
        Map<String, Map<String, BoneState>> live = LIVE.get(source);

        if (live == null)
        {
            return;
        }

        Map<String, Map<String, BoneState>> copy = new HashMap<>();

        for (Map.Entry<String, Map<String, BoneState>> form : live.entrySet())
        {
            Map<String, BoneState> bones = new HashMap<>();

            for (Map.Entry<String, BoneState> bone : form.getValue().entrySet())
            {
                BoneState from = bone.getValue();
                BoneState to = new BoneState();

                to.transform.copy(from.transform);
                to.orient = from.orient == null ? null : new Quaternionf(from.orient);
                to.offset = from.offset == null ? null : new Vector3f(from.offset);
                bones.put(bone.getKey(), to);
            }

            copy.put(form.getKey(), bones);
        }

        FROZEN.put(target, copy);
    }

    public static void forget(IEntity target)
    {
        FROZEN.remove(target);
    }

    /** Called by ModelFormRendererMixin: after the animation and IK, before physics. */
    public static void onModelPose(ModelForm form, IEntity entity, ModelInstance model)
    {
        if (form == null || entity == null || model == null || model.model == null)
        {
            return;
        }

        try
        {
            Map<String, Map<String, BoneState>> frozen = FROZEN.get(entity);

            if (frozen != null)
            {
                apply(frozen.get(FormUtils.getPath(form)), model.model);
            }
            else if (AfterimageSettings.enabled())
            {
                record(entity, FormUtils.getPath(form), model.model);
            }
        }
        catch (RuntimeException | LinkageError e)
        {
            /* Never break a frame over a pose: the afterimage just keeps the stand-in pose. */
        }
    }

    private static void record(IEntity entity, String path, IModel model)
    {
        Map<String, BoneState> bones = LIVE.computeIfAbsent(entity, (k) -> new HashMap<>())
            .computeIfAbsent(path, (k) -> new HashMap<>());

        for (ModelGroup group : model.getAllGroups())
        {
            store(bones, "g:" + group.id, group.current, group.orient, group.offset);
        }

        for (BOBJBone bone : model.getAllBOBJBones())
        {
            store(bones, "b:" + bone.name, bone.transform, bone.orient, bone.offset);
        }
    }

    private static void store(Map<String, BoneState> bones, String name, Transform transform, Quaternionf orient, Vector3f offset)
    {
        BoneState state = bones.computeIfAbsent(name, (k) -> new BoneState());

        state.transform.copy(transform);

        if (orient == null)
        {
            state.orient = null;
        }
        else if (state.orient == null)
        {
            state.orient = new Quaternionf(orient);
        }
        else
        {
            state.orient.set(orient);
        }

        if (offset == null)
        {
            state.offset = null;
        }
        else if (state.offset == null)
        {
            state.offset = new Vector3f(offset);
        }
        else
        {
            state.offset.set(offset);
        }
    }

    private static void apply(Map<String, BoneState> bones, IModel model)
    {
        if (bones == null)
        {
            return;
        }

        for (ModelGroup group : model.getAllGroups())
        {
            BoneState state = bones.get("g:" + group.id);

            if (state != null)
            {
                group.current.copy(state.transform);
                group.orient = state.orient == null ? null : new Quaternionf(state.orient);
                group.offset = state.offset == null ? null : new Vector3f(state.offset);
            }
        }

        for (BOBJBone bone : model.getAllBOBJBones())
        {
            BoneState state = bones.get("b:" + bone.name);

            if (state != null)
            {
                bone.transform.copy(state.transform);
                bone.orient = state.orient == null ? null : new Quaternionf(state.orient);
                bone.offset = state.offset == null ? null : new Vector3f(state.offset);
            }
        }
    }
}
