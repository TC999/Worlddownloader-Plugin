package me.third.right.pluginBase;

import me.third.right.commands.Command;
import me.third.right.hud.Hud;
import me.third.right.modules.Hack;
import me.third.right.plugins.PluginBase;

@PluginBase.PluginInfo(name = "PluginBase", author = "ThirdRight")
public class Main extends PluginBase {

    @Override
    public Hack[] registerHacks() {
        return new Hack[] {new SampleHack()};
    }

    @Override
    public Hud[] registerHuds() {
        return new Hud[] {new SampleHud()};
    }

    @Override
    public Command[] registerCommands() {
        return new Command[]{new SampleCommand()};
    }
}
