package me.third.right.worldDownloader;

import me.third.right.commands.Command;
import me.third.right.hud.Hud;
import me.third.right.modules.Hack;
import me.third.right.plugins.PluginBase;
import me.third.right.worldDownloader.hacks.WorldDownloader;

@PluginBase.PluginInfo(name = "PluginBase", author = "ThirdRight")
public class Main extends PluginBase {

    @Override
    public Hack[] registerHacks() {
        return new Hack[] {
                new WorldDownloader()
        };
    }

    @Override
    public Hud[] registerHuds() {
        return new Hud[0];
    }

    @Override
    public Command[] registerCommands() {
        return new Command[0];
    }
}
