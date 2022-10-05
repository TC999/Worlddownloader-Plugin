package me.third.right.pluginBase;

import me.bush.eventbus.annotation.EventListener;
import me.third.right.ThirdMod;
import me.third.right.events.client.TickEvent;
import me.third.right.modules.Hack;
import me.third.right.modules.HackStandard;
import me.third.right.utils.client.enums.Category;
import me.third.right.utils.client.utils.ChatUtils;

@Hack.HackInfo(name = "SampleHack", description = "Sample hack", category = Category.OTHER)
public class SampleHack extends HackStandard {

    @Override
    public void onEnable() {
        ThirdMod.EVENT_PROCESSOR.subscribe(this);
    }

    @Override
    public void onDisable() {
        ThirdMod.EVENT_PROCESSOR.unsubscribe(this);
    }

    @EventListener
    public void onTick(TickEvent event) {
        if(nullCheck()) return;
        ChatUtils.debug("SampleHack is running!");
    }
}
