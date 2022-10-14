package me.third.right.worldDownloader;

import me.third.right.commands.Command;
import me.third.right.hud.Hud;
import me.third.right.modules.Hack;
import me.third.right.plugins.PluginBase;
import me.third.right.worldDownloader.hacks.ImagerTest;
import me.third.right.worldDownloader.hacks.WorldDownloader;
import me.third.right.worldDownloader.huds.PerformanceElement;

@PluginBase.PluginInfo(name = "WorldDownloader", author = "ThirdRight", version = "1.1")
public class Main extends PluginBase {

    public static Main INSTANCE;

    public Main() {
        INSTANCE = this;
    }

    @Override
    public Hack[] registerHacks() {
        return new Hack[] {
                new WorldDownloader(),
                new ImagerTest()
        };
    }

    @Override
    public Hud[] registerHuds() {
        return new Hud[] {
                new PerformanceElement()
        };
    }

    @Override
    public Command[] registerCommands() {
        return new Command[0];
    }
}
