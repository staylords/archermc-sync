package me.staylords.sync;

import co.aikar.commands.BukkitCommandManager;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.MessageBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageHistory;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.util.DiscordUtil;
import lombok.Getter;
import me.staylords.sync.commands.CancelCommand;
import me.staylords.sync.commands.GeneralJDACommands;
import me.staylords.sync.commands.SyncCommand;
import me.staylords.sync.listeners.GeneralJDAListeners;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.util.Arrays;

public class SyncPlugin extends JavaPlugin {
    @Getter
    private static SyncPlugin instance;

    /*
    Let's make sure to create a brand-new channel in the discord to utilize as main sync channel.
     */
    private final static String SYNC_CHANNEL = "1062919124348575764";

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
                new SyncCommand(),
                new CancelCommand()
        ).forEach(manager::registerCommand);

        /*
        Initialize splash commands class.
         */
        new GeneralJDACommands(this);
    }

    public void initialize() {
        TextChannel textChannel = DiscordUtil.getJda().getTextChannelById(SYNC_CHANNEL);

        if (textChannel != null) {
            /*
            We run a full clear, so we prevent dupes.
             */
            MessageHistory history = MessageHistory.getHistoryFromBeginning(textChannel).complete();
            history.getRetrievedHistory().forEach(message -> message.delete().queue());

            Button syncButton = Button.success("sync-account", "Sync");
            Button unsyncButton = Button.danger("unsync-account", "Unsync");

            EmbedBuilder builder = new EmbedBuilder();
            builder
                    .setColor(new Color(255, 65, 65))
                    .setTitle(":archermc: ArcherMC Official Synchronization Process :archermc:")
                    .addField("Hello there!",
                            "In order to **synchronize** your **in-game roles** and be able to access **our features**, click" +
                                    "\n" +
                                    "the `Sync` button and follow the **procedure**." +
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
                    .setFooter("ArcherMC", DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl());

            Message message = new MessageBuilder()
                    .setEmbeds(builder.build())
                    .setActionRows(ActionRow.of(syncButton, unsyncButton))
                    .build();

            textChannel.sendMessage(message).queue();
        } else {
            this.getLogger().warning("Channel cannot be found.");
        }

    }

}
