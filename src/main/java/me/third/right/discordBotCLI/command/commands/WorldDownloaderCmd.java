package me.third.right.discordBotCLI.command.commands;

import me.third.right.discordBotCLI.command.Cmd;
import me.third.right.discordBotCLI.utils.enums.Authority;
import me.third.right.worldDownloader.Main;
import me.third.right.worldDownloader.hacks.WorldDownloader;
import me.third.right.worldDownloader.managers.DatabaseManager;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.HashMap;

@Cmd.CmdInfo(name = "worldDownloader", description = "World Downloader Commands", authority = Authority.ADMIN)
public class WorldDownloaderCmd extends Cmd {
    @Override
    public void onMessage(MessageCreateEvent event, String[] strings) {
        final WorldDownloader worldDownloader = WorldDownloader.INSTANCE;
        final DatabaseManager databaseManager = worldDownloader.getDatabaseManager();

        final EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("World Downloader status:");
        embedBuilder.addField("Status:", worldDownloader.isEnabled() ? "Enabled" : "Disabled");
        if(databaseManager != null) {
            embedBuilder.addField("Database:", databaseManager.getDatabaseName());
            embedBuilder.addField("DB NewChunks Size:", databaseManager.getNewChunkCount()+"");
            embedBuilder.addField("DB TileEntity Size:", databaseManager.getTileCount()+"");
            embedBuilder.addField("DB Block Size:", databaseManager.getBlockCount()+"");

            final StringBuilder stringBuilder = new StringBuilder();
            HashMap<String , Integer> map = databaseManager.calcTotals();
            stringBuilder.append("Total:");
            map.forEach((key, value) -> stringBuilder.append("\n").append(key).append(": ").append(value));
            embedBuilder.setDescription(stringBuilder.toString());
        } else {
            embedBuilder.addField("Database Manager:", "Not initialized");
        }
        embedBuilder.setFooter("World Downloader v"+ Main.INSTANCE.getVersion());

        event.getChannel().sendMessage(embedBuilder);
    }
}
