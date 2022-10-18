package me.third.right.worldDownloader.hacks;

import me.bush.eventbus.annotation.EventListener;
import me.third.right.ThirdMod;
import me.third.right.events.client.PacketEvent;
import me.third.right.events.client.TickEvent;
import me.third.right.modules.Hack;
import me.third.right.settings.setting.SliderSetting;
import me.third.right.utils.client.enums.Category;
import me.third.right.utils.client.utils.ChatUtils;
import me.third.right.worldDownloader.managers.ChunkImagerManager;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.world.chunk.Chunk;

import static me.third.right.worldDownloader.utils.ChunkUtils.isChunkEmpty;

@Hack.HackInfo(name = "MiniMap", description = "Shows a minimap.", category = Category.HUD)
public class MiniMap extends Hack {
    //Vars
    public static MiniMap INSTANCE;
    private String serverIP = "";
    private ChunkImagerManager chunkImagerManager;

    //Settings
    private final SliderSetting size = setting(new SliderSetting("Size", 4,1,10,1, SliderSetting.ValueDisplay.INTEGER));

    public MiniMap() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        ThirdMod.EVENT_PROCESSOR.subscribe(this);
        reset();
    }

    @Override
    public void onDisable() {
        ThirdMod.EVENT_PROCESSOR.subscribe(this);
        reset();
    }

    //Events

    @EventListener
    public void onTick(TickEvent event) {
        if(mc.player == null || mc.world == null) return;

        final String currentServerIP = ChatUtils.getFormattedServerIP();
        if(!serverIP.equals(currentServerIP)) {
            serverIP = currentServerIP;
            reset();
        }
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if(mc.player == null || mc.world == null) return;

        if(event.getPacket() instanceof SPacketChunkData) {
            final SPacketChunkData packet = (SPacketChunkData) event.getPacket();
            final Chunk chunk = mc.world.getChunk(packet.getChunkX(), packet.getChunkZ());

            if(chunk.isEmpty() || isChunkEmpty(chunk)) {
                return;
            }

            chunkImagerManager.chunkToImage(chunk);
        } else if(event.getPacket() instanceof SPacketBlockChange) {
            final SPacketBlockChange packet = (SPacketBlockChange) event.getPacket();
            final Chunk chunk = mc.world.getChunk(packet.getBlockPosition());

            if(chunk.isEmpty() || isChunkEmpty(chunk)) {
                return;
            }

            chunkImagerManager.chunkToImage(chunk);
        }
    }

    public void reset() {
        chunkImagerManager = new ChunkImagerManager(serverIP);
    }

    public ChunkImagerManager getChunkImagerManager() {return chunkImagerManager;}

    public int getSize() {return size.getValueI();}
}
