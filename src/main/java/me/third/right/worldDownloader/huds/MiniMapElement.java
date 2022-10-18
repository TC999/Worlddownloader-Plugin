package me.third.right.worldDownloader.huds;

import me.third.right.ThirdMod;
import me.third.right.hud.Hud;
import me.third.right.utils.client.utils.ChatUtils;
import me.third.right.worldDownloader.hacks.MiniMap;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

@Hud.HudInfo(name = "MiniMap")
public class MiniMapElement extends Hud {
    private final MiniMap miniMap = MiniMap.INSTANCE;
    private int width = 16;
    private int height = 16;
    private Path path = null;
    private final HashMap<ChunkPos, ResourceLocation> chunkImageMap = new HashMap<>();

    public MiniMapElement() {
        setRequirements(MiniMap.INSTANCE);
    }

    @Override
    public void onRender() {
        if(mc.player == null || mc.world == null || path == null) return;

        if(chunkImageMap.isEmpty()) return;

        for(int x = -miniMap.getSize(); x < miniMap.getSize(); x++) {
            for (int z = -miniMap.getSize(); z < miniMap.getSize(); z++) {
                final BlockPos pos = mc.player.getPosition().add(x * 16, 0, z * 16);
                final ChunkPos chunkPos = new ChunkPos(pos);

                final ResourceLocation resourceLocation = chunkImageMap.get(chunkPos);
                if (resourceLocation == null) {
                    miniMap.getChunkImagerManager().chunkToImageFiltered(mc.world.getChunk(chunkPos.x, chunkPos.z));
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
        final String serverIP = ChatUtils.getFormattedServerIP();

        int maxReads = 4;
        int chunkDistance = miniMap.getSize() + 2;

        mc.addScheduledTask(this::cleanUp);

        for(int x = -chunkDistance; x < chunkDistance; x++) {
            for (int z = -chunkDistance; z < chunkDistance; z++) {
                if(maxReads == 0) return;

                final Path finalPath;
                final BlockPos pos = mc.player.getPosition().add(x * 16, 0, z * 16);
                final ChunkPos chunkPos = new ChunkPos(pos);

                if(chunkImageMap.containsKey(chunkPos)) {
                    continue;//TODO: check if image is up to date
                }
                switch (mc.player.dimension) {
                    case 0:
                        finalPath = ThirdMod.configFolder.resolve("ChunkImages").resolve(serverIP).resolve("Overworld").resolve(chunkPos.x + "," + chunkPos.z + ".png");
                        break;
                    case -1:
                        finalPath = ThirdMod.configFolder.resolve("ChunkImages").resolve(serverIP).resolve("Nether").resolve(chunkPos.x + "," + chunkPos.z + ".png");
                        break;
                    case 1:
                        finalPath = ThirdMod.configFolder.resolve("ChunkImages").resolve(serverIP).resolve("End").resolve(chunkPos.x + "," + chunkPos.z + ".png");
                        break;
                    default:
                        finalPath = null;
                        break;
                }

                path = finalPath;
                if (path == null) {
                    continue;
                }

                if (!Files.exists(path)) {
                    miniMap.getChunkImagerManager().chunkToImageFiltered(mc.world.getChunk(chunkPos.x, chunkPos.z));
                    continue;
                }

                final BufferedImage bufferedImage = getImage(path.toFile(), ImageIO::read);
                if (bufferedImage == null) continue;

                maxReads--;
                final DynamicTexture dynamicTexture = new DynamicTexture(bufferedImage);
                try {
                    dynamicTexture.loadTexture(mc.getResourceManager());
                } catch (IOException e) {
                    continue;
                }
                chunkImageMap.put(chunkPos, mc.getTextureManager().getDynamicTextureLocation(chunkPos.x + "," + chunkPos.z, dynamicTexture));
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

    private void cleanUp() {
        for(ChunkPos chunkPos : chunkImageMap.keySet()) {
            ChunkPos playerChunkPos = new ChunkPos(mc.player.getPosition());
            int xDiff = chunkPos.x - playerChunkPos.x;
            int zDiff = chunkPos.z - playerChunkPos.z;
            int distance = (int) Math.sqrt(xDiff * xDiff + zDiff * zDiff);
            if(distance > (miniMap.getSize() + 2)) {
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
            ex.printStackTrace();
            return null;
        }
    }
    @FunctionalInterface
    private interface ThrowingFunction<T, R> {//TODO move this to main client.
        R apply(T obj) throws IOException;
    }
}
