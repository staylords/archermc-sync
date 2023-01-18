package me.staylords.sync.task;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import lombok.var;
import me.staylords.sync.SyncPlugin;
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
        this.instance.initialize();

        /*
         * DiscordSRV uses an ancient JDA API which doesn't allow to use Models or correct JDA Command framework, so
         * we double register all of our commands in order to make sure players can execute private messages commands.
         */
        for (var pluginSlashCommand : this.instance.getSlashCommands()) {
            DiscordUtil.getJda().updateCommands().addCommands(pluginSlashCommand.getCommandData()).queue();
        }

        /*
         * After everything is loaded, we cancel this task and go ahead.
         */
        this.cancel();
    }
}
