package me.third.right.worldDownloader.managers;

import me.bush.eventbus.annotation.EventListener;
import me.third.right.ThirdMod;
import me.third.right.events.client.TickEvent;
import me.third.right.utils.client.manage.ThreadManager;
import me.third.right.utils.client.utils.FileUtils;
import me.third.right.worldDownloader.utils.CImagerRunnable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.chunk.Chunk;

import java.nio.file.Path;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChunkImagerManager {
    protected Minecraft mc = Minecraft.getMinecraft();
    protected ThreadManager threadManager = ThreadManager.INSTANCE;
    private final String serverIP;
    private final Path imagePathDir;
    private final Queue<CImagerRunnable> queue = new ConcurrentLinkedQueue<>();

    public ChunkImagerManager(String serverIP) {
        this.serverIP = serverIP;
        imagePathDir = ThirdMod.configFolder.resolve("ChunkImages");
        //TODO add mass folder check.
        FileUtils.folderExists(imagePathDir);
        FileUtils.folderExists(imagePathDir.resolve(serverIP));
        FileUtils.folderExists(imagePathDir.resolve(serverIP).resolve("Overworld"));
        FileUtils.folderExists(imagePathDir.resolve(serverIP).resolve("Nether"));
        FileUtils.folderExists(imagePathDir.resolve(serverIP).resolve("End"));
        ThirdMod.EVENT_PROCESSOR.subscribe(this);
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
        if(mc.player == null || mc.world == null) return;
        if(threadManager.getQueueSize() < threadManager.getPoolSize() * 2) {
            if(!queue.isEmpty()) {
                threadManager.submit(queue.poll());
            }
        }
    }
}
