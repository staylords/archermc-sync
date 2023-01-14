package me.staylords.sync.listeners;

import fun.lewisdev.coinflip.events.CoinflipCompletedEvent;
import fun.lewisdev.coinflip.events.CoinflipCreatedEvent;
import fun.lewisdev.coinflip.game.CoinflipGame;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.MessageBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.util.DiscordUtil;
import me.staylords.sync.SyncPlugin;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.jonspitfire.economyapi.types.Economy;

import java.awt.*;

public class BukkitListeners implements Listener {

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!SyncPlugin.isLinked(player.getUniqueId())) return;
        if (!SyncPlugin.isInDiscord(player.getUniqueId())) return;

        SyncPlugin.getInstance().update(player);
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!SyncPlugin.isLinked(player.getUniqueId())) return;
        if (!SyncPlugin.isInDiscord(player.getUniqueId())) return;

        SyncPlugin.getInstance().update(player);
    }

    @EventHandler
    public void onCoinflipCreatedEvent(CoinflipCreatedEvent event) {
        CoinflipGame game = event.getCoinflipGame();
        if (game == null) return;
        Economy provider = event.getCoinflipGame().getProvider();

        Button takeCoinflipButton = Button.success("take-coinflip", "Take Coinflip");
        Button createCoinflipButton = Button.primary("create-coinflip", "Create a Coinflip");
        Button checkBalanceButton = Button.primary("create-balance", "Check your balance");

        EmbedBuilder builder = new EmbedBuilder();
        builder
                .setColor(new Color(255, 65, 65))
                .setTitle(game.getOfflinePlayer().getName() + "'s coinflip")
                .addField("Hello there!", "Think luck is on your side? Face " + game.getOfflinePlayer().getName() + " in a coinflip!", false)
                .addField("Value", ChatColor.stripColor(provider.format(game.getAmount())), true)
                .addField("Currency", WordUtils.capitalize(provider.getInputName()), true)
                .setThumbnail("https://mc-heads.net/avatar/" + game.getOfflinePlayer().getName())
                .setImage("https://i.imgur.com/3dCjNA4.jpg")
                .setFooter(SyncPlugin.BOT_FOOTER, DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl());

        Message message = new MessageBuilder()
                .setEmbeds(builder.build())
                .setActionRows(ActionRow.of(takeCoinflipButton, createCoinflipButton, checkBalanceButton))
                .build();

        DiscordUtil.queueMessage(DiscordUtil.getTextChannelById(SyncPlugin.COIN_FLIP_CHANNEL), message);
    }

    @EventHandler
    public void onCoinflipCompletedEvent(CoinflipCompletedEvent event) {
        EmbedBuilder builder = new EmbedBuilder();
        builder
                .setColor(new Color(255, 65, 65))
                .setTitle(WordUtils.capitalize(event.getProvider().getInputName() + " coinflip"))
                .setAuthor("Better luck next time, " + event.getLoser().getName() + "!", "https://mc-heads.net/avatar/" + event.getLoser().getName(), "https://mc-heads.net/avatar/" + event.getLoser().getName())
                .addField("Game Summary", "**" + event.getWinner().getName() + "** has defeated **" + event.getLoser().getName() + "** in a **" + ChatColor.stripColor(event.getProvider().format(event.getWinnings())) + "** coinflip!", true)
                .setThumbnail("https://mc-heads.net/avatar/" + event.getWinner().getName())
                .setFooter(SyncPlugin.BOT_FOOTER, DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl());

        Message message = new MessageBuilder()
                .setEmbeds(builder.build())
                .build();

        DiscordUtil.queueMessage(DiscordUtil.getTextChannelById(SyncPlugin.COIN_FLIP_CHANNEL), message);
    }


}
