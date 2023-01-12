package me.staylords.sync.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import github.scarsz.discordsrv.api.events.AccountUnlinkedEvent;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import github.scarsz.discordsrv.util.DiscordUtil;
import me.staylords.sync.SyncPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;

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

        EmbedBuilder builder = new EmbedBuilder();
        builder
                .setColor(new Color(106, 255, 35))
                .setTitle(SyncPlugin.BOT_TITLE)
                .addField("Thank you for successfully linking your Minecraft account to the ArcherMC Discord!",
                        "You have completed the verification process and synced your account. " + "(" + user.getAsTag() + " -> " + event.getPlayer().getName() + ")"
                                + "\n", false)
                .setThumbnail("https://mc-heads.net/avatar/" + event.getPlayer().getName())
                .setImage("https://i.imgur.com/3dCjNA4.jpg")
                .setFooter(SyncPlugin.BOT_FOOTER, DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl());

        event.getUser().openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(builder.build()))
                .queue();

        String[] messages = {
                "",
                "§9§m------------------------------------",
                "§aThank you for successfully linking your Minecraft account to the ArcherMC Discord!",
                "",
                "§eThis is what we got for you, §e§l" + event.getPlayer().getName() + "§e.",
                "§7- §e(§c" + event.getPlayer().getName() + " §d-> " + user.getAsTag() + "§e)",
                "",
                " §5§l[§dVerified Role on Discord§5, §dVerified Tablist Suffix§5, §dDiscord rank synchronization§5, §dIn-game coin flips through Discord§5... §dand much more coming real soon!§5§l]",
                "§9§m------------------------------------"
        };

        if (event.getPlayer() instanceof Player) {
            ((Player) event.getPlayer()).sendTitle("§c§lArcher§4§lMC §c§lVerified", "§bPress §b§lTAB §bto see your brand new §a§l✓ §bsuffix!", 60, 60, 60);
            ((Player) event.getPlayer()).sendMessage(messages);
        }
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

        switch (event.getButton().getId()) {
            case "sync-account":
                if (SyncPlugin.isLinked(event.getUser().getId())) {
                    event.deferReply().setEphemeral(true).queue();
                    event.getHook().sendMessageEmbeds(new EmbedBuilder()
                            .setColor(new Color(255, 65, 65))
                            .setTitle(SyncPlugin.BOT_TITLE)
                            .addField("Hello " + event.getUser().getName() + "!",
                                    "Your discord account is already linked to " + returnFancyName(event.getUser().getId()) +
                                            "\n" +
                                            "" +
                                            "\n" +
                                            "If you wish to unlink it simply click the `Unlink` button on the pinned message!", false)
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
