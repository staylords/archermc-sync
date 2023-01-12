package me.staylords.sync.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

@CommandAlias("sync")
public class SyncCommand extends BaseCommand {

    @Default
    public void sync(Player player) {
        AccountLinkManager accountManager = DiscordSRV.getPlugin().getAccountLinkManager();
        if (player instanceof ConsoleCommandSender) return;

        /*
        Prevent filling the @getLinkingCodes Map with same UUID's
        Check is pointless since it will only remove it as long as it's there.
         */
        accountManager.getLinkingCodes().values().remove(player.getUniqueId());

        player.sendMessage("§m-----------------------");
        player.sendMessage("§d§lArcherMC Discord Sync");
        player.sendMessage("§m-----------------------");
        player.sendMessage("§aHello §a§l" + player.getName() + "§a! §aWe generated a code for you.");
        player.sendMessage("§eYour sync code is: §e§l" + accountManager.generateCode(player.getUniqueId()) + "§e.");
        player.sendMessage("§cIf you'd like to end this process, type §c§l/cancel§c.");
        player.sendMessage("§m-----------------------");
    }

}
