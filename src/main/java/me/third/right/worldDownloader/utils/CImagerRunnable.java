package me.third.right.worldDownloader.utils;

import me.third.right.utils.client.utils.BlockUtils;
import me.third.right.utils.client.utils.ChatUtils;
import me.third.right.worldDownloader.managers.PerformanceTracker;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.apache.commons.lang3.time.StopWatch;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static me.third.right.worldDownloader.hacks.WorldDownloader.isChunkEmpty;

public class CImagerRunnable implements Runnable {
    protected final Minecraft mc = Minecraft.getMinecraft();
    private final Path finalPath;
    private final Chunk chunk;

    public CImagerRunnable(Path finalPath, Chunk chunk) {
        this.finalPath = finalPath;
        this.chunk = chunk;
    }

    @Override
    public void run() {
        if(mc.player == null || mc.world == null) return;

        if(chunk == null) {
            return;
        }

        if(isChunkEmpty(chunk)) {
            return;
        }

        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        int lastY = -1;
        File outfile = finalPath.toFile();
        BufferedImage bufferedImage = new BufferedImage(16, 16, 2);
        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {

                int skyY = 256;
                while(skyY != 0) {
                    BlockPos pos = new BlockPos(chunk.x * 16 + x, skyY, chunk.z * 16 + z);
                    IBlockState state = mc.world.getBlockState(pos);
                    Block block = state.getBlock();
                    if(block instanceof BlockAir) {
                        skyY--;
                        continue;
                    }
                    //TODO add Nether but split the chunk it layered segments.

                    if(BlockUtils.isSolid(block)) {
                        MapColor mapColor = state.getMapColor(chunk.getWorld(), pos);
                        int colour = mapColor.colorValue;

                        if (lastY != -1){
                            int diff = lastY - skyY;
                            colour = darken(colour, Math.max(0, Math.min(diff * 10, 60)));
                        }

                        bufferedImage.setRGB(x, z, colour | 254 << 24);//Set to block map colour.
                        lastY = skyY;
                        break;
                    } else if(BlockUtils.isLiquid(block)) {
                        int colourBlend = state.getMapColor(chunk.getWorld(), pos).colorValue;
                        if(skyY <= 1) {
                            bufferedImage.setRGB(x, z, colourBlend | 254 << 24);//Set to liquid colour if there is only one block below.
                            break;
                        }

                        int searchDepth = 5;
                        int newY = skyY;
                        while(searchDepth != 0) {
                            BlockPos pos2 = new BlockPos(chunk.x * 16 + x, newY, chunk.z * 16 + z);
                            IBlockState state2 = mc.world.getBlockState(pos2);
                            Block block2 = state2.getBlock();
                            if(block2 instanceof BlockAir) {
                                newY--;
                                searchDepth--;
                                continue;
                            }

                            if(BlockUtils.isSolid(block2)) {
                                colourBlend = blendColour(colourBlend, state2.getMapColor(chunk.getWorld(), pos2).colorValue);//Blend the colour with the block below.
                                break;
                            } else if(BlockUtils.isLiquid(block2)) {
                                colourBlend = darken(colourBlend, 30);//Darken the colour.
                                newY--;
                                searchDepth--;
                            }
                        }

                        bufferedImage.setRGB(x, z, colourBlend | 254 << 24);//Set to liquid colour if there is only one block below.
                        break;
                    } else {
                        skyY--;
                    }
                }

                if(skyY == 0) {
                    bufferedImage.setRGB(x, z, -16777216);//Set to black
                }
            }
        }

        try {
            ImageIO.write(bufferedImage, "png", outfile);
        } catch (IOException var5) {
            ChatUtils.error("Could not save map.");
        }

        stopWatch.stop();
        PerformanceTracker.addTime(stopWatch.getTime(TimeUnit.MILLISECONDS));
    }

    private int darken(int colour, int amount) {
        int r = (colour >> 16) & 0xFF;
        int g = (colour >> 8) & 0xFF;
        int b = colour & 0xFF;

        r = Math.max(0, r - amount);
        g = Math.max(0, g - amount);
        b = Math.max(0, b - amount);

        return (r << 16) | (g << 8) | b;
    }

    private int blendColour(int colour1, int colour2) {//Auto generated by Github Copilot
        int r1 = (colour1 >> 16) & 0xFF;
        int g1 = (colour1 >> 8) & 0xFF;
        int b1 = (colour1) & 0xFF;

        int r2 = (colour2 >> 16) & 0xFF;
        int g2 = (colour2 >> 8) & 0xFF;
        int b2 = (colour2) & 0xFF;

        int r = (r1 + r2) / 2;
        int g = (g1 + g2) / 2;
        int b = (b1 + b2) / 2;

        return (r << 16) | (g << 8) | b;
    }

}
