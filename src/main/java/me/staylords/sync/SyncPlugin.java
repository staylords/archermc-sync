package me.staylords.sync;

import co.aikar.commands.BukkitCommandManager;
import fun.lewisdev.coinflip.DeluxeCoinflip;
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
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import github.scarsz.discordsrv.objects.managers.GroupSynchronizationManager;
import github.scarsz.discordsrv.util.DiscordUtil;
import lombok.Getter;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TablistFormatManager;
import me.staylords.sync.commands.LinkCommand;
import me.staylords.sync.listeners.BukkitListeners;
import me.staylords.sync.listeners.GeneralJDAListeners;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import pl.jonspitfire.economyapi.types.Economy;

import java.awt.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SyncPlugin extends JavaPlugin implements SlashCommandProvider {
    @Getter
    private static SyncPlugin instance;

    /**
     * Let's make sure to create two brand-new channels in the discord to utilize respectively as sync and coin flip main channels.
     **/
    private final static String SYNC_CHANNEL = "1062919124348575764";
    public final static String COIN_FLIP_CHANNEL = "1063540284127973426";
    public final static String BOT_TITLE = ":archermc: ArcherMC Synchronization :archermc:";
    public final static String BOT_FOOTER = "ArcherMC Official Bot";

    @Override
    public void onEnable() {
        instance = this;

        /*
        Registering all our JDA listeners through DiscordSRV API
        # me.staylords.sync.listeners.GeneralJDAListeners
        # ...
         */
        DiscordSRV.api.subscribe(new GeneralJDAListeners());

        /*
        Using co.aikar command framework since I have no clue what ArcherMC uses.
         */
        BukkitCommandManager manager = new BukkitCommandManager(this);
        manager.registerCommand(new LinkCommand());

        this.getServer().getPluginManager().registerEvents(new BukkitListeners(), this);
    }

    public void initialize() {
        TextChannel textChannel = DiscordUtil.getJda().getTextChannelById(SYNC_CHANNEL);

        if (textChannel != null) {
            /*
            We fully delete messages and pins, so we prevent dupes and channel will look cleaner.
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
                    .setColor(new Color(255, 65, 65))
                    .setTitle(BOT_TITLE)
                    .addField("Hello there!",
                            "In order to **synchronize** your **in-game roles** and be able to access **our " +
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
            This assumes that the channel has at least a message, so we wait until the message above is queued and sent, and we pin it again.
             */
            textChannel.getHistory().retrievePast(1)
                    .map(messages -> messages.get(0))
                    .queueAfter(3, TimeUnit.SECONDS, m -> m.pin().queue());
        } else {
            this.getLogger().warning("Channel cannot be found.");
        }
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

        Economy provider = this.getProviderByName(currency);
        if (provider == null) {
            event.getHook().sendMessage("This currency does not exist, make sure to insert a value between `[money, tokens, gems]`.").queue();
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
            event.getHook().sendMessage("Sorry **" + playerName + "** but you **do not** have **enough " + provider.getInputName() + "** to create this coinflip!\n" +
                    "Your current balance is: **" + ChatColor.stripColor(provider.format(provider.getBalance(playerName))) + "**").queue();
            return;
        }

        //After we pass every check, we create CoinflipGame
        CoinflipGame game = new CoinflipGame(uuid, new ItemStack(Material.SKULL_ITEM), provider, wager);

        provider.withdraw(playerName, wager);
        playerManager.addCoinflipGame(uuid, game);
        Bukkit.getPluginManager().callEvent(new CoinflipCreatedEvent(Bukkit.getOfflinePlayer(uuid), game));

        event.getHook().sendMessage("You successfully created a " + ChatColor.stripColor(provider.format(wager)) + " coinflip!").queue();
    }

    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        return new HashSet<>(Arrays.asList(
                new PluginSlashCommand(this, new CommandData("sync", "Link your Minecraft account to our Discord server!")
                        .addOption(OptionType.INTEGER, "code", "The code you received in-game.", true)),

                new PluginSlashCommand(this, new CommandData("cf", "Challenge your luck betting in-game currency through Discord!")
                        .addOption(OptionType.INTEGER, "wager", "How much you want to bet?", true)
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
     * Method from Lfun/lewisdev/coinflip/command/CoinflipCommand;getProviderByName(Ljava/lang/String;)Lpl/jonspitfire/economyapi/types/Economy;
     **/
    private Economy getProviderByName(String name) {
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
     * Updates Discord name and adds verified checkmark in TAB
     **/
    public void update(Player player) {
        AccountLinkManager accountManager = DiscordSRV.getPlugin().getAccountLinkManager();
        GroupSynchronizationManager synchronizationManager = DiscordSRV.getPlugin().getGroupSynchronizationManager();
        Member member = DiscordUtil.getMemberById(accountManager.getDiscordId(player.getUniqueId()));
        TablistFormatManager tablistFormatManager = TabAPI.getInstance().getTablistFormatManager();

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            DiscordUtil.setNickname(member,
                    "[" +
                            ChatColor.stripColor(synchronizationManager.getPermissions().getPrimaryGroup(player)) +
                            "] " +
                            player.getName());

            if (player.isOnline()) {
                tablistFormatManager.setSuffix(TabAPI.getInstance().getPlayer(player.getUniqueId()), " §a§l✓");
            }
        });
    }
}
