package me.third.right.pluginBase;

import me.third.right.commands.Command;
import me.third.right.utils.client.utils.ChatUtils;

@Command.CmdInfo(name = "sample", description = "Sample command", syntax = ".sample")
public class SampleCommand extends Command {

    @Override
    public void call(String[] strings) throws CmdException {
        ChatUtils.message("Sample command called!");
    }
}
