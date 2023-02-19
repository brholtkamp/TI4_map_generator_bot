package ti4.commands.game;

import java.io.File;
import java.util.List;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.generator.GenerateMap;
import ti4.helpers.Constants;
import ti4.helpers.DisplayType;
import ti4.map.Map;
import ti4.map.MapManager;
import ti4.map.MapSaveLoadManager;
import ti4.message.MessageHelper;

public class GameEnd extends GameSubcommandData {
    public GameEnd() {
            super(Constants.GAME_END, "Declare the game has ended - deletes role and informs @Bothelper");
            addOptions(new OptionData(OptionType.STRING, Constants.CONFIRM, "Confirm ending the game with 'YES'").setRequired(true));
    }

    public void execute(SlashCommandInteractionEvent event) {
        MapManager mapManager = MapManager.getInstance();
        Map userActiveMap = mapManager.getUserActiveMap(event.getUser().getId());

        if (userActiveMap == null) {
            MessageHelper.replyToMessage(event, "Must set active Game");
            return;
        }
        String gameName = userActiveMap.getName();
        if (!event.getChannel().getName().startsWith(gameName + "-")) {
            MessageHelper.replyToMessage(event, "`/game end` must be executed in game channel only!");
            return;
        }
        OptionMapping option = event.getOption(Constants.CONFIRM);
        if (option == null || !"YES".equals(option.getAsString())) {
            MessageHelper.replyToMessage(event, "Must confirm with 'YES'");
            return;
        }

        List<Role> gameRoles = event.getGuild().getRolesByName(gameName, true);
        if (gameRoles.size() > 1) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "There are multiple roles that match this game name (" + gameName + "): " + gameRoles);
        } else if (gameRoles.size() == 0) {
            MessageHelper.sendMessageToChannel(event.getChannel(), "No roles match the game name (" + gameName + ")");
        } else {
            //POST GAME INFO
            userActiveMap.setHasEnded(true);
            MapSaveLoadManager.saveMap(userActiveMap);
            MessageHelper.sendMessageToChannel(event.getChannel(), Info.getGameInfo(null, null, userActiveMap).toString());           

            //SEND THE MAP IMAGE
            File file = GenerateMap.getInstance().saveImage(userActiveMap, DisplayType.map, event);
            MessageHelper.replyToMessage(event, file);
            
            //INFORM BOTHELPER
            MessageHelper.sendMessageToChannel(event.getChannel(), event.getGuild().getRolesByName("Bothelper", true).get(0).getAsMention() + " - this game has concluded");
            // TextChannel bothelperLoungeChannel = event.getGuild().getTextChannelById(1029569891193331712l);
            TextChannel bothelperLoungeChannel = event.getGuild().getTextChannelsByName("bothelper-lounge", true).get(0);
            if (bothelperLoungeChannel != null) MessageHelper.sendMessageToChannel(bothelperLoungeChannel, event.getChannel().getAsMention() + " - Game: " + gameName + " has concluded. React here when complete");
            
            //ASK USERS FOR SUMMARY
            TextChannel pbdChroniclesChannel = event.getGuild().getTextChannelsByName("the-pbd-chronicles", true).get(0);
            String channelMention = pbdChroniclesChannel == null ? "#the-pbd-chronicles" : pbdChroniclesChannel.getAsMention();
            Role gameRole = gameRoles.get(0);
            StringBuilder message = new StringBuilder();
            for (Member member : event.getGuild().getMembersWithRoles(gameRole)) {
                message.append(member.getAsMention());
            }
            message.append("\nPlease provide a summary of the game for the @Bothelper to post into " + channelMention);
            MessageHelper.sendMessageToChannel(event.getChannel(), message.toString());
            
            MessageHelper.sendMessageToChannel(event.getChannel(), "Role deleted: " + gameRole.getName());
            gameRole.delete().queue();
            
        }
    }
}