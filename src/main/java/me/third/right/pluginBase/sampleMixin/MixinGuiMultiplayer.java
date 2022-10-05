package me.third.right.pluginBase.sampleMixin;

import me.third.right.utils.client.utils.LoggerUtils;
import net.minecraft.client.gui.GuiMultiplayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMultiplayer.class)
public class MixinGuiMultiplayer {

    @Inject(
            method = {"initGui"},
            at = {@At("HEAD")}
    )
    public void drawScreen(CallbackInfo ci) {
        LoggerUtils.logBasic("Sample text");
    }
}
