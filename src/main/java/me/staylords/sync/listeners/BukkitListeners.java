package me.staylords.sync.listeners;

import fun.lewisdev.coinflip.events.CoinflipCompletedEvent;
import fun.lewisdev.coinflip.events.CoinflipCreatedEvent;
import fun.lewisdev.coinflip.game.CoinflipGame;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.MessageBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import github.scarsz.discordsrv.util.DiscordUtil;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TablistFormatManager;
import me.staylords.sync.SyncPlugin;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.jonspitfire.economyapi.types.Economy;

import java.awt.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author staylords
 */
public class BukkitListeners implements Listener {

    private final SyncPlugin instance;

    public BukkitListeners(SyncPlugin instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!SyncPlugin.isLinked(player.getUniqueId())) return;
        if (!SyncPlugin.isInDiscord(player.getUniqueId())) return;

        this.instance.update(player);
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!SyncPlugin.isLinked(player.getUniqueId())) return;
        if (!SyncPlugin.isInDiscord(player.getUniqueId())) return;

        this.instance.update(player);
    }

    @EventHandler
    public void onCoinflipCreatedEvent(CoinflipCreatedEvent event) {
        CoinflipGame game = event.getCoinflipGame();
        if (game == null) return;
        Economy provider = event.getCoinflipGame().getProvider();

        Button takeCoinflipButton = Button.success("take-coinflip", "Take Coinflip");
        Button createCoinflipButton = Button.primary("create-coinflip", "Create a Coinflip");
        Button checkBalanceButton = Button.primary("check-balance", "Check your balance");

        EmbedBuilder builder = new EmbedBuilder();
        builder
                .setColor(new Color(255, 65, 65))
                .setTitle(game.getOfflinePlayer().getName() + "'s coinflip")
                .addField("Value", ChatColor.stripColor(provider.format(game.getAmount())), true)
                .addField("Currency", WordUtils.capitalize(provider.getInputName()), true)
                .setThumbnail("https://mc-heads.net/avatar/" + game.getOfflinePlayer().getName())
                .setFooter("Think luck is on your side? Face " + game.getOfflinePlayer().getName() + " in a coinflip!");

        Message message = new MessageBuilder()
                .setEmbeds(builder.build())
                .setActionRows(ActionRow.of(takeCoinflipButton, createCoinflipButton, checkBalanceButton))
                .build();
        DiscordUtil.queueMessage(DiscordUtil.getTextChannelById(SyncPlugin.COIN_FLIP_CHANNEL), message);
    }

    @EventHandler
    public void onCoinflipCompletedEvent(CoinflipCompletedEvent event) {
        TextChannel channel = DiscordUtil.getTextChannelById(SyncPlugin.COIN_FLIP_CHANNEL);

        String toReturn = "**" + returnFancyName(event.getWinner().getUniqueId()) + "** has defeated **" + returnFancyName(event.getLoser().getUniqueId()) +
                "** in a **" + ChatColor.stripColor(event.getProvider().format(event.getWinnings())) + "** coinflip!";

        DiscordUtil.queueMessage(channel, toReturn);

        /*
         * We try to delete the discord message even tho the coinflip was only interacted in-game.
         * Only retrieving past 100 messages to prevent performance issues.
         */
        channel.getHistory().retrievePast(100)
                .queueAfter(1, TimeUnit.SECONDS, messages -> messages
                        .stream()
                        .filter(m -> m != null && !m.getEmbeds().isEmpty())
                        .forEach(m -> m.getEmbeds()
                                .stream()
                                .filter(e -> e.getTitle() != null && e.getTitle().startsWith(event.getAuthor().getName()))
                                .forEach(e -> m.delete().queue())));
    }

    private String returnFancyName(UUID uuid) {
        AccountLinkManager accountManager = DiscordSRV.getPlugin().getAccountLinkManager();
        if (SyncPlugin.isLinked(uuid) && SyncPlugin.isInDiscord(uuid)) {
            Member member = DiscordUtil.getMemberById(accountManager.getDiscordId(uuid));
            return member.getAsMention();
        }
        return Bukkit.getOfflinePlayer(uuid).getName();
    }
}