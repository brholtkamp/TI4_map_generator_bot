package ti4.commands.fow;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.helpers.Constants;
import ti4.helpers.Helper;
import ti4.map.*;
import ti4.message.MessageHelper;

public class SetFogFilter extends FOWSubcommandData {
    public SetFogFilter() {
        super(Constants.SET_FOG_FILTER, "Set the color of the fog tiles for your view of the map.");
        addOptions(new OptionData(OptionType.STRING, Constants.FOG_FILTER, "How you want the tile to be labeled").setAutoComplete(true).setRequired(true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Map activeMap = getActiveMap();
        Player player = activeMap.getPlayer(getUser().getId());
        player = Helper.getGamePlayer(activeMap, player, event, null);

        MessageChannel channel = event.getChannel();
        if (player == null) {
            MessageHelper.sendMessageToChannel(channel, "You're not a player of this game");
            return;
        }

        OptionMapping fogColorMapping = event.getOption(Constants.FOG_FILTER);
        if (fogColorMapping == null) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "Specify color");
            return;
        }

        String color_suffix = null;
        switch (fogColorMapping.getAsString()) {
            case Constants.FOW_FILTER_DARK_GREY -> color_suffix = "default";
            case Constants.FOW_FILTER_SEPIA -> color_suffix = "sepia";
            case Constants.FOW_FILTER_WHITE -> color_suffix = "white";
            case Constants.FOW_FILTER_PINK -> color_suffix = "pink";
            case Constants.FOW_FILTER_PURPLE -> color_suffix = "purple";
            case Constants.FOW_FILTER_FROG -> color_suffix = "frog";
        }

        player.setFogFilter(color_suffix);
        MapSaveLoadManager.saveMap(activeMap);
    }
}