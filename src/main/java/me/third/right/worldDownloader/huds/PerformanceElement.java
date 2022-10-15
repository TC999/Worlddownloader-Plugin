package me.third.right.worldDownloader.huds;

import me.third.right.ThirdMod;
import me.third.right.hud.Hud;
import me.third.right.utils.client.enums.ReleaseType;
import me.third.right.worldDownloader.managers.PerformanceTracker;

@Hud.HudInfo(name = "Performance")
public class PerformanceElement extends Hud {

    public PerformanceElement() {
        setRequirements(x -> ThirdMod.releaseType.equals(ReleaseType.Development));
    }

    @Override
    public void onRender() {
        drawString(String.format("RenderTime: Avg: %sms Highest: %sms New: %s", PerformanceTracker.getAverage(), PerformanceTracker.getHighest(), PerformanceTracker.getNewest()), getX(), getY(), guiHud.getRGBInt());
    }
}
