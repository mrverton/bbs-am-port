package com.afterimage.bbs;

import com.afterimage.bbs.client.AfterimageClient;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.events.BBSAddonMod;
import mchorse.bbs_mod.events.Subscribe;
import mchorse.bbs_mod.events.register.RegisterSettingsEvent;
import mchorse.bbs_mod.events.register.RegisterSourcePacksEvent;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.resources.packs.InternalAssetsSourcePack;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The common half of the addon, registered under {@code bbs-addon}. BBS FS 2.4 has no separate
 * client entrypoint, so the client half is attached from here, and only on the client.
 */
public class AfterimageAddon implements BBSAddonMod
{
    public static final String MOD_ID = "afterimage";

    /** The icon of the afterimage clip (assets/afterimage/assets/textures/clip_icon.png), 16x16. */
    public static final mchorse.bbs_mod.ui.utils.icons.Icon CLIP_ICON = new mchorse.bbs_mod.ui.utils.icons.Icon(Link.create(MOD_ID + ":textures/clip_icon.png"), MOD_ID + "_clip", 0, 0, 16, 16, 16, 16);
    /** The clip type: it shows up in the camera clips' "add" menu as "Afterimage". */
    public static final Link CLIP = Link.create(MOD_ID + ":afterimage");

    @Subscribe
    public void onSourcePacks(RegisterSourcePacksEvent event)
    {
        /* Makes assets/afterimage/assets/... of this jar addressable as afterimage:... links. */
        event.provider.register(new InternalAssetsSourcePack(MOD_ID, "assets/" + MOD_ID + "/assets", AfterimageAddon.class));

        /* This event comes while BBS is still starting, before any client event of BBS is sent. */
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT)
        {
            AfterimageClient.attach();
        }
    }

    /** BBS 2.4 sends this after it has built the camera clip factory. */
    @Subscribe
    public void onRegisterSettings(RegisterSettingsEvent event)
    {
        try
        {
            BBSMod.getFactoryCameraClips().register(CLIP, AfterimageClip.class, new ClipFactoryData(CLIP_ICON, 0x35C8F5));
        }
        catch (Throwable e)
        {
            /* BBS's event bus swallows exceptions silently, so say it out loud. */
            e.printStackTrace();
        }
    }
}
