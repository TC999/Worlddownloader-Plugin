package me.third.right.worldDownloader.managers;


import jetbrains.exodus.entitystore.*;
import me.third.right.ThirdMod;
import me.third.right.utils.client.objects.Triplet;
import me.third.right.utils.client.utils.ChatUtils;
import me.third.right.utils.client.utils.LoggerUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.chunk.Chunk;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DatabaseManager {
    protected Minecraft mc = Minecraft.getMinecraft();
    private final String databaseName;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private final PersistentEntityStore entityStore;
    private boolean isClosing = false;

    public DatabaseManager(String databaseName) {
        this.databaseName = databaseName;
        if(databaseName.isEmpty()) {
            LoggerUtils.logError("Database name is empty");
            entityStore = PersistentEntityStores.newInstance(ThirdMod.configFolder.resolve("WorldDatabase").resolve(ChatUtils.getFormattedServerIP()).toFile());
            return;
        }
        entityStore = PersistentEntityStores.newInstance(ThirdMod.configFolder.resolve("WorldDatabase").resolve(databaseName).toFile());
    }


    public void storeChunkInfo(Chunk chunk) {
        if(isClosing) return;
        StoreTransaction transaction = entityStore.beginTransaction();
        final int id = Objects.hashCode(chunk.x) ^ Objects.hashCode(chunk.z);

        try {
            do {
                //TileEntity
                for (TileEntity tileEntity : chunk.getTileEntityMap().values()) {
                    Entity entity = transaction.newEntity("TileEntity");
                    entity.setProperty("id", id);
                    entity.setProperty("x", tileEntity.getPos().getX());
                    entity.setProperty("y", tileEntity.getPos().getY());
                    entity.setProperty("z", tileEntity.getPos().getZ());
                    entity.setProperty("chunkX", chunk.x);
                    entity.setProperty("chunkZ", chunk.z);
                    entity.setProperty("date", dtf.format(LocalDateTime.now()));
                    entity.setProperty("class", tileEntity.getClass().getName());
                    entity.setProperty("dimension", mc.player.dimension);
                }
                if(transaction != entityStore.getCurrentTransaction()) {
                    transaction = null;
                    break;
                }
            } while (!transaction.flush());
        } finally {
            if(transaction != null) {
                transaction.abort();
            }
        }

        transaction = entityStore.beginTransaction();
        try {
            do {
                //Block
                final Map<Block, Integer> blockCount = new HashMap<>();
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 256; y++) {
                        for (int z = 0; z < 16; z++) {
                            final Block block = chunk.getBlockState(x, y, z).getBlock();
                            if (block instanceof BlockAir) continue;
                            if (blockCount.containsKey(block)) {
                                blockCount.put(block, blockCount.get(block) + 1);
                            } else {
                                blockCount.put(block, 1);
                            }
                        }
                    }
                }

                for (Map.Entry<Block, Integer> entry : blockCount.entrySet()) {
                    Entity entity = transaction.newEntity("Block");
                    entity.setProperty("id", id);
                    entity.setProperty("block", entry.getKey().getLocalizedName());
                    entity.setProperty("count", entry.getValue());
                    entity.setProperty("chunkX", chunk.x);
                    entity.setProperty("chunkZ", chunk.z);
                    entity.setProperty("date", dtf.format(LocalDateTime.now()));
                    entity.setProperty("dimension", mc.player.dimension);
                }
                if(transaction != entityStore.getCurrentTransaction()) {
                    transaction = null;
                    break;
                }
            } while (!transaction.flush());
        } finally {
            if(transaction != null) {
                transaction.abort();
            }
        }
    }

    public long getBlockCount() {
        if(isClosing) return 0;
        return entityStore.beginReadonlyTransaction().getAll("Block").size();
    }

    public long getTileCount() {
        if(isClosing) return 0;
        return entityStore.beginReadonlyTransaction().getAll("TileEntity").size();
    }

    public HashMap<String , Integer>  calcTotals() {
        if(isClosing) return null;
        HashMap<String , Integer> blockCount = new HashMap<>();
        final EntityIterable blocks = entityStore.beginReadonlyTransaction().getAll("Block");
        for(Entity block : blocks) {
            final String blockName = String.valueOf(block.getProperty("block"));
            final int count = Integer.parseInt(String.valueOf(block.getProperty("count")));
            if(blockCount.containsKey(blockName)) {
                blockCount.put(blockName, blockCount.get(blockName) + count);
            } else {
                blockCount.put(blockName, count);
            }
        }
        return blockCount;
    }

    // * New Chunks START
    public void storeNewChunk(int x, int z) {
        if(isClosing) return;
        StoreTransaction transaction = entityStore.beginTransaction();
        try {
            do {
                final Entity newChunk = transaction.newEntity("NewChunk");
                newChunk.setProperty("id", Objects.hashCode(x) ^ Objects.hashCode(z));
                newChunk.setProperty("x", x);
                newChunk.setProperty("z", z);
                newChunk.setProperty("dimension", mc.player.dimension);
                final LocalDateTime now = LocalDateTime.now();
                newChunk.setProperty("date", dtf.format(now));
                if(transaction != entityStore.getCurrentTransaction()) {
                    transaction = null;
                    break;
                }
            } while (!transaction.flush());
        } finally {
            if(transaction != null) {
                transaction.abort();
            }
        }
    }

    public Triplet<Integer, Integer, String> getNewChunkEntry(int id) {
        if(isClosing) return null;
        final EntityIterable newChunks = entityStore.beginReadonlyTransaction().getAll("NewChunk");
        for (Entity newChunk : newChunks) {
            if(Objects.equals(newChunk.getProperty("id"), id)) {
                int x = Integer.parseInt(String.valueOf(newChunk.getProperty("x")));
                int z = Integer.parseInt(String.valueOf(newChunk.getProperty("z")));
                String date = String.valueOf(newChunk.getProperty("date"));
                return new Triplet<>(x, z, date);
            }
        }
        return null;
    }

    public long getNewChunkCount() {
        if(isClosing) return 0;
        return entityStore.beginReadonlyTransaction().getAll("NewChunk").size();
    }
    // * New Chunks END

    public String getDatabaseName() {
        return databaseName;
    }

    public void disconnect() {
        isClosing = true;
        while(entityStore.getCurrentTransaction() != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        entityStore.close();
    }
}
