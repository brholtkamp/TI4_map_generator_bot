package ti4.commands.status;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import ti4.generator.Mapper;
import ti4.helpers.Constants;
import ti4.helpers.Emojis;
import ti4.helpers.Helper;
import ti4.map.*;
import ti4.message.MessageHelper;

public class RevealStage2 extends StatusSubcommandData {
    public RevealStage2() {
        super(Constants.REVEAL_STATGE2, "Reveal Stage2 Public Objective");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        revealS2(event, event.getChannel());
    }

    public void revealS2(GenericInteractionCreateEvent event, MessageChannel channel)
    {
        Map activeMap = MapManager.getInstance().getUserActiveMap(event.getUser().getId());
        java.util.Map.Entry<String, Integer> objective = activeMap.revealState2();

        String[] objectiveText = Mapper.getPublicObjective(objective.getKey()).split(";");
        String objectiveName = objectiveText[0];
        // String objectivePhase = objectiveText[1];
        String objectiveDescription = objectiveText[2];
        // String objectiveValue = objectiveText[3];

        StringBuilder sb = new StringBuilder();
        sb.append(Helper.getGamePing(event, activeMap));
        sb.append(" **Stage 2 Public Objective Revealed**").append("\n");
        sb.append("(").append(objective.getValue()).append(") ");
        sb.append(Emojis.Public2alt).append(" ");
        sb.append("**").append(objectiveName).append("** - ").append(objectiveDescription).append("\n");
        MessageHelper.sendMessageToChannelAndPin(channel, sb.toString());
    }

    @Override
    public void reply(SlashCommandInteractionEvent event) {
        String userID = event.getUser().getId();
        Map activeMap = MapManager.getInstance().getUserActiveMap(userID);
        MapSaveLoadManager.saveMap(activeMap, event);
    }
}
