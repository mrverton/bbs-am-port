package com.afterimage.bbs.client;

import com.afterimage.bbs.AfterimageClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIAnchorKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.utils.UI;

import java.util.ArrayList;
import java.util.List;

/**
 * The editor of an afterimage clip: every setting of the afterimages in one place, next to the
 * clip on the timeline.
 */
public class UIAfterimageClip extends UIClip<AfterimageClip>
{
    private static IKey key(String id)
    {
        return L10n.lang("afterimage.clip." + id);
    }

    private static mchorse.bbs_mod.ui.framework.elements.UIElement field(IKey label, mchorse.bbs_mod.ui.framework.elements.UIElement element)
    {
        return UI.column(UI.label(label), element);
    }

    private UITextbox target;
    private UIButton pick;
    private UIButton everyone;
    private UIToggle always;
    private UITrackpad interval;
    private UITrackpad spacing;
    private UITrackpad hold;
    private UITrackpad fade;
    private UITrackpad startAlpha;
    private UICirculate colorMode;
    private UIColor color1;
    private UIColor color2;
    private UITrackpad strength;
    private UITrackpad rainbowStep;
    private UITrackpad rainbowLife;
    private UICirculate driftDir;
    private UITrackpad driftSpeed;

    public UIAfterimageClip(AfterimageClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    /** Refreshers of the widgets. No initializer on purpose: registerUI() runs inside super(). */
    private List<Runnable> fillers;

    protected <T extends UIElement> T bind(T element, Runnable filler)
    {
        if (this.fillers == null)
        {
            this.fillers = new ArrayList<Runnable>();
        }

        this.fillers.add(filler);
        filler.run();

        return element;
    }

    @Override
    public void fillData()
    {
        super.fillData();

        if (this.fillers != null)
        {
            for (Runnable filler : this.fillers)
            {
                filler.run();
            }
        }
    }

    private UITextbox textbox(int maxLength, ValueString value)
    {
        UITextbox textbox = new UITextbox(maxLength, (text) -> this.editor.editMultiple(value, (v) -> v.set(text)));

        return this.bind(textbox, () -> textbox.setText(value.get()));
    }

    private UIToggle toggle(IKey label, ValueBoolean value)
    {
        UIToggle toggle = new UIToggle(label, (b) -> this.editor.editMultiple(value, (v) -> v.set(b.getValue())));

        return this.bind(toggle, () -> toggle.setValue(value.get()));
    }

    private UITrackpad trackpad(ValueInt value)
    {
        UITrackpad trackpad = new UITrackpad((d) -> this.editor.editMultiple(value, d.intValue()));

        trackpad.integer();

        return this.bind(trackpad, () -> trackpad.setValue(value.get()));
    }

    private UITrackpad trackpad(ValueFloat value)
    {
        UITrackpad trackpad = new UITrackpad((d) -> this.editor.editMultiple(value, (v) -> v.set(d.floatValue())));

        return this.bind(trackpad, () -> trackpad.setValue(value.get()));
    }

    private UIColor color(ValueInt value)
    {
        UIColor color = new UIColor((rgb) -> this.editor.editMultiple(value, rgb));

        return this.bind(color, () -> color.setColor(value.get()));
    }

    /** The target replay is stored as the replay's id (as in 2.7); 2.4's actor picker works by index. */
    private int targetIndex(Film film)
    {
        String target = this.clip.target.get();

        if (film != null)
        {
            List<Replay> list = film.replays.getList();

            for (int i = 0; i < list.size(); i++)
            {
                if (list.get(i).getId().equals(target))
                {
                    return i;
                }
            }
        }

        return -1;
    }

    private UICirculate circulate(ValueInt value, String... labels)
    {
        UICirculate button = new UICirculate((b) -> this.editor.editMultiple(value, b.getValue()));

        for (String label : labels)
        {
            button.addLabel(L10n.lang("afterimage." + label));
        }

        return this.bind(button, () -> button.setValue(Math.max(0, Math.min(labels.length - 1, value.get()))));
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.target = this.textbox(200, this.clip.target);
        this.target.tooltip(key("target.tooltip"));
        this.pick = new UIButton(key("target.pick"), (b) ->
        {
            UIFilmPanel panel = this.getParent(UIFilmPanel.class);

            if (panel != null)
            {
                UIAnchorKeyframeFactory.displayActors(this.getContext(), panel.getController().getEntities(), this.targetIndex(panel.getData()), (index) ->
                {
                    Film film = panel.getData();
                    String picked = "";

                    if (film != null && index != null && index >= 0 && index < film.replays.getList().size())
                    {
                        picked = film.replays.getList().get(index).getId();
                    }

                    /* A lambda may only capture a variable that is never reassigned. */
                    final String id = picked;

                    this.editor.editMultiple(this.clip.target, (v) -> v.set(id));
                    this.target.setText(id);
                });
            }
        });
        this.everyone = new UIButton(key("target.all"), (b) -> this.clip.target.set(""));

        this.always = this.toggle(key("always"), this.clip.always);
        this.always.tooltip(key("always.tooltip"));
        this.interval = this.trackpad(this.clip.interval);
        this.spacing = this.trackpad(this.clip.spacing);

        this.hold = this.trackpad(this.clip.hold);
        this.fade = this.trackpad(this.clip.fade);
        this.startAlpha = this.trackpad(this.clip.startAlpha);

        this.colorMode = this.circulate(this.clip.colorMode, "color.original", "color.solid", "color.gradient", "color.alternate", "color.rainbow");
        this.color1 = this.color(this.clip.color1);
        this.color2 = this.color(this.clip.color2);
        this.strength = this.trackpad(this.clip.strength);
        this.rainbowStep = this.trackpad(this.clip.rainbowStep);
        this.rainbowLife = this.trackpad(this.clip.rainbowLife);

        this.driftDir = this.circulate(this.clip.driftDir, "drift.none", "drift.right", "drift.left", "drift.up", "drift.down", "drift.forward", "drift.back");
        this.driftSpeed = this.trackpad(this.clip.driftSpeed);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(key("section.target"), this.target, UI.row(this.pick, this.everyone)));
        this.panels.add(this.section(key("section.when"), this.always, field(key("interval"), this.interval), field(key("spacing"), this.spacing)));
        this.panels.add(this.section(key("section.life"), field(key("hold"), this.hold), field(key("fade"), this.fade), field(key("start_alpha"), this.startAlpha)));
        this.panels.add(this.section(key("section.color"), this.colorMode, field(key("color1"), this.color1), field(key("color2"), this.color2),
                field(key("strength"), this.strength), field(key("rainbow_step"), this.rainbowStep), field(key("rainbow_life"), this.rainbowLife)));
        this.panels.add(this.section(key("section.motion"), this.driftDir, field(key("drift_speed"), this.driftSpeed)));
    }
}
