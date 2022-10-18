package me.third.right.worldDownloader.managers;


import jetbrains.exodus.entitystore.*;
import me.third.right.ThirdMod;
import me.third.right.utils.client.objects.Pair;
import me.third.right.utils.client.utils.ChatUtils;
import me.third.right.utils.client.utils.LoggerUtils;
import me.third.right.worldDownloader.utils.ValueStore;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    public void storeChunkInfo(Chunk chunk) {//TODO add Hash checks to see if the chunk has changed if not then don't store it.
        if(isClosing) return;
        StoreTransaction transaction = entityStore.beginTransaction();
        final int id = Objects.hashCode(chunk.x) ^ Objects.hashCode(chunk.z);
        //Block Counts
        try {
            do {
                //Block
                final Map<Block, Pair<Integer, HashSet<BlockPos>>> blockCount = new HashMap<>();
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 256; y++) {
                        for (int z = 0; z < 16; z++) {
                            final Block block = chunk.getBlockState(x, y, z).getBlock();
                            if (block instanceof BlockAir) continue;
                            if (blockCount.containsKey(block)) {
                                final Pair<Integer, HashSet<BlockPos>> pair = blockCount.get(block);
                                pair.setFirst(pair.getFirst() + 1);
                                pair.getSecond().add(new BlockPos(chunk.x * 16 + x, y, chunk.z * 16 + z));
                            } else {
                                blockCount.put(block, new Pair<>(1, new HashSet<>(Collections.singletonList(new BlockPos(chunk.x * 16 + x, y, chunk.z * 16 + z)))));
                            }
                        }
                    }
                }

                Entity entity = transaction.newEntity("Block");
                entity.setProperty("id", id);;
                entity.setProperty("chunkX", chunk.x);
                entity.setProperty("chunkZ", chunk.z);
                entity.setProperty("date", dtf.format(LocalDateTime.now()));
                entity.setProperty("dimension", mc.player.dimension);

                //#name:count@x,y,z|x,y,z
                //Split by # then by : then by @ then by ,
                final StringBuilder blockString = new StringBuilder();
                for (Map.Entry<Block, Pair<Integer, HashSet<BlockPos>>> entry : blockCount.entrySet()) {
                    blockString.append(String.format("#%s:%d@", entry.getKey().getLocalizedName(), entry.getValue().getFirst()));

                    for (BlockPos pos : entry.getValue().getSecond()) {
                        blockString.append(String.format("%d,%d,%d|", pos.getX(), pos.getY(), pos.getZ()));
                    }
                }
                entity.setProperty("blocks", blockString.toString());

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

    public HashMap<String , BigInteger>  calcTotals() {
        if(isClosing) return null;
        HashMap<String, BigInteger> blockCount = new HashMap<>();
        final EntityIterable blocks = entityStore.beginReadonlyTransaction().getAll("Block");
        for (Entity block : blocks) {
            final String[] blocksSplit = String.valueOf(block.getProperty("blocks")).split("#");
            for (String blockSplit : blocksSplit) {
                if (blockSplit.isEmpty()) continue;
                final String[] blockSplitSplit = blockSplit.split(":");
                final String blockName = blockSplitSplit[0];
                final int blockCountInt = Integer.parseInt(blockSplitSplit[1].split("@")[0]);
                if (blockCount.containsKey(blockName)) {
                    blockCount.put(blockName, blockCount.get(blockName).add(BigInteger.valueOf(blockCountInt)));
                } else {
                    blockCount.put(blockName, BigInteger.valueOf(blockCountInt));
                }
            }
        }
        return blockCount;
    }

    // * New Chunks START
    public void storeNewChunk(int x, int z) {//TODO change chunk location to chunkX and chunkZ instead of x and z
        if(isClosing) return;
        StoreTransaction transaction = entityStore.beginTransaction();
        try {
            do {
                final Entity newChunk = transaction.newEntity("NewChunk");
                newChunk.setProperty("id", Objects.hashCode(x) ^ Objects.hashCode(z));
                newChunk.setProperty("x", x);
                newChunk.setProperty("z", z);
                newChunk.setProperty("dimension", mc.player.dimension);
                newChunk.setProperty("date", dtf.format(LocalDateTime.now()));
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

    public ValueStore[] getEntry(String entity, int id) {
        if(isClosing) return null;
        final EntityIterable entries = entityStore.beginReadonlyTransaction().getAll(entity);
        final List<ValueStore> valueStores = new ArrayList<>();
        for (Entity entry : entries) {
            if(Objects.equals(entry.getProperty("id"), id)) {
                Pair<String, String>[] properties = new Pair[entry.getPropertyNames().size()];
                int index = 0;
                for(String name : entry.getPropertyNames()) {
                    properties[index] = new Pair<>(name, String.valueOf(entry.getProperty(name)));
                    index++;
                }
                valueStores.add(new ValueStore(entity, properties));
            }
        }
        return valueStores.toArray(new ValueStore[0]);
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
