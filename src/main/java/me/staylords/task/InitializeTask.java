package me.staylords.sync.task;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import github.scarsz.discordsrv.util.DiscordUtil;
import lombok.var;
import me.staylords.sync.SyncPlugin;
import me.staylords.sync.listeners.GeneralJDAListeners;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author staylords
 */
public class InitializeTask extends BukkitRunnable {

    private final SyncPlugin instance;

    public InitializeTask(SyncPlugin instance) {
        this.instance = instance;
    }

    @Override
    public void run() {
        /*
         * DiscordSRV needs to fully load its information before we can initialize
         * our basic project functions, so after waiting patiently we go for it.
         */
        if (!DiscordSRV.isReady) return;

        /*
         * Load
         */
        this.instance.check();
        DiscordUtil.getJda().addEventListener(new GeneralJDAListeners(this.instance));

        /*
         * DiscordSRV uses an ancient JDA API which doesn't allow to use Models or correct JDA Command framework, so
         * we double register all of our commands in order to make sure players can execute private messages commands.
         */
        this.instance.getSlashCommands().forEach(pluginSlashCommand -> DiscordUtil.getJda().updateCommands().addCommands(pluginSlashCommand.getCommandData()).queue());
        DiscordUtil.getJda().updateCommands().queue();
        DiscordUtil.getJda().upsertCommand(new CommandData("sync", "Link your Minecraft account to our Discord server!")
                .addOption(OptionType.INTEGER, "code", "The code you received in-game.", true)).queue();
        DiscordUtil.getJda().updateCommands().queue();

        /*
         * After everything is loaded, we cancel this task and go ahead.
         */
        this.cancel();
    }
}
