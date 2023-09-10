package ti4.commands.franken;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.commands.leaders.LeaderInfo;
import ti4.generator.Mapper;
import ti4.helpers.Constants;
import ti4.helpers.Helper;
import ti4.map.Game;
import ti4.map.Player;

public abstract class LeaderAddRemove extends FrankenSubcommandData {
    public LeaderAddRemove(String name, String description) {
        super(name, description);
        addOptions(new OptionData(OptionType.STRING, Constants.LEADER, "Leader Name").setRequired(true).setAutoComplete(true));
        addOptions(new OptionData(OptionType.STRING, Constants.LEADER_1, "Leader Name").setAutoComplete(true));
        addOptions(new OptionData(OptionType.STRING, Constants.LEADER_2, "Leader Name").setAutoComplete(true));
        addOptions(new OptionData(OptionType.STRING, Constants.LEADER_3, "Leader Name").setAutoComplete(true));
        addOptions(new OptionData(OptionType.STRING, Constants.LEADER_4, "Leader Name").setAutoComplete(true));
    }

    public void execute(SlashCommandInteractionEvent event) {
        List<String> leaderIDs = new ArrayList<>();

        //GET ALL ABILITY OPTIONS AS STRING
        for (OptionMapping option : event.getOptions().stream().filter(o -> o != null && o.getName().contains(Constants.LEADER)).toList()) {
            leaderIDs.add(option.getAsString());
        }

        leaderIDs.removeIf(StringUtils::isEmpty);
        leaderIDs.removeIf(leaderID -> !Mapper.getLeaderRepresentations().containsKey(leaderID));

        if (leaderIDs.isEmpty()) {
            sendMessage("No valid leaders were provided. Please see `/help list_leaders` for available choices.");
            return;
        }
        
        Game activeGame = getActiveGame();
        Player player = activeGame.getPlayer(getUser().getId());
        player = Helper.getGamePlayer(activeGame, player, event, null);
        if (player == null) {
            sendMessage("Player could not be found");
            return;
        }

        doAction(player, leaderIDs);

        LeaderInfo.sendLeadersInfo(activeGame, player, event);
    }

    public abstract void doAction(Player player, List<String> leaderIDs);
    
}
