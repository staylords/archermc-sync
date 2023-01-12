package me.staylords.sync;

import co.aikar.commands.BukkitCommandManager;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.MessageBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageHistory;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import github.scarsz.discordsrv.util.DiscordUtil;
import lombok.Getter;
import me.staylords.sync.commands.AdminCommand;
import me.staylords.sync.commands.LinkCommand;
import me.staylords.sync.listeners.GeneralJDAListeners;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SyncPlugin extends JavaPlugin implements SlashCommandProvider {
    @Getter
    private static SyncPlugin instance;

    /*
    Let's make sure to create two brand-new channels in the discord to utilize respectively as sync and coin flip main channels.
     */
    private final static String SYNC_CHANNEL = "1062919124348575764";
    private final static String COIN_FLIP_CHANNEL = "";
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
        Arrays.asList(
                new LinkCommand(),
                new AdminCommand()
        ).forEach(manager::registerCommand);
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

            Button syncButton = Button.success("sync-account", "Sync");
            Button unsyncButton = Button.danger("unsync-account", "Unsync");

            EmbedBuilder builder = new EmbedBuilder();
            builder
                    .setColor(new Color(255, 65, 65))
                    .setTitle(BOT_TITLE)
                    .addField("Hello there!",
                            "In order to **synchronize** your **in-game roles** and be able to access **our " +
                                    "features**, click the `Sync` button and follow the **procedure**." +
                                    "\n" +
                                    " " +
                                    "\n" +
                                    "Make sure your DM's are open or either enable `'User Settings -> Privacy & Safety -> Allow direct messages from server members'`." +
                                    "\n" +
                                    " " +
                                    "\n" +
                                    "If you'd like to **unsync** your account simply click the corresponding button below.",
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

    /**
     * Quickly recover synchronized players information from java.util.UUID
     */
    public static boolean isSynchronized(UUID uuid) {
        return DiscordSRV.getPlugin().getAccountLinkManager().getLinkedAccounts().containsValue(uuid);
    }

    /**
     * Quickly recover synchronized players information from discord user id
     */
    public static boolean isSynchronized(String userId) {
        return DiscordSRV.getPlugin().getAccountLinkManager().getLinkedAccounts().containsKey(userId);
    }

    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        return new HashSet<>(Collections.singletonList(
                new PluginSlashCommand(this, new CommandData("sync", "Link your Minecraft account to our Discord server!")
                        .addOption(OptionType.INTEGER, "code", "The code you received in-game.", true))
        ));
    }

    @SlashCommand(path = "sync")
    public void onSyncCommand(SlashCommandEvent event) {
        AccountLinkManager accountManager = DiscordSRV.getPlugin().getAccountLinkManager();

        if (event.getOption("code") == null || Objects.requireNonNull(event.getOption("code")).getAsString().chars().count() != 4) {
            event.deferReply().setEphemeral(true).queue();
            event.getHook().sendMessage("Looks like you didn't insert a valid 4 digits synchronization code. Retry or execute the command again!").queue();
            return;
        }

        int code = Integer.parseInt(Objects.requireNonNull(event.getOption("code")).getAsString());
        try {
            accountManager.link(event.getUser().getId(), accountManager.getLinkingCodes().get(String.valueOf(code)));
            accountManager.save();
            OfflinePlayer player = Bukkit.getOfflinePlayer(accountManager.getUuid(event.getUser().getId()));
            ((Player) player).sendMessage("u got verified test?");
        } catch (Exception e) {
            e.printStackTrace();
        }

        event.reply(String.valueOf(code)).queue();
    }
}
