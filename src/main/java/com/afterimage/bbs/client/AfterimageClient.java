package com.afterimage.bbs.client;

import com.afterimage.bbs.AfterimageAddon;
import com.afterimage.bbs.AfterimageClip;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.events.BBSAddonMod;
import mchorse.bbs_mod.events.Subscribe;
import mchorse.bbs_mod.events.register.RegisterClientSettingsEvent;
import mchorse.bbs_mod.events.register.RegisterL10nEvent;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.util.Collections;

/**
 * The client half of the addon. In BBS FS 2.4 it is subscribed by hand (see {@link #attach()}),
 * because 2.4 only reads the {@code bbs-addon} entrypoint key.
 */
public class AfterimageClient implements BBSAddonMod
{
    private static boolean attached;

    public static void attach()
    {
        if (!attached)
        {
            attached = true;
            BBSMod.events.register(new AfterimageClient());
        }
    }

    @Subscribe
    public void onL10n(RegisterL10nEvent event)
    {
        /* English is BBS's fallback language, Russian is the second one shipped. Any other language
         * simply gets no file from this addon and falls back to English. */
        event.l10n.register((lang) ->
        {
            if (lang.equals("en_us") || lang.equals("ru_ru"))
            {
                return Collections.singletonList(Link.create(AfterimageAddon.MOD_ID + ":strings/" + lang + ".json"));
            }

            return Collections.<Link>emptyList();
        });

        /* BBS 2.4 loads its strings before it sends this event, so load ours now. */
        event.l10n.reload();
    }

    @Subscribe
    public void onSettings(RegisterClientSettingsEvent event)
    {
        event.register(Icons.GEAR, AfterimageAddon.MOD_ID, AfterimageSettings::register);

        /* The editor panel of the clip. */
        UIClip.register(AfterimageClip.class, UIAfterimageClip::new);
    }
}
