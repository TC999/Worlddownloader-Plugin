package me.third.right.worldDownloader.huds;

import me.third.right.hud.Hud;
import me.third.right.utils.client.objects.Pair;
import me.third.right.utils.client.utils.LoggerUtils;
import me.third.right.utils.render.Render2D;
import me.third.right.worldDownloader.hacks.MiniMap;
import me.third.right.worldDownloader.managers.ChunkImagerManager;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

@Hud.HudInfo(name = "MiniMap")
public class MiniMapElement extends Hud {
    public static MiniMapElement INSTANCE;
    private final MiniMap miniMap = MiniMap.INSTANCE;
    private final ChunkImagerManager chunkImagerManager = miniMap.getChunkImagerManager();
    private int width = 16;
    private int height = 16;
    private final HashMap<ChunkPos, Pair<ResourceLocation, Boolean>> chunkImageMap = new HashMap<>();

    public MiniMapElement() {
        INSTANCE = this;
        setRequirements(MiniMap.INSTANCE);
    }

    @Override
    public void onRender() {
        if(guiHud.nullCheckFull()) return;

        Render2D.drawRect(getX() - (width * miniMap.getSize()) - 2,
                getY() - (height * miniMap.getSize()) - 2,
                getX() + (width * miniMap.getSize()) + 2,
                getY() + (height * miniMap.getSize()) + 2,
                miniMap.getColour()
        );

        if(miniMap.isOutline()) {
            Render2D.drawOutlineRect(getX() - (width * miniMap.getSize()) - 1,
                    getY() - (height * miniMap.getSize()) - 1,
                    getX() + (width * miniMap.getSize()) + 1,
                    getY() + (height * miniMap.getSize()) + 1,
                    miniMap.getColourOutline(),
                    1.2F
            );
        }

        renderMap();
    }

    private void renderMap() {
        if(chunkImageMap.isEmpty()) return;

        for(int x = -miniMap.getSize(); x < miniMap.getSize(); x++) {
            for (int z = -miniMap.getSize(); z < miniMap.getSize(); z++) {
                final BlockPos pos = mc.player.getPosition().add(x * 16, 0, z * 16);
                final ChunkPos chunkPos = new ChunkPos(pos);

                if(!chunkImageMap.containsKey(chunkPos)) continue;

                final ResourceLocation resourceLocation = chunkImageMap.get(chunkPos).getFirst();
                if (resourceLocation == null) {
                    chunkImagerManager.chunkToImageFiltered(mc.world.getChunk(chunkPos.x, chunkPos.z));
                    continue;
                }

                int offsetX = getX() + (x * width);
                int offsetY = getY() + (z * height);

                boolean blend = GL11.glGetBoolean(GL11.GL_BLEND);
                GL11.glPushMatrix();
                if (!blend) GL11.glEnable(GL11.GL_BLEND);
                GL11.glColor4f(1, 1, 1, 1);
                mc.getTextureManager().bindTexture(resourceLocation);
                Gui.drawModalRectWithCustomSizedTexture(offsetX, offsetY, 0, 0, width, height, width, height);
                if (!blend) GL11.glDisable(GL11.GL_BLEND);
                GL11.glPopMatrix();
            }
        }
    }


    @Override
    public void onUpdate() {
        if(guiHud.nullCheckFull()) return;

        int maxReads = 4;//TODO adjust this.
        int chunkDistance = miniMap.getSize() + 2;

        mc.addScheduledTask(this::cleanUp);

        for(int x = -chunkDistance; x < chunkDistance; x++) {
            for (int z = -chunkDistance; z < chunkDistance; z++) {
                if(maxReads == 0) return;
                final BlockPos pos = mc.player.getPosition().add(x * 16, 0, z * 16);
                final ChunkPos chunkPos = new ChunkPos(pos);

                if(chunkImageMap.containsKey(chunkPos) && !chunkImageMap.get(chunkPos).getSecond()) {
                    continue;
                }

                final File path = chunkImagerManager.getImage(chunkPos.x, chunkPos.z);
                if(path == null) {
                    continue;
                }

                final BufferedImage bufferedImage = getImage(path, ImageIO::read);
                if (bufferedImage == null) continue;

                maxReads--;
                final DynamicTexture dynamicTexture = new DynamicTexture(bufferedImage);
                try {
                    dynamicTexture.loadTexture(mc.getResourceManager());
                } catch (IOException e) {
                    continue;
                }

                final Pair<ResourceLocation, Boolean> pair = new Pair<>(mc.getTextureManager().getDynamicTextureLocation("chunkImage", dynamicTexture), false);
                chunkImageMap.put(chunkPos, pair);
            }
        }
    }

    @Override
    public void onPinned() {
        reset();
    }

    @Override
    public void onUnpinned() {
        reset();
    }

    public void invalidateChunk(ChunkPos chunk) {
        if(chunkImageMap.containsKey(chunk)) {
            chunkImageMap.get(chunk).setSecond(true);
        }
    }

    private void cleanUp() {
        for(ChunkPos chunkPos : chunkImageMap.keySet()) {
            ChunkPos playerChunkPos = new ChunkPos(mc.player.getPosition());
            int xDiff = chunkPos.x - playerChunkPos.x;
            int zDiff = chunkPos.z - playerChunkPos.z;
            int distance = (int) Math.sqrt(xDiff * xDiff + zDiff * zDiff);
            if(distance > (miniMap.getSize() + 4)) {
                chunkImageMap.remove(chunkPos);
            }
        }
    }

    private void reset() {
        chunkImageMap.clear();
    }

    //TODO: move to utils for version 4.5
    private <T> BufferedImage getImage(T source, ThrowingFunction<T, BufferedImage> readFunction) {//TODO move this to main client.
        try {
            return readFunction.apply(source);
        } catch (IOException ex) {
            LoggerUtils.logError(ex.toString());
            return null;
        }
    }
    @FunctionalInterface
    private interface ThrowingFunction<T, R> {//TODO move this to main client.
        R apply(T obj) throws IOException;
    }
}
