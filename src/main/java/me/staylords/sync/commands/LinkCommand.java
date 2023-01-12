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

        if (SyncPlugin.isLinked(player.getUniqueId())) {
            player.sendMessage("§9§m------------------------------------");
            player.sendMessage("§cHello " + player.getName() + "! Your Minecraft account is already linked to " + returnFancyName(player.getUniqueId()) + ". If you wish to unlink it follow the steps in the #sync channel.");
            player.sendMessage("§9§m------------------------------------");
            return;
        }

        /*
        We prevent filling github.scarsz.discordsrv.objects.managers.AccountLinkManager.getLinkingCodes Map<String, UUID>
        with the same UUID so after a little check we remove it and send the player a 'warning' message.
         */
        if (accountManager.getLinkingCodes().containsValue(player.getUniqueId())) {
            player.sendMessage("§7§o[Server: Generating a new code for " + player.getName() + "]");
            accountManager.getLinkingCodes().values().remove(player.getUniqueId());
        }

        String[] toMessage = {
                "§9§m------------------------------------",
                "§c§lArcherMC Synchronization Process",
                "§c§l<§e!§c§l> §cDo not share your code with anyone §c§l<§e!§c§l>",
                "",
                "§eHello §e§l" + player.getName() + "§e! Here's your 4 digits code: §e§l" + accountManager.generateCode(player.getUniqueId()) + "§e. ",
                "§eJoin discord.gg/archermc and follow the steps in the #sync channel in order to get your Minecraft account linked!",
                "§9§m------------------------------------"
        };

        player.sendMessage(toMessage);
    }

    /**
     * Clean discord tag if the player is still through the members. Instead, we return its User ID.
     *
     * @return fancy discord name
     */
    private String returnFancyName(UUID uuid) {
        AccountLinkManager accountManager = DiscordSRV.getPlugin().getAccountLinkManager();
        return DiscordUtil.getJda().getUserById(accountManager.getDiscordId(uuid)) == null ? accountManager.getDiscordId(uuid) : Objects.requireNonNull(DiscordUtil.getJda().getUserById(accountManager.getDiscordId(uuid))).getAsTag();
    }

}
