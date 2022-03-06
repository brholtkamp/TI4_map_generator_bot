package ti4.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import ti4.generator.GenerateMap;
import ti4.generator.Mapper;
import ti4.helpers.AliasHandler;
import ti4.helpers.Constants;
import ti4.map.Map;
import ti4.map.MapManager;
import ti4.map.MapSaveLoadManager;
import ti4.map.Tile;
import ti4.message.MessageHelper;

import java.io.File;
import java.util.StringTokenizer;

public class AddUnits implements Command {

    @Override
    public boolean accept(SlashCommandInteractionEvent event) {
        return event.getName().equals(Constants.ADD_UNITS);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Member member = event.getInteraction().getMember();
        if (member == null) {
            MessageHelper.replyToMessage(event, "Caller ID not found");
            return;
        }
        String userID = member.getId();
        MapManager mapManager = MapManager.getInstance();
        if (!mapManager.isUserWithActiveMap(userID)) {
            MessageHelper.replyToMessage(event, "Set your active map using: /set_map mapname");
        } else {

            String tileID = AliasHandler.resolveTile(event.getOptions().get(0).getAsString().toLowerCase());

            Map activeMap = mapManager.getUserActiveMap(userID);
            Tile tile = activeMap.getTile(tileID);
            if (tile == null) {
                MessageHelper.replyToMessage(event, "Tile in map not found");
                return;
            }
            String color = event.getOptions().get(1).getAsString().toLowerCase();
            if (!Mapper.isColorValid(color)) {
                MessageHelper.replyToMessage(event, "Color not valid");
                return;
            }

            String unitList = event.getOptions().get(2).getAsString().toLowerCase();
            unitList = unitList.replace(", ", ",");
            StringTokenizer tokenizer = new StringTokenizer(unitList, ",");
            while (tokenizer.hasMoreTokens()) {
                StringTokenizer unitInfoTokenizer = new StringTokenizer(tokenizer.nextToken(), " ");
                String unit = AliasHandler.resolveUnit(unitInfoTokenizer.nextToken());
                String unitID = Mapper.getUnitID(unit, color);
                String unitPath = tile.getUnitPath(unitID);
                if (unitPath == null) {
                    MessageHelper.replyToMessage(event, "Unit: " + unit + " is not valid and not supported.");
                }
                int count = 1;
                boolean numberIsSet = false;
                String planetName = Constants.SPACE;
                if (unitInfoTokenizer.hasMoreTokens()) {
                    String ifNumber = unitInfoTokenizer.nextToken();
                    try {
                        count = Integer.parseInt(ifNumber);
                        numberIsSet = true;
                    } catch (Exception e) {
                        planetName = AliasHandler.resolvePlanet(ifNumber);
                    }
                }
                if (unitInfoTokenizer.hasMoreTokens() && numberIsSet) {
                    planetName = AliasHandler.resolvePlanet(unitInfoTokenizer.nextToken());
                }
                tile.addUnit(planetName, unitID, count);
            }
            MapSaveLoadManager.saveMap(activeMap);

            File file = GenerateMap.getInstance().saveImage(activeMap);
            MessageHelper.replyToMessage(event, file);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void registerCommands(CommandListUpdateAction commands) {
        // Moderation commands with required options
        commands.addCommands(
                Commands.slash(Constants.ADD_UNITS, "Add units to map")
                        .addOptions(new OptionData(OptionType.STRING, Constants.TILE_NAME, "System name")
                                .setRequired(true))
                        .addOptions(new OptionData(OptionType.STRING, Constants.COLOR, "Color")
                                .setRequired(true))
                        .addOptions(new OptionData(OptionType.STRING, Constants.UNIT_NAMES, "Unit name/s. Example: DN, DN, CA")
                                .setRequired(true))


        );
    }
}
