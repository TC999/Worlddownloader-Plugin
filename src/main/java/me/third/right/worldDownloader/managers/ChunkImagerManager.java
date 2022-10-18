package me.third.right.worldDownloader.managers;

import me.bush.eventbus.annotation.EventListener;
import me.third.right.ThirdMod;
import me.third.right.events.client.TickEvent;
import me.third.right.utils.client.manage.ThreadManager;
import me.third.right.utils.client.utils.FileUtils;
import me.third.right.worldDownloader.utils.CImagerRunnable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.chunk.Chunk;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static me.third.right.worldDownloader.utils.ChunkUtils.isChunkEmpty;

public class ChunkImagerManager {
    protected Minecraft mc = Minecraft.getMinecraft();
    protected ThreadManager threadManager = ThreadManager.INSTANCE;
    private final String serverIP;
    private final Path imagePathDir;
    private final Queue<CImagerRunnable> queue = new ConcurrentLinkedQueue<>();

    public ChunkImagerManager(String serverIP) {
        this.serverIP = serverIP;
        imagePathDir = ThirdMod.configFolder.resolve("ChunkImages");
        FileUtils.folderExists(
                imagePathDir,
                imagePathDir.resolve(serverIP),
                imagePathDir.resolve(serverIP).resolve("Overworld"),
                imagePathDir.resolve(serverIP).resolve("Nether"),
                imagePathDir.resolve(serverIP).resolve("End")
        );
        ThirdMod.EVENT_PROCESSOR.subscribe(this);
    }

    public void chunkToImageFiltered(Chunk chunk) {
        if(chunk == null) {
            return;
        }

        if(isChunkEmpty(chunk)) {
            return;
        }

        chunkToImage(chunk);
    }

    public void chunkToImage(Chunk chunk) {
        final Path finalPath;
        switch (mc.player.dimension) {
            case 0:
                finalPath = imagePathDir.resolve(serverIP).resolve("Overworld").resolve(chunk.x + "," + chunk.z + ".png");
                break;
            case -1:
                finalPath = imagePathDir.resolve(serverIP).resolve("Nether").resolve(chunk.x + "," + chunk.z + ".png");
                break;
            case 1:
                finalPath = imagePathDir.resolve(serverIP).resolve("End").resolve(chunk.x + "," + chunk.z + ".png");
                break;
            default:
                finalPath = null;
                break;
        }
        if(finalPath == null) return;
        if(threadManager.getQueueSize() >= threadManager.getPoolSize() * 2) {
            queue.add(new CImagerRunnable(finalPath, chunk));
        } else {
            threadManager.submit(new CImagerRunnable(finalPath, chunk));
        }
    }

    @EventListener
    public void onTick(TickEvent event) {
        if(mc.player == null || mc.world == null || threadManager == null) return;
        if(threadManager.getQueueSize() < threadManager.getPoolSize() * 2) {
            if(!queue.isEmpty()) {
                threadManager.submit(queue.poll());
            }
        }
    }

    public File getImage(int chunkX, int chunkZ) {
        return getImage(chunkX, chunkZ, mc.player.dimension);
    }

    public File getImage(int chunkX, int chunkZ, int dimension) {
        final File file;
        switch (dimension) {
            case 0:
                file = imagePathDir.resolve(serverIP).resolve("Overworld").resolve(chunkX + "," + chunkZ + ".png").toFile();
                break;
            case -1:
                file = imagePathDir.resolve(serverIP).resolve("Nether").resolve(chunkX + "," + chunkZ + ".png").toFile();
                break;
            case 1:
                file = imagePathDir.resolve(serverIP).resolve("End").resolve(chunkX + "," + chunkZ + ".png").toFile();
                break;
            default:
                return null;
        }

        if(Files.exists(file.toPath())) {
            return file;
        } else {
            return null;
        }
    }
}
