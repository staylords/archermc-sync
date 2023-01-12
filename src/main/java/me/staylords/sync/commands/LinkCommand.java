package me.staylords.sync.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import github.scarsz.discordsrv.util.DiscordUtil;
import me.staylords.sync.SyncPlugin;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

@CommandAlias("link")
public class LinkCommand extends BaseCommand {

    @Default
    @CommandPermission("")
    public void onLinkCommand(Player player) {
        AccountLinkManager accountManager = DiscordSRV.getPlugin().getAccountLinkManager();

        if (player instanceof ConsoleCommandSender) return;

        if (SyncPlugin.isSynchronized(player.getUniqueId())) {
            player.sendMessage("§9§m------------------------------------");
            player.sendMessage("§cHello " + player.getName() + "! Your Minecraft account is already linked with " + returnFancyName(player.getUniqueId()) + ". If you wish to un-link it follow the steps in the #sync channel.");
            player.sendMessage("§9§m------------------------------------");
            return;
        }

        /*
        Prevent filling the @getLinkingCodes Map with same UUID's
        Send the player a warning message
         */
        if (accountManager.getLinkingCodes().containsValue(player.getUniqueId())) {
            player.sendMessage("§c[§e!§c] Your old code is no longer valid! We're generating a new one!");
            accountManager.getLinkingCodes().values().remove(player.getUniqueId());
        }

        player.sendMessage("§9§m------------------------------------");
        player.sendMessage("§c§lArcherMC Synchronization Process");
        player.sendMessage("");
        player.sendMessage("§eHello §e§l" + player.getName() + "§e! Here's your 4 digits code: §e§n§l" + accountManager.generateCode(player.getUniqueId()) + "§e. Join discord.gg/archermc and follow the steps in the #sync channel in order to get your Minecraft account linked!");
        player.sendMessage("§9§m------------------------------------");
    }

    private String returnFancyName(UUID uuid) {
        AccountLinkManager accountManager = DiscordSRV.getPlugin().getAccountLinkManager();
        return Objects.requireNonNull(DiscordUtil.getJda().getUserById(accountManager.getDiscordId(uuid)), accountManager.getDiscordId(uuid)).getAsTag();
    }

}
