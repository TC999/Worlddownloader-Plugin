package me.third.right.worldDownloader.hacks;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import me.bush.eventbus.annotation.EventListener;
import me.third.right.ThirdMod;
import me.third.right.events.client.PacketEvent;
import me.third.right.events.client.TickEvent;
import me.third.right.modules.Hack;
import me.third.right.modules.HackStandard;
import me.third.right.utils.client.enums.Category;
import me.third.right.utils.client.utils.LoggerUtils;
import me.third.right.worldDownloader.mixins.IChunkProviderClient;
import me.third.right.worldDownloader.utils.AnvilChunkWDL;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.nbt.*;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.ThreadedFileIOBase;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

@Hack.DontSaveState
@Hack.DisableOnDisconnect
@Hack.HackInfo(name = "WorldDownloader", description = "Downloads the world you are in", category = Category.OTHER)
public class WorldDownloader extends HackStandard {
    //TODO add world downloader
    //TODO add world database (When chunk has been saved, Block counts, etc)
    //Vars
    private AnvilChunkWDL anvilChunkWDL;
    private SaveHandler saveHandler;
    private int newChunks = 0;
    private int maxChunks = 0;
    //Settings

    //Overrides

    @Override
    public void onEnable() {
        ThirdMod.EVENT_PROCESSOR.subscribe(this);
        if(nullCheck()) {
            disable();
            return;
        }

        startDownload();
    }

    @Override
    public void onDisable() {
        ThirdMod.EVENT_PROCESSOR.unsubscribe(this);
        if(nullCheck()) return;

        stopDownload();
    }

    @Override
    public void onClose() {
        stopDownload();
    }

    @Override
    public String setHudInfo() {
        return String.format("%s/%s", newChunks, maxChunks);
    }

    //Events

    @EventListener
    public void onTick(TickEvent event) {
        if(nullCheck()) return;
        maxChunks = (mc.gameSettings.renderDistanceChunks * 16) / 4;
    }

    @EventListener
    public void onPacketEvent(PacketEvent.Receive event) {
        if(nullCheck()) return;

        if(event.getPacket() instanceof SPacketChunkData) {
            newChunks++;

            if(newChunks > maxChunks) {
                newChunks = 0;
                try {
                    saveChunks();
                } catch (NullPointerException e) {
                    LoggerUtils.logDebug("Failed at packet event.");
                }
            }
        }

    }

    //Methods

    public void startDownload() {
        saveHandler = (SaveHandler) mc.getSaveLoader().getSaveLoader("world", true);
        anvilChunkWDL = AnvilChunkWDL.create(saveHandler, mc.world.provider);
    }

    public void stopDownload() {
        if(nullCheck()) {
            LoggerUtils.moduleLog(this, "Potentially threw away a lot of data, cause you didn't stop the download before closing the game.");
            return;
        }
        saveWorld();
        mc.getSaveLoader().flushCache();
        saveHandler.flush();
    }


    public void saveWorld() {
        NBTTagCompound playerNBT = savePlayer();

        saveWorldInfo(playerNBT);
        saveChunks();

        try {
            ThreadedFileIOBase.getThreadedIOInstance().waitForFinish();
        } catch (Exception e) {
            throw new RuntimeException("Threw exception waiting for asynchronous IO to finish. Hmmm.", e);
        }
    }

    public void saveChunks() {
        final ChunkProviderClient chunkProvider = mc.world.getChunkProvider();

        final List<Chunk> chunks = new ArrayList<>(((IChunkProviderClient)chunkProvider).getLoadedChunks().values());
        if(chunks.isEmpty()) {
            LoggerUtils.logDebug("No chunks to save");
            return;
        }

        LoggerUtils.logDebug("Chunks size: "+chunks.size());

        for(Chunk chunk : chunks) {

            if(chunk == null) {
                LoggerUtils.logDebug("Chunk is null");
                continue;
            }

            if(isChunkEmpty(chunk)) {
                LoggerUtils.logDebug("Chunk is empty");
                continue;
            }

            try {
                anvilChunkWDL.saveChunk(mc.world, chunk);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isChunkEmpty(Chunk chunk) {
        if (chunk.isEmpty() || chunk instanceof EmptyChunk) {
            return true;
        }

        final ExtendedBlockStorage[] array = chunk.getBlockStorageArray();
        for (int i = 1; i < array.length; i++) {
            if (array[i] != Chunk.NULL_BLOCK_STORAGE) {
                return false;
            }
        }
        if (array[0] != Chunk.NULL_BLOCK_STORAGE) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        int id = Block.getStateId(array[0].get(x, y, z));
                        id = (id & 0xFFF) << 4 | (id & 0xF000) >> 12;
                        if ((id > 0x00F) && (id < 0x1A0 || id > 0x1AF)) {
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void saveWorldInfo(NBTTagCompound playerInfoNBT) {

        mc.world.getWorldInfo().setSaveVersion(19133);//19133 1343

        NBTTagCompound worldInfoNBT = mc.world.getWorldInfo().cloneNBTCompound(playerInfoNBT);
        NBTTagCompound rootWorldInfoNBT = new NBTTagCompound();
        rootWorldInfoNBT.setTag("Data", worldInfoNBT);

        applyOverridesToWorldInfo(worldInfoNBT, rootWorldInfoNBT);

        File saveDirectory = saveHandler.getWorldDirectory();
        File dataFile = new File(saveDirectory, "level.dat_new");
        File dataFileBackup = new File(saveDirectory, "level.dat_old");
        File dataFileOld = new File(saveDirectory, "level.dat");

        try (FileOutputStream stream = new FileOutputStream(dataFile)) {
            CompressedStreamTools.writeCompressed(rootWorldInfoNBT, stream);

            if (dataFileBackup.exists()) {
                dataFileBackup.delete();
            }

            dataFileOld.renameTo(dataFileBackup);

            if (dataFileOld.exists()) {
                dataFileOld.delete();
            }

            dataFile.renameTo(dataFileOld);

            if (dataFile.exists()) {
                dataFile.delete();
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't save the world metadata!", e);
        }
    }

    public static void applyOverridesToWorldInfo(NBTTagCompound worldInfoNBT, NBTTagCompound rootWorldInfoNBT) {//TODO automate this and add settings for this.
        // LevelName
        String worldName = "world";//TODO add StringSetting to chnage world name
        worldInfoNBT.setString("LevelName", worldName);
        // Cheats
        worldInfoNBT.setBoolean("allowCommands", true);
        // GameType
        worldInfoNBT.setInteger("GameType", 1); // Creative
        // Time
        worldInfoNBT.setLong("Time", mc.world.getWorldTime());
        // RandomSeed
        long seed = 0;
        worldInfoNBT.setLong("RandomSeed", seed);
        // MapFeatures
        worldInfoNBT.setBoolean("MapFeatures", false);
        // generatorName
        worldInfoNBT.setString("generatorName", "flat");
        // generatorOptions
        worldInfoNBT.setString("generatorOptions", ";0");
        // generatorVersion
        worldInfoNBT.setInteger("generatorVersion", 0);
        // Weather
        worldInfoNBT.setBoolean("raining", false);
        worldInfoNBT.setInteger("rainTime", 0);
        worldInfoNBT.setBoolean("thundering", false);
        worldInfoNBT.setInteger("thunderTime", 0);
        // Spawn
        int x = MathHelper.floor(mc.player.posX);
        int y = MathHelper.floor(mc.player.posY);
        int z = MathHelper.floor(mc.player.posZ);
        worldInfoNBT.setInteger("SpawnX", x);
        worldInfoNBT.setInteger("SpawnY", y);
        worldInfoNBT.setInteger("SpawnZ", z);
        worldInfoNBT.setBoolean("initialized", true);


        // Gamerules (most of these are already populated)
/*
        NBTTagCompound gamerules = worldInfoNBT.getCompoundTag("GameRules");
        for (String prop : worldProps.stringPropertyNames()) {
            if (!prop.startsWith("GameRule.")) {
                continue;
            }
            String rule = prop.substring("GameRule.".length());
            gamerules.setString(rule, worldProps.getProperty(prop));
        }
 */
    }

    public NBTTagCompound savePlayer() {

        NBTTagCompound playerNBT = new NBTTagCompound();
        mc.player.writeToNBT(playerNBT);

        applyOverridesToPlayer(playerNBT);

        File playersDirectory = new File(saveHandler.getWorldDirectory(), "playerdata");
        File playerFileTmp = new File(playersDirectory, mc.player.getUniqueID() + ".dat.tmp");
        File playerFile = new File(playersDirectory, mc.player.getUniqueID() + ".dat");

        try (FileOutputStream stream = new FileOutputStream(playerFileTmp)) {

            CompressedStreamTools.writeCompressed(playerNBT, stream);

            // Remove the old player file to make space for the new one.
            if (playerFile.exists()) {
                playerFile.delete();
            }

            playerFileTmp.renameTo(playerFile);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't save the player!", e);
        }

        return playerNBT;
    }

    private void applyOverridesToPlayer(NBTTagCompound playerNBT) {
        // Health
        playerNBT.setShort("Health", (short) mc.player.getHealth());

        // foodLevel, foodTimer, foodSaturationLevel, foodExhaustionLevel
        playerNBT.setInteger("foodLevel", 20);
        playerNBT.setInteger("foodTickTimer", 0);
        playerNBT.setFloat("foodSaturationLevel", 5.0f);

        // Player Position
        BlockPos playerPos = mc.player.getPosition();

        //Positions are offset to center of block,
        //or player height.
        NBTTagList pos = new NBTTagList();
        pos.appendTag(new NBTTagDouble(playerPos.getX() + 0.5D));
        pos.appendTag(new NBTTagDouble(playerPos.getY() + 0.621D));
        pos.appendTag(new NBTTagDouble(playerPos.getZ() + 0.5D));
        playerNBT.setTag("Pos", pos);
        NBTTagList motion = new NBTTagList();
        motion.appendTag(new NBTTagDouble(0.0D));
        //Force them to land on the ground?
        motion.appendTag(new NBTTagDouble(-0.0001D));
        motion.appendTag(new NBTTagDouble(0.0D));
        playerNBT.setTag("Motion", motion);
        NBTTagList rotation = new NBTTagList();
        rotation.appendTag(new NBTTagFloat(0.0f));
        rotation.appendTag(new NBTTagFloat(0.0f));
        playerNBT.setTag("Rotation", rotation);


        // If the player is able to fly, spawn them flying.
        // Helps ensure they don't fall out of the world.
        playerNBT.getCompoundTag("abilities").setBoolean("flying", true);
    }

}
