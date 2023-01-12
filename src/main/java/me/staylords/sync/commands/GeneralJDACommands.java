package me.staylords.sync.commands;

import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandPriority;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import me.staylords.sync.SyncPlugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GeneralJDACommands implements SlashCommandProvider {

    private final SyncPlugin plugin;

    public GeneralJDACommands(SyncPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        return new HashSet<>(Collections.singletonList(
                // Main command
                new PluginSlashCommand(this.plugin, new CommandData("sync", "A classic match of ping pong"), SlashCommandPriority.LAST)));
    }

    @SlashCommand(path = "sync", priority = SlashCommandPriority.LAST)
    public void syncCommand(SlashCommandEvent event) {
        System.out.println("test");
        event.reply("test").queue();
    }

}
