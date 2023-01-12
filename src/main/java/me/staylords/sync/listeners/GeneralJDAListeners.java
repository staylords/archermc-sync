package me.staylords.sync.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import github.scarsz.discordsrv.api.events.AccountUnlinkedEvent;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import github.scarsz.discordsrv.util.DiscordUtil;
import me.staylords.sync.SyncPlugin;
import org.bukkit.Bukkit;

import java.awt.*;

public class GeneralJDAListeners extends ListenerAdapter {

    @Subscribe
    public void onDiscordReadyEvent(DiscordReadyEvent event) {
        /*
        We must do it here in order to make sure DiscordSRV loads all discord information before SyncPlugin.java does.
         */
        SyncPlugin.getInstance().initialize();
        DiscordUtil.getJda().addEventListener(this);

        /*
        DiscordSRV uses an ancient JDA API which doesn't allow to use Models or correct JDA Command framework, so we double register all our commands in order to make sure players can execute private messages commands.
         */
        SyncPlugin.getInstance().getSlashCommands().forEach(pluginSlashCommand -> DiscordUtil.getJda().updateCommands().addCommands(pluginSlashCommand.getCommandData()).queue());
    }

    @Subscribe
    public void onAccountLinkedEvent(AccountLinkedEvent event) {
        System.out.println("aaaaaaaaaaaaaaaaaaaaaaaa");
    }

    @Subscribe
    public void onAccountUnlinkedEvent(AccountUnlinkedEvent event) {
        System.out.println("bbbbbbbbbbbbbbbbbbbbbbbbb");
    }

    @Subscribe
    public void onButtonClick(ButtonClickEvent event) {
        AccountLinkManager accountManager = DiscordSRV.getPlugin().getAccountLinkManager();

        if (event.getButton() == null) return;
        if (event.getButton().getId() == null) return;

        String response = "";
        System.out.println(accountManager.getLinkingCodes().toString());

        switch (event.getButton().getId()) {
            case "sync-account":
                if (SyncPlugin.isSynchronized(event.getUser().getId())) {
                    event.deferReply().setEphemeral(true).queue();
                    event.getHook().sendMessageEmbeds(new EmbedBuilder()
                            .setColor(new Color(255, 65, 65))
                            .setTitle(SyncPlugin.BOT_TITLE)
                            .addField("Hello " + event.getUser().getName() + "!",
                                    "Your discord account is already linked with " + returnFancyName(event.getUser().getId()) +
                                            "\n" +
                                            "" +
                                            "\n" +
                                            "If you wish to un-link it simply click the `Unsync` button on the pinned message!", false)
                            .setFooter(SyncPlugin.BOT_FOOTER, DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl())
                            .build()).queue();
                    return;
                }

                EmbedBuilder builder = new EmbedBuilder();
                builder
                        .setColor(new Color(255, 65, 65))
                        .setTitle(SyncPlugin.BOT_TITLE)
                        .addField("Hello " + event.getUser().getName() + "!",
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
                break;
            case "unsync-account":
                event.deferReply().setEphemeral(true).queue();
                event.getHook().sendMessage("Your account is not synced.").queue();
                break;
            default:
                break;
        }
    }

    private String returnFancyName(String userId) {
        AccountLinkManager accountManager = DiscordSRV.getPlugin().getAccountLinkManager();
        return Bukkit.getOfflinePlayer(accountManager.getLinkedAccounts().get(userId)).getName();
    }

}
