package me.staylords.sync.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import github.scarsz.discordsrv.util.DiscordUtil;
import me.staylords.sync.SyncPlugin;

public class GeneralJDAListeners extends ListenerAdapter {

    @Subscribe
    public void onDiscordReadyEvent(DiscordReadyEvent event) {
        /*
        We must do it here in order to make sure DiscordSRV loads all discord information.
         */
        SyncPlugin.getInstance().initialize();
        DiscordUtil.getJda().addEventListener(this);
    }

    public void onButtonClick(ButtonClickEvent event) {
        AccountLinkManager accountManager = DiscordSRV.getPlugin().getAccountLinkManager();

        if (event.getButton() == null) return;
        if (event.getButton().getId() == null) return;

        String response = "";
        System.out.println(accountManager.getLinkingCodes().toString());

        switch (event.getButton().getId()) {
            case "sync-account":
                event.deferReply().setEphemeral(true).queue();
                event.getHook().sendMessage("test").queue();

                event.getUser().openPrivateChannel()
                        .flatMap(channel -> channel.sendMessage("Enter your verification code below followed by the command /sync"))
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
}
