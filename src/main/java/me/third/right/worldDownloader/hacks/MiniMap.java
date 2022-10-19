package me.third.right.worldDownloader.hacks;

import me.bush.eventbus.annotation.EventListener;
import me.third.right.ThirdMod;
import me.third.right.events.client.PacketEvent;
import me.third.right.events.client.TickEvent;
import me.third.right.modules.Hack;
import me.third.right.settings.setting.CheckboxSetting;
import me.third.right.settings.setting.SliderSetting;
import me.third.right.utils.client.enums.Category;
import me.third.right.worldDownloader.events.CImageCompleteEvent;
import me.third.right.worldDownloader.huds.MiniMapElement;
import me.third.right.worldDownloader.managers.ChunkImagerManager;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.network.play.server.SPacketMultiBlockChange;
import net.minecraft.world.chunk.Chunk;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static me.third.right.utils.client.utils.ColourUtils.rgbToInt;
import static me.third.right.worldDownloader.utils.ChunkUtils.isChunkEmpty;

@Hack.HackInfo(name = "MiniMap", description = "Shows a minimap.", category = Category.HUD)
public class MiniMap extends Hack {
    //Vars
    public static MiniMap INSTANCE;
    private final ChunkImagerManager chunkImagerManager = new ChunkImagerManager();

    //Settings
    private final SliderSetting size = setting(new SliderSetting("Size", 4,1,10,1, SliderSetting.ValueDisplay.INTEGER));
    private final SliderSetting red = setting(new SliderSetting("Red", 0,0,255,1, SliderSetting.ValueDisplay.INTEGER));
    private final SliderSetting green = setting(new SliderSetting("Green", 0,0,255,1, SliderSetting.ValueDisplay.INTEGER));
    private final SliderSetting blue = setting(new SliderSetting("Blue", 0,0,255,1, SliderSetting.ValueDisplay.INTEGER));
    private final SliderSetting alpha = setting(new SliderSetting("Alpha", 255,0,255,1, SliderSetting.ValueDisplay.INTEGER));
    private final CheckboxSetting outline = setting(new CheckboxSetting("Outline", true));
    private final SliderSetting redOutline = setting(new SliderSetting("RedOutline", 255,0,255,1, SliderSetting.ValueDisplay.INTEGER));
    private final SliderSetting greenOutline = setting(new SliderSetting("GreenOutline", 0,0,255,1, SliderSetting.ValueDisplay.INTEGER));
    private final SliderSetting blueOutline = setting(new SliderSetting("BlueOutline", 255,0,255,1, SliderSetting.ValueDisplay.INTEGER));
    private final SliderSetting alphaOutline = setting(new SliderSetting("AlphaOutline", 255,0,255,1, SliderSetting.ValueDisplay.INTEGER));

    public MiniMap() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        ThirdMod.EVENT_PROCESSOR.subscribe(this);
    }

    @Override
    public void onDisable() {
        ThirdMod.EVENT_PROCESSOR.subscribe(this);
    }

    //Events

    @EventListener
    public void onTick(TickEvent event) {
        if(nullCheckFull()) return;
        chunkImagerManager.onTick();
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if(nullCheckFull() || event.getPacket() == null) return;

        if(event.getPacket() instanceof SPacketChunkData) {
            final SPacketChunkData packet = (SPacketChunkData) event.getPacket();

            final Chunk chunk = mc.world.getChunk(packet.getChunkX(), packet.getChunkZ());

            if(isChunkEmpty(chunk)) return;
            chunkImagerManager.chunkToImageFiltered(chunk);

        } else if(event.getPacket() instanceof SPacketBlockChange) {
            final SPacketBlockChange packet = (SPacketBlockChange) event.getPacket();

            final Chunk chunk = mc.world.getChunk(packet.getBlockPosition());

            if(isChunkEmpty(chunk)) return;
            chunkImagerManager.chunkToImageFiltered(chunk);

        } else if(event.getPacket() instanceof SPacketMultiBlockChange) {
            final SPacketMultiBlockChange packet = (SPacketMultiBlockChange) event.getPacket();

            if(packet.getChangedBlocks().length != 0) {
                final Queue<Chunk> chunks = new ConcurrentLinkedQueue<>();
                for (SPacketMultiBlockChange.BlockUpdateData data : packet.getChangedBlocks()) {
                    final Chunk chunk = mc.world.getChunk(data.getPos());

                    if (isChunkEmpty(chunk)) continue;

                    if (chunks.contains(chunk)) continue;
                    chunks.add(chunk);
                }

                while (!chunks.isEmpty()) {
                    final Chunk chunk = chunks.poll();

                    chunkImagerManager.chunkToImageFiltered(chunk);
                }
            }
        }
    }

    @EventListener
    public void onRenderComplete(CImageCompleteEvent event) {
        if(nullCheckFull()) return;
        MiniMapElement.INSTANCE.invalidateChunk(event.getChunkPos());
    }

    public int getColour() {
        return rgbToInt(red.getValueI(), green.getValueI(), blue.getValueI(), alpha.getValueI());
    }

    public int getColourOutline() {
        return rgbToInt(redOutline.getValueI(), greenOutline.getValueI(), blueOutline.getValueI(), alphaOutline.getValueI());
    }

    public boolean isOutline() {
        return outline.isChecked();
    }

    public ChunkImagerManager getChunkImagerManager() {return chunkImagerManager;}

    public int getSize() {return size.getValueI();}
}
