package me.staylords.sync.listeners;

import fun.lewisdev.coinflip.DeluxeCoinflip;
import fun.lewisdev.coinflip.config.Messages;
import fun.lewisdev.coinflip.events.CoinflipCompletedEvent;
import fun.lewisdev.coinflip.game.CoinflipGame;
import fun.lewisdev.coinflip.menu.inventories.CoinflipGUI;
import fun.lewisdev.coinflip.player.PlayerManager;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import github.scarsz.discordsrv.api.events.AccountUnlinkedEvent;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.MessageBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import github.scarsz.discordsrv.objects.managers.GroupSynchronizationManager;
import github.scarsz.discordsrv.util.DiscordUtil;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TablistFormatManager;
import me.staylords.sync.SyncPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import pl.jonspitfire.economyapi.types.Economy;

import java.awt.*;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class GeneralJDAListeners extends ListenerAdapter {

    @Subscribe
    public void onDiscordReadyEvent(DiscordReadyEvent event) {
        /*
        DiscordSRV needs to fully load its information before we can initialize
        our basic project functions, so after waiting patiently we go for it.
         */
        SyncPlugin.getInstance().initialize();
        DiscordUtil.getJda().addEventListener(this);

        /*
        DiscordSRV uses an ancient JDA API which doesn't allow to use Models or correct JDA Command framework, so
        we double register all of our commands in order to make sure players can execute private messages commands.
         */
        SyncPlugin.getInstance().getSlashCommands().forEach(pluginSlashCommand -> DiscordUtil.getJda().updateCommands().addCommands(pluginSlashCommand.getCommandData()).queue());
    }

    @Subscribe
    public void onAccountLinkedEvent(AccountLinkedEvent event) {
        User user = event.getUser();
        if (user == null) return;

        EmbedBuilder builder = new EmbedBuilder();
        builder
                .setColor(new Color(106, 255, 35))
                .setTitle(SyncPlugin.BOT_TITLE)
                .addField("Thank you for successfully linking your Minecraft account to the ArcherMC Discord!",
                        "You have completed the verification process and synced your account."
                                + "\n", false)
                .setThumbnail("https://mc-heads.net/avatar/" + event.getPlayer().getName())
                .setImage("https://i.imgur.com/3dCjNA4.jpg")
                .setFooter(SyncPlugin.BOT_FOOTER, DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl());

        user.openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(builder.build()))
                .queue();

        String[] messages = {
                "",
                "§9§m------------------------------------",
                "§aThank you for successfully linking your Minecraft account to the ArcherMC Discord!",
                "§7- §e(§c" + event.getPlayer().getName() + " §d-> " + user.getAsTag() + "§e)",
                "",
                "§eThis is what we got for you, §e§l" + event.getPlayer().getName() + "§e:",
                " §5[§dVerified Role on Discord§7, §dVerified Tablist Suffix§7, §dAutomatic rank synchronization§7, §dIn-game coin flips through Discord... and much more coming real soon!§5]",
                "§9§m------------------------------------"
        };

        if (event.getPlayer() instanceof Player) {
            Player player = event.getPlayer().getPlayer();

            SyncPlugin.getInstance().update(player);

            player.sendTitle("§a§lAccount linked!", "§bPress §b§lTAB §bto see your brand new §a§l✓ §bsuffix!", 60, 60, 60);
            player.sendMessage(messages);
        }
    }

    @Subscribe
    public void onAccountUnlinkedEvent(AccountUnlinkedEvent event) {
        User user = event.getDiscordUser();
        if (user == null) return;

        EmbedBuilder builder = new EmbedBuilder();
        builder
                .setColor(new Color(255, 211, 35))
                .setTitle(SyncPlugin.BOT_TITLE)
                .addField("Your account has been successfully unlinked.",
                        "If you'd like to **link** it again, account simply click the `Link` button in the `#sync` channel."
                                + "\n", false)
                .setThumbnail("https://mc-heads.net/avatar/" + event.getPlayer().getName())
                .setImage("https://i.imgur.com/3dCjNA4.jpg")
                .setFooter(SyncPlugin.BOT_FOOTER, DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl());

        user.openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(builder.build()))
                .queue();

        GroupSynchronizationManager synchronizationManager = DiscordSRV.getPlugin().getGroupSynchronizationManager();

        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            synchronizationManager.removeSynchronizedRoles(event.getPlayer());
            DiscordUtil.setNickname(DiscordUtil.getMemberById(user.getId()), event.getDiscordUser().getName());

            if (event.getPlayer() instanceof Player) {
                TablistFormatManager tablistFormatManager = TabAPI.getInstance().getTablistFormatManager();

                tablistFormatManager.resetSuffix(TabAPI.getInstance().getPlayer(event.getPlayer().getUniqueId()));
            }
        });

        String[] messages = {
                "§9§m------------------------------------",
                "§cYour account has been successfully unlinked.",
                "§9§m------------------------------------"
        };

        if (event.getPlayer() instanceof Player) {
            Player player = event.getPlayer().getPlayer();

            player.sendMessage(messages);
        }
    }

    @Subscribe
    public void onButtonClick(ButtonClickEvent event) {
        if (event.getButton() == null) return;
        if (event.getButton().getId() == null) return;
        User user = event.getUser();

        event.deferReply().setEphemeral(true).queue();

        /*
        Coinflip integrations
         */
        if (event.getButton().getId().equalsIgnoreCase("take-coinflip")) {
            if (!SyncPlugin.isLinked(user.getId())) {
                event.getHook().sendMessage("Looks like your Discord account is not linked to any Minecraft account. If you wish to play a coinflip game through discord make sure to check the `#sync` channel.").queue();
                return;
            }

            PlayerManager playerManager = DeluxeCoinflip.getInstance().getPlayerManager();

            AtomicReference<String> coinflipAuthor = new AtomicReference<>();
            event.getMessage().getEmbeds().forEach(messageEmbed -> coinflipAuthor.set(Objects.requireNonNull(messageEmbed.getTitle())
                    .replace("'s coinflip", "")));

            OfflinePlayer author = Bukkit.getOfflinePlayer(coinflipAuthor.get());

            if (playerManager.getCurrentGames().containsKey(author.getUniqueId())) {
                /*
                Coinflip from @author exists, so we run coinflip function.
                 */
                CoinflipGame game = playerManager.getCurrentGames().get(author.getUniqueId());
                OfflinePlayer player = Bukkit.getOfflinePlayer(SyncPlugin.returnFancyName(user.getId()));

                if (player.getUniqueId() == author.getUniqueId()) {
                    event.getHook().sendMessage("You can't play against yourself!").queue();
                    return;
                }

                Economy provider = game.getProvider();

                if (provider.getBalance(player.getUniqueId()) < game.getAmount()) {
                    event.getHook().sendMessage("Sorry **" + player.getName() + "** but you **do not** have **enough " + provider.getInputName() + "** to take this coinflip!\n" +
                            "Your current balance is: **" + ChatColor.stripColor(provider.format(provider.getBalance(player))) + "**").queue();
                    return;
                }

                //If every check is passed, we execute coinflip and play the game.
                event.getHook().sendMessage("You challenged " + author.getName() + " in a " + ChatColor.stripColor(provider.format(game.getAmount())) + " coinflip.").queue();

                provider.withdraw(player, game.getAmount());
                playerManager.removeCoinFlip(author.getUniqueId());

                if (author.isOnline()) {
                    author.getPlayer().sendMessage(Messages.PLAYER_CHALLENGE.toString().replace("{OPPONENT}", player.getName()));
                }

                OfflinePlayer winner = new Random().nextInt(2) == 0 ? player : author;
                OfflinePlayer loser = winner == player ? author : player;

                Bukkit.getScheduler().runTask(SyncPlugin.getInstance(), () -> {
                    long winAmount = game.getAmount() * 2;

                    double taxRate = DeluxeCoinflip.getInstance().getConfig().getDouble("settings.tax.rate");
                    long taxed = (long) (taxRate * winAmount / 100.0);
                    winAmount -= taxed;

                    Bukkit.getPluginManager().callEvent(new CoinflipCompletedEvent(winner, loser, winAmount, provider, author));
                    provider.deposit(winner.getUniqueId(), winAmount);

                    playerManager.getPlayer(winner.getUniqueId()).updateWins();
                    playerManager.getPlayer(winner.getUniqueId()).updateProfit(winAmount);
                    playerManager.getPlayer(loser.getUniqueId()).updateLosses();

                    if (winner.isOnline()) {
                        winner.getPlayer().sendMessage(CoinflipGUI.replacePlaceholders(Messages.GAME_SUMMARY_WIN.toString(), String.valueOf(taxRate), provider.format(taxed), winner.getName(), loser.getName(), provider.getDisplay(), provider.getInputName(), provider.format(winAmount)));
                    }
                    if (loser.isOnline()) {
                        loser.getPlayer().sendMessage(CoinflipGUI.replacePlaceholders(Messages.GAME_SUMMARY_LOSS.toString(), String.valueOf(taxRate), provider.format(taxed), winner.getName(), loser.getName(), provider.getDisplay(), provider.getInputName(), provider.format(winAmount)));
                    }
                });

                event.getMessage().delete().queue();
            } else {
                event.getHook().sendMessage("This coinflip is not available anymore.").queue();
                event.getMessage().delete().queue();
            }

            return;
        }

        if (event.getButton().getId().equalsIgnoreCase("create-coinflip")) {
            if (!SyncPlugin.isLinked(user.getId())) {
                event.getHook().sendMessage("Looks like your Discord account is not linked to any Minecraft account. If you wish to create a coinflip game through discord make sure to check the `#sync` channel.").queue();
                return;
            }

            event.getHook().sendMessage("Hello there! To create a coinflip execute `/cf [amount] [money/tokens/gems]`.").queue();
            return;
        }

        if (event.getButton().getId().equalsIgnoreCase("check-balance")) {
            if (!SyncPlugin.isLinked(user.getId())) {
                event.getHook().sendMessage("Looks like your Discord account is not linked to any Minecraft account. If you wish to check your in-game balance through discord make sure to check the `#sync` channel.").queue();
                return;
            }

            event.getHook().sendMessage("Hello there! To check your in-balance execute `/bal`.").queue();
            return;
        }

        /*
        Sync integration
         */
        AccountLinkManager accountManager = DiscordSRV.getPlugin().getAccountLinkManager();

        if (event.getButton().getId().equalsIgnoreCase("sync-account")) {
            if (SyncPlugin.isLinked(user.getId())) {
                event.getHook().sendMessageEmbeds(new EmbedBuilder()
                        .setColor(new Color(255, 65, 65))
                        .setTitle(SyncPlugin.BOT_TITLE)
                        .addField("Hello " + event.getUser().getName() + "!",
                                "Your discord account is already linked to " + SyncPlugin.returnFancyName(event.getUser().getId()) +
                                        "\n" +
                                        "" +
                                        "\n" +
                                        "If you wish to unlink it simply click the `Unlink` button on the pinned message!", false)
                        .setFooter(SyncPlugin.BOT_FOOTER, DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl())
                        .build()).queue();
                return;
            }

            event.getHook().sendMessage("We've sent you a message! Continue the verification process in our private chat.").queue();

            EmbedBuilder builder = new EmbedBuilder();
            builder
                    .setColor(new Color(255, 65, 65))
                    .setTitle(SyncPlugin.BOT_TITLE)
                    .addField("Hello " + user.getName() + "!",
                            "Connect to our server via `archermc.net` and run the command `/link`." +
                                    "\n" +
                                    "" +
                                    "\n" +
                                    "Once you have the **4 digits code** we generated for you **in-game**, please link your **account** by executing `/sync [****]` in this chat and you'll be **good to go**! :smile:",
                            false)
                    .setThumbnail(event.getUser().getEffectiveAvatarUrl())
                    .setImage("https://i.imgur.com/3dCjNA4.jpg")
                    .setFooter(SyncPlugin.BOT_FOOTER, DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl());

            event.getUser().openPrivateChannel()
                    .flatMap(channel -> channel.sendMessageEmbeds(builder.build()))
                    .queue();
        }

        if (event.getButton().getId().equalsIgnoreCase("unsync-account")) {
            if (!SyncPlugin.isLinked(user.getId())) {
                event.getHook().sendMessage("Looks like your Discord account is not linked to any Minecraft account.").queue();
                return;
            }

            Button confirmButton = Button.success("confirm-unsync-process", "Yes, I want to unlink it.");
            Button cancelButton = Button.danger("cancel-unsync-process", "No, I want to cancel the process.");

            EmbedBuilder builder = new EmbedBuilder();
            builder
                    .setColor(new Color(255, 65, 65))
                    .setTitle(SyncPlugin.BOT_TITLE)
                    .addField("Hello " + SyncPlugin.returnFancyName(user.getId()) + "!\n",
                            "In order to **unlink** your account, select an option down below.\nAre you really sure you want to do it?", false)
                    .setThumbnail("https://mc-heads.net/avatar/" + SyncPlugin.returnFancyName(event.getUser().getId()))
                    .setImage("https://i.imgur.com/3dCjNA4.jpg")
                    .setFooter(SyncPlugin.BOT_FOOTER, DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl());

            Message message = new MessageBuilder()
                    .setEmbeds(builder.build())
                    .setActionRows(ActionRow.of(confirmButton, cancelButton))
                    .build();

            event.getHook().sendMessage(message).queue();
        }

        switch (event.getButton().getId()) {
            case "confirm-unsync-process":
                if (!SyncPlugin.isLinked(user.getId())) {
                    event.getHook().sendMessage("Looks like your Discord account is not linked to any Minecraft account.").queue();
                    return;
                }
                Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
                    accountManager.unlink(user.getId());
                    accountManager.save();
                });
                event.getHook().editOriginal("You have successfully unlinked your account.").queue();
                break;
            case "cancel-unsync-process":
                if (!SyncPlugin.isLinked(user.getId())) {
                    event.getHook().sendMessage("Looks like your Discord account is not linked to any Minecraft account.").queue();
                    return;
                }
                event.getHook().editOriginal("You have cancelled the unlinking process.").queue();
                break;
            default:
                break;
        }
    }

}
