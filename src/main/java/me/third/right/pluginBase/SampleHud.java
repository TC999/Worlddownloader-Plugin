package me.third.right.pluginBase;

import me.third.right.hud.Hud;

import static me.third.right.utils.client.utils.ColourUtils.rgbToInt;

@Hud.HudInfo(name = "SampleHud")
public class SampleHud extends Hud {

    @Override
    public void onRender() {
        drawString("SampleHud", getX(), getY(), rgbToInt(255,0,0));
    }
}
