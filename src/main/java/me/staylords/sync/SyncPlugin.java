package me.staylords.sync;

import co.aikar.commands.BukkitCommandManager;
import fun.lewisdev.coinflip.DeluxeCoinflip;
import fun.lewisdev.coinflip.config.Messages;
import fun.lewisdev.coinflip.events.CoinflipCreatedEvent;
import fun.lewisdev.coinflip.game.CoinflipGame;
import fun.lewisdev.coinflip.player.PlayerManager;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.MessageBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.SubcommandData;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import github.scarsz.discordsrv.util.DiscordUtil;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TablistFormatManager;
import me.staylords.sync.commands.LinkCommand;
import me.staylords.sync.listeners.BukkitListeners;
import me.staylords.sync.listeners.GeneralJDAListeners;
import me.staylords.sync.task.InitializeTask;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import pl.jonspitfire.economyapi.types.Economy;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Discord account linking & coinflip
 * implementation to DiscordSRV API.
 *
 * @author staylords
 */
public class SyncPlugin extends JavaPlugin implements SlashCommandProvider {

    /**
     * Let's make sure to create two brand-new channels in the discord to utilize respectively as sync and coin flip main channels.
     **/
    private static final String SYNC_CHANNEL = "1063990513897844806";
    public static final String COIN_FLIP_CHANNEL = "1043375303420026942";
    public static final String BOT_TITLE = "ArcherMC Discord Linking";
    public static final String BOT_FOOTER = "ArcherMC Official Bot";

    private GeneralJDAListeners hook;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin("DiscordSRV") != null) {
            subscribeToDiscordSrv();
        } else {
            this.getServer().shutdown();
        }

        /*
         * Using co.aikar command framework since I have no clue what ArcherMC uses.
         */
        BukkitCommandManager manager = new BukkitCommandManager(this);
        manager.registerCommand(new LinkCommand());

        //Bukkit
        this.getServer().getPluginManager().registerEvents(new BukkitListeners(this), this);
    }

    @Override
    public void onDisable() {
        DiscordSRV.api.unsubscribe(hook);
    }

    public void subscribeToDiscordSrv() {
        /*
         * Registering all our JDA listeners through DiscordSRV API
         * # me.staylords.sync.listeners.GeneralJDAListeners
         * # ...
         */
        DiscordSRV.api.subscribe(hook = new GeneralJDAListeners(this));

        //Bukkit
        new InitializeTask(this).runTaskTimerAsynchronously(this, 20L * 10, 20L * 10);

        /*
         * DiscordSRV uses an ancient JDA API which doesn't allow to use Models or correct JDA Command framework, so
         * we double register all of our commands in order to make sure players can execute private messages commands.
         */
        /*
         * DiscordSRV uses an ancient JDA API which doesn't allow to use Models or correct JDA Command framework, so
         * we double register all of our commands in order to make sure players can execute private messages commands.
         */
        this.getSlashCommands().forEach(pluginSlashCommand -> DiscordUtil.getJda().updateCommands().addCommands(pluginSlashCommand.getCommandData()).queue());
        DiscordUtil.getJda().updateCommands().queue();
        DiscordUtil.getJda().upsertCommand(new CommandData("sync", "Link your Minecraft account to our Discord server!")
                .addOption(OptionType.INTEGER, "code", "The code you received in-game.", true)).queue();
        DiscordUtil.getJda().updateCommands().queue();
    }

    public void check() {
        TextChannel textChannel = DiscordSRV.getPlugin().getJda().getTextChannelById(SYNC_CHANNEL);
        List<MessageEmbed> embeds = new ArrayList<>();
        if (textChannel == null) return;

        MessageHistory history = MessageHistory.getHistoryFromBeginning(textChannel).complete();
        history.getRetrievedHistory().forEach(message -> {
            if (!message.getEmbeds().isEmpty()) {
                embeds.addAll(message.getEmbeds());
            }
        });

        if (embeds.isEmpty()) {
            this.initialize();
        }
    }

    public void initialize() {
        TextChannel textChannel = DiscordSRV.getPlugin().getJda().getTextChannelById(SYNC_CHANNEL);
        if (textChannel == null) return;

        textChannel.getHistory().retrievePast(100)
                .queue(messages -> messages
                        .stream()
                        .filter(m -> m != null && !m.getEmbeds().isEmpty())
                        .forEach(m -> m.getEmbeds()
                                .stream()
                                .filter(e -> e.getTitle() != null && e.getTitle().startsWith(BOT_TITLE))
                                .forEach(e -> m.delete().queue())));


        /*
         * We fully delete messages and pins, so we prevent dupes and channel will look cleaner.
         */
        MessageHistory history = MessageHistory.getHistoryFromBeginning(textChannel).complete();
        history.getRetrievedHistory().forEach(message -> {
            if (message.isPinned())
                message.unpin().queue();
            message.delete().queue();
        });

        Button syncButton = Button.success("sync-account", "Link");
        Button unsyncButton = Button.danger("unsync-account", "Unlink");

        EmbedBuilder builder = new EmbedBuilder();
        builder
                .setColor(new Color(209, 62, 75))
                .setTitle(BOT_TITLE)
                .addField("ArcherMC In-Game to Discord Integration :link:",
                        "In order to **link** your **in-game roles** and be able to access **our " +
                                    "features**, click the `Link` button and follow the **procedure**." +
                                    "\n" +
                                    " " +
                                    "\n" +
                                    "Make sure your DM's are open or either enable `'User Settings -> Privacy & Safety -> Allow direct messages from server members'`." +
                                    "\n" +
                                    " " +
                                    "\n" +
                                    "If you'd like to **unlink** your account simply click the corresponding button below.",
                            false)
                    .setThumbnail("https://media.discordapp.net/attachments/560764349690675211/976975445784420452/Archer.png?width=959&height=671")
                    .setImage("https://i.imgur.com/3dCjNA4.jpg")
                    .setFooter(BOT_FOOTER, DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl());

            Message message = new MessageBuilder()
                    .setEmbeds(builder.build())
                    .setActionRows(ActionRow.of(syncButton, unsyncButton))
                    .build();

            textChannel.sendMessage(message).queue();

            /*
             * This assumes that the channel has at least a message, so we wait until the message above is queued and sent, and we pin it again.
             */
            textChannel.getHistory().retrievePast(1)
                    .map(messages -> messages.get(0))
                    .queueAfter(3, TimeUnit.SECONDS, m -> m.pin().queue());
    }

    @SlashCommand(path = "sync")
    public void onSyncCommand(SlashCommandEvent event) {
        AccountLinkManager accountManager = DiscordSRV.getPlugin().getAccountLinkManager();
        event.deferReply().setEphemeral(true).queue();

        if (SyncPlugin.isLinked(event.getUser().getId())) {
            event.getHook().sendMessage("Looks like your account is already linked.").queue();
            return;
        }

        if (event.getChannel().getType() != ChannelType.PRIVATE) {
            event.getHook().sendMessage("You can only execute this command in private chat!").queue();
            return;
        }

        String code = Objects.requireNonNull(event.getOption("code")).getAsString();
        if (code.length() != 4 || !NumberUtils.isDigits(code) || !accountManager.getLinkingCodes().containsKey(code)) {
            event.getHook().sendMessage("You didn't insert a valid 4 digits synchronization code. Retry or execute the command again!").queue();
            return;
        }

        UUID retrievedUuid = accountManager.getLinkingCodes().get(code);

        try {
            event.getHook().sendMessage("You have provided a valid code. We're starting the verification process.").queue();

            Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
                accountManager.link(event.getUser().getId(), retrievedUuid);
                accountManager.save();
            });
        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().sendMessage("Looks like we're having some internal issues. Make a support request to a staff member as soon as possible.").queue();
        }
    }

    @SlashCommand(path = "bal")
    public void onBalanceCommand(SlashCommandEvent event) {
        User user = event.getUser();

        event.deferReply().setEphemeral(true).queue();

        if (!SyncPlugin.isLinked(user.getId())) {
            event.getHook().sendMessage("Your Minecraft account must be linked in order to execute this command.").queue();
            return;
        }

        TextChannel channel = DiscordUtil.getTextChannelById(SyncPlugin.COIN_FLIP_CHANNEL);

        if (!event.getChannel().getId().equals(channel.getId())) {
            event.getHook().sendMessage("You can only execute this command in the `#coinflip` text channel.").queue();
            return;
        }

        Economy money = SyncPlugin.getProviderByName("money");
        Economy tokens = SyncPlugin.getProviderByName("tokens");
        Economy gems = SyncPlugin.getProviderByName("gems");

        if (money == null || tokens == null || gems == null) {
            event.getHook().sendMessage("Looks like we're having some internal issues. Make a support request to a staff member as soon as possible.").queue();
            return;
        }

        UUID uuid = SyncPlugin.returnUUID(user.getId());

        event.getHook().sendMessage("Your current **balance** is: \n"
                + "– Money: `" + ChatColor.stripColor(money.format(money.getBalance(uuid))) + "`\n"
                + "– Tokens: `" + ChatColor.stripColor(tokens.format(tokens.getBalance(uuid))) + "`\n"
                + "– Gems: `" + ChatColor.stripColor(gems.format(gems.getBalance(uuid))) + "`\n").queue();
    }

    @SlashCommand(path = "cf")
    public void onCoinflipCommand(SlashCommandEvent event) {
        User user = event.getUser();

        event.deferReply().setEphemeral(true).queue();

        if (!SyncPlugin.isLinked(user.getId())) {
            event.getHook().sendMessage("Your Minecraft account must be linked in order to execute this command.").queue();
            return;
        }

        TextChannel channel = DiscordUtil.getTextChannelById(SyncPlugin.COIN_FLIP_CHANNEL);

        if (!event.getChannel().getId().equals(channel.getId())) {
            event.getHook().sendMessage("You can only execute this command in the `#coinflip` text channel.").queue();
            return;
        }

        long wager = NumberUtils.createLong(Objects.requireNonNull(event.getOption("wager")).getAsString());
        String currency = Objects.requireNonNull(event.getOption("currency")).getAsString();

        Economy provider = getProviderByName(currency);
        if (provider == null) {
            event.getHook().sendMessage("This currency does not exist, make sure to insert a value between `[money, tokens, gems]`").queue();
            return;
        }

        String playerName = returnFancyName(user.getId());
        UUID uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        PlayerManager playerManager = DeluxeCoinflip.getInstance().getPlayerManager();

        if (playerManager.getCurrentGames().containsKey(uuid)) {
            event.getHook().sendMessage("You already have an active coinflip.").queue();
            return;
        }

        if (provider.getBalance(returnFancyName(user.getId())) < wager) {
            event.getHook().sendMessage("Sorry **" + playerName + "** but you do not have **enough " + provider.getInputName() + "** to create this coinflip!\n" +
                    "Your current balance is: **" + ChatColor.stripColor(provider.format(provider.getBalance(playerName))) + "**").queue();
            return;
        }

        //After we pass every check, we create CoinflipGame
        CoinflipGame game = new CoinflipGame(uuid, new ItemStack(Material.SKULL_ITEM), provider, wager);

        provider.withdraw(uuid, wager);
        playerManager.addCoinflipGame(uuid, game);
        Bukkit.getPluginManager().callEvent(new CoinflipCreatedEvent(Bukkit.getOfflinePlayer(uuid), game));

        event.getHook().sendMessage("You successfully created a " + ChatColor.stripColor(provider.format(wager)) + " coinflip!").queue();
    }

    @SlashCommand(path = "cfcancel")
    public void onCoinflipCancelCommand(SlashCommandEvent event) {
        User user = event.getUser();

        event.deferReply().setEphemeral(true).queue();

        if (!SyncPlugin.isLinked(user.getId())) {
            event.getHook().sendMessage("Your Minecraft account must be linked in order to execute this command.").queue();
            return;
        }

        TextChannel channel = DiscordUtil.getTextChannelById(SyncPlugin.COIN_FLIP_CHANNEL);

        if (!event.getChannel().getId().equals(channel.getId())) {
            event.getHook().sendMessage("You can only execute this command in the `#coinflip` text channel.").queue();
            return;
        }

        String playerName = returnFancyName(user.getId());
        UUID uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        PlayerManager playerManager = DeluxeCoinflip.getInstance().getPlayerManager();

        //We found a coinflip game, so we delete it here now.
        if (playerManager.getCurrentGames().containsKey(uuid)) {
            CoinflipGame game = playerManager.getCurrentGames().get(uuid);
            game.getProvider().deposit(uuid, (double) game.getAmount());
            playerManager.getCurrentGames().remove(uuid);

            event.getHook().sendMessage("You have successfully cancelled your ongoing coinflip game.").queue();

            channel.getHistory().retrievePast(100)
                    .queueAfter(1, TimeUnit.SECONDS, messages -> messages
                            .stream()
                            .filter(m -> m != null && !m.getEmbeds().isEmpty())
                            .forEach(m -> m.getEmbeds()
                                    .stream()
                                    .filter(e -> e.getTitle() != null && e.getTitle().startsWith(playerName))
                                    .forEach(e -> m.delete().queue())));
        } else {
            event.getHook().sendMessage("You do not have an active coinflip game.").queue();
        }
    }

    @SlashCommand(path = "whois")
    public void onWhoIsCommand(SlashCommandEvent event) {
        User user = event.getUser();

        event.deferReply().setEphemeral(true).queue();

        User target = Objects.requireNonNull(event.getOption("user")).getAsUser();
        if (!SyncPlugin.isLinked(target.getId())) {
            event.getHook().sendMessage(target.getName() + "'s Minecraft account must be linked in order to execute this command.").queue();
            return;
        }

        String targetName = returnFancyName(target.getId());

        event.getHook().sendMessage(target.getName() + "'s Minecraft account is: " + targetName).queue();
    }

    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        return new HashSet<>(Arrays.asList(
                new PluginSlashCommand(this, new CommandData("sync", "Link your Minecraft account to our Discord server!")
                        .addOption(OptionType.INTEGER, "code", "The code you received in-game.", true)),

                new PluginSlashCommand(this, new CommandData("bal", "Check your in-game balance!")),

                new PluginSlashCommand(this, new CommandData("whois", "Finds a user ign by their Discord!")
                        .addOption(OptionType.USER, "user", "Discord username")),

                new PluginSlashCommand(this, new CommandData("cfcancel", "Cancels an ongoing coinflip.")),

                new PluginSlashCommand(this, new CommandData("cf", "Challenge your luck betting in-game currency through Discord!")
                        .addOption(OptionType.INTEGER, "wager", "How much do you want to bet?", true)
                        .addOption(OptionType.STRING, "currency", "Choose between money, tokens or gems!", true))
        ));
    }

    /**
     * Quickly recover synchronized players information from Player UUID
     */
    public static boolean isLinked(UUID uuid) {
        return DiscordSRV.getPlugin().getAccountLinkManager().getLinkedAccounts().containsValue(uuid);
    }

    /**
     * Quickly recover synchronized players information from Discord User ID
     */
    public static boolean isLinked(String userId) {
        return DiscordSRV.getPlugin().getAccountLinkManager().getLinkedAccounts().containsKey(userId);
    }

    /**
     * Quick check to see if a Player is still in our Discord from Player UUID
     */
    public static boolean isInDiscord(UUID uuid) {
        return DiscordUtil.getUserById(DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(uuid)) != null;
    }

    /**
     * @see DeluxeCoinflip#getEconomyProviders()
     **/
    public static Economy getProviderByName(String name) {
        for (Economy provider : DeluxeCoinflip.getInstance().getEconomyProviders()) {
            if (!name.equalsIgnoreCase(provider.getInputName())) continue;
            return provider;
        }
        return null;
    }

    /**
     * @return Clean Minecraft name
     **/
    public static String returnFancyName(String userId) {
        AccountLinkManager accountManager = DiscordSRV.getPlugin().getAccountLinkManager();
        return Bukkit.getOfflinePlayer(accountManager.getLinkedAccounts().get(userId)).getName();
    }

    /**
     * @return Minecraft UUID
     **/
    public static UUID returnUUID(String userId) {
        AccountLinkManager accountManager = DiscordSRV.getPlugin().getAccountLinkManager();
        return accountManager.getLinkedAccounts().get(userId);
    }

    /**
     * Updates Discord name and adds verified checkmark in TAB
     **/
    public void update(Player player) {
        AccountLinkManager accountManager = DiscordSRV.getPlugin().getAccountLinkManager();
        Member member = DiscordUtil.getMemberById(accountManager.getDiscordId(player.getUniqueId()));
        TablistFormatManager tablistFormatManager = TabAPI.getInstance().getTablistFormatManager();

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            DiscordUtil.setNickname(member, player.getName());

            if (player.isOnline()) {
                tablistFormatManager.setSuffix(TabAPI.getInstance().getPlayer(player.getUniqueId()), " §a§l✓");
            }
        });
    }
}