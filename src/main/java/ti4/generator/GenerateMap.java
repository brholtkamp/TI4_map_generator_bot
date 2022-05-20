package ti4.generator;

import org.jetbrains.annotations.NotNull;
import ti4.ResourceHelper;
import ti4.helpers.*;
import ti4.map.Map;
import ti4.map.*;

import javax.annotation.CheckForNull;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class GenerateMap {
    private Graphics graphics;
    private BufferedImage mainImage;
    private int width;
    private int height;
    private int heightStorage;
    private int heightStats;
    private int heightForGameInfo;
    private int heightForGameInfoStorage;
    private int scoreTokenWidth;
    private int extraWidth = 200;
    private static Point tilePositionPoint = new Point(230, 295);
    private static Point numberPositionPoint = new Point(45, 35);
    private static HashMap<Player, Integer> userVPs = new HashMap<>();

    private final int width6 = 2000;
    private final int heght6 = 2100;
    private final int width8 = 2500;
    private final int heght8 = 3050;

    private static GenerateMap instance;

    private GenerateMap() {
        try {
            String controlID = Mapper.getControlID("red");
            BufferedImage bufferedImage = resizeImage(ImageIO.read(new File(Mapper.getCCPath(controlID))), 0.45f);
            scoreTokenWidth = bufferedImage.getWidth();
        } catch (IOException e) {
            LoggerHandler.logError("Could read file data for setup file", e);
        }
        init(null);
        resetImage();
    }

    private void init(Map map) {
        int mapWidth = width6;
        int mapHeight = heght6;
        if (map != null && map.getPlayerCountForMap() == 8) {
            mapWidth = width8;
            mapHeight = heght8;
        }
        width = mapWidth + (extraWidth * 2);
        heightForGameInfo = mapHeight;
        heightForGameInfoStorage = heightForGameInfo;
        height = heightForGameInfo + mapHeight / 2 + 2000;
        heightStats = mapHeight / 2 + 2000;
        heightStorage = height;
    }

    private void resetImage() {
        mainImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        graphics = mainImage.getGraphics();
    }

    public static GenerateMap getInstance() {
        if (instance == null) {
            instance = new GenerateMap();
        }
        return instance;
    }

    public File saveImage(Map map) {
        if (map.getDisplayTypeForced() != null) {
            return saveImage(map, map.getDisplayTypeForced());
        }
        return saveImage(map, DisplayType.all);
    }

    public File saveImage(Map map, @CheckForNull DisplayType displayType) {
        init(map);
        if (map.getDisplayTypeForced() != null) {
            displayType = map.getDisplayTypeForced();
        } else if (displayType == null) {
            displayType = map.getDisplayTypeForced();
            if (displayType == null) {
                displayType = DisplayType.all;
            }
        }
        if (displayType == DisplayType.stats) {
            heightForGameInfo = 40;
            height = heightStats;
        } else if (displayType == DisplayType.map) {
            heightForGameInfo = heightForGameInfoStorage;
            height = heightForGameInfoStorage + 300;
        } else {
            heightForGameInfo = heightForGameInfoStorage;
            height = heightStorage;
        }
        resetImage();
        File file = Storage.getMapImageStorage("temp.png");
        try {
            if (displayType == DisplayType.all || displayType == DisplayType.map) {
                HashMap<String, Tile> tileMap = new HashMap<>(map.getTileMap());
                String setup = tileMap.keySet().stream()
                        .filter(key -> key.startsWith("setup"))
                        .findFirst()
                        .orElse(null);
                if (setup != null) {
                    addTile(tileMap.get(setup), map);
                    tileMap.remove(setup);
                }
                tileMap.keySet().stream()
                        .sorted()
                        .forEach(key -> addTile(tileMap.get(key), map));
            }
            graphics.setFont(Storage.getFont32());
            graphics.setColor(Color.WHITE);
            String timeStamp = getTimeStamp();
            graphics.drawString(map.getName() + " " + timeStamp, 0, 34);

            gameInfo(map, displayType);

            ImageWriter imageWriter = ImageIO.getImageWritersByFormatName("png").next();
            imageWriter.setOutput(ImageIO.createImageOutputStream(file));
            ImageWriteParam defaultWriteParam = imageWriter.getDefaultWriteParam();
            if (defaultWriteParam.canWriteCompressed()) {
                defaultWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                defaultWriteParam.setCompressionQuality(0.01f);
            }

            imageWriter.write(null, new IIOImage(mainImage, null, null), defaultWriteParam);
        } catch (IOException e) {
            LoggerHandler.log("Could not save generated map");
        }

        String timeStamp = getTimeStamp();
        String absolutePath = file.getParent() + "/" + map.getName() + "_" + timeStamp + ".jpg";
        try (FileInputStream fileInputStream = new FileInputStream(file);
             FileOutputStream fileOutputStream = new FileOutputStream(absolutePath)) {

            final BufferedImage image = ImageIO.read(fileInputStream);
            fileInputStream.close();

            final BufferedImage convertedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            convertedImage.createGraphics().drawImage(image, 0, 0, Color.black, null);

            final boolean canWrite = ImageIO.write(convertedImage, "jpg", fileOutputStream);

            if (!canWrite) {
                throw new IllegalStateException("Failed to write image.");
            }
        } catch (IOException e) {
            LoggerHandler.log("Could not save jpg file", e);
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
        File jpgFile = new File(absolutePath);
        MapFileDeleter.addFileToDelete(jpgFile);
        return jpgFile;
    }

    @NotNull
    public static String getTimeStamp() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd - HH.mm.ss");
        return ZonedDateTime.now(ZoneOffset.UTC).format(fmt);

    }

    @CheckForNull
    private String getFactionPath(String factionID) {
        if (factionID.equals("null")) {
            return null;
        }
        String factionFileName = Mapper.getFactionFileName(factionID);
        String factionFile = ResourceHelper.getInstance().getFactionFile(factionFileName);
        if (factionFile == null) {
            LoggerHandler.log("Could not find faction: " + factionID);
        }
        return factionFile;
    }

    private String getGeneralPath(String tokenID) {
        String fileName = Mapper.getGeneralFileName(tokenID);
        String filePath = ResourceHelper.getInstance().getGeneralFile(fileName);
        if (filePath == null) {
            LoggerHandler.log("Could not find general token: " + tokenID);
        }
        return filePath;
    }

    private void gameInfo(Map map, DisplayType displayType) throws IOException {

        int widthOfLine = 2300;
        int y = heightForGameInfo + 60;
        int x = 10;
        HashMap<String, Player> players = map.getPlayers();
        float percent = 0.15f;
        int deltaY = 35;

        int tempY = y;
        y += 200;
        y = objectives(map, y);

        graphics.setFont(Storage.getFont50());
        graphics.setColor(Color.WHITE);
        graphics.drawString(map.getCustomName(), 0, tempY);
        scoreTrack(map, tempY + 40);
        if (displayType != DisplayType.stats) {
            playerInfo(map);
        }

        if (displayType == DisplayType.all || displayType == DisplayType.stats) {
            graphics.setFont(Storage.getFont32());
            Graphics2D g2 = (Graphics2D) graphics;
            g2.setStroke(new BasicStroke(5));
            for (Player player : players.values()) {
                int baseY = y;
                y += 34;
                graphics.setFont(Storage.getFont32());
                Color color = getColor(player.getColor());
                graphics.setColor(Color.WHITE);
                String userName = player.getUserName() + " (" + player.getColor() + ")";
                graphics.drawString(userName, x, y);
                y += 2;
                String faction = player.getFaction();
                if (faction != null) {
                    String factionPath = getFactionPath(faction);
                    if (factionPath != null) {
                        BufferedImage bufferedImage;
                        if ("keleres".equals(faction)) {
                            bufferedImage = resizeImage(ImageIO.read(new File(factionPath)), 0.7f);
                        } else {
                            bufferedImage = resizeImage(ImageIO.read(new File(factionPath)), percent);
                        }
                        graphics.drawImage(bufferedImage, x, y, null);
                    }
                }
                StringBuilder sb = new StringBuilder();
                int sc = player.getSC();
                String scText = sc == 0 ? " " : Integer.toString(sc);
                scText = getSCNumberIfNaaluInPlay(player, map, scText);
                sb.append("SC: ").append(scText).append("   ");

                graphics.setColor(getSCColor(sc, map));

                graphics.drawString(sb.toString(), x + 100, y + deltaY);
                graphics.setColor(color);

                graphics.setColor(Color.WHITE);
                sb = new StringBuilder();
                sb.append(player.getTacticalCC()).append("T/");
                if ("letnev".equals(faction)) {
                    sb.append(player.getFleetCC()).append("+2").append("F/");
                } else {
                    sb.append(player.getFleetCC()).append("F/");
                }
                sb.append(player.getStrategicCC()).append("S ");
                sb.append("TG: ").append(player.getTg());
                sb.append(" C:").append(player.getCommodities()).append("/").append(player.getCommoditiesTotal());
                sb.append(" ").append("AC: ").append(player.getAc()).append(" ");
                sb.append("PN: ").append(player.getPnCount()).append(" ");
                sb.append("SO: ").append(player.getSo()).append(" scored: ").append(player.getSoScored()).append(" ");
                sb.append("CRF: ").append(player.getCrf()).append(" ");
                sb.append("HRF: ").append(player.getHrf()).append(" ");
                sb.append("IRF: ").append(player.getIrf()).append(" ");
                sb.append("VRF: ").append(player.getVrf()).append(" ");
                if (player.isPassed()) {
                    sb.append(" PASSED");

                }
                graphics.drawString(sb.toString(), x + 230, y + deltaY);

                int pnX = 0;
                int pnY = 40;
                List<String> promissoryNotesInPlayArea = player.getPromissoryNotesInPlayArea();
                for (String id : promissoryNotesInPlayArea) {
                    if (id.endsWith("_sftt")) {
                        continue;
                    }
                    String promissoryNoteOwner = Mapper.getPromissoryNoteOwner(id);
                    for (Player player_ : players.values()) {
                        if (player_ != player) {
                            String playerColor = player_.getColor();
                            String playerFaction = player_.getFaction();
                            if (playerColor != null && playerColor.equals(promissoryNoteOwner) ||
                                    playerFaction != null && playerFaction.equals(promissoryNoteOwner)) {
                                graphics.setColor(getColor(player_.getColor()));
                                String promissoryNote = Mapper.getPromissoryNote(id);
                                String[] pnSplit = promissoryNote.split(";");
                                graphics.drawString(pnSplit[0] + "(" + playerFaction + ")", x + 230 + pnX, y + deltaY + pnY);
                                pnX = pnX + pnSplit[0].length() * 28;
                            }
                        }
                    }
                }
                int techStartY = y + deltaY + pnY;
                int techY = techs(player, techStartY);
                graphics.setColor(color);
                y += 90 + (techY - techStartY);

                if (!player.getPlanets().isEmpty()) {
                    planetInfo(player, map, 10, y);
                    y += 155;
                }

                g2.setColor(color);
                g2.drawRect(x - 5, baseY, x + widthOfLine, y - baseY);
                y += 15;

            }
            y = strategyCards(map, y);

            y += 40;
            graphics.setColor(Color.WHITE);
            graphics.setFont(Storage.getFont32());
            graphics.drawString("LAWS", x, y);

            graphics.setFont(Storage.getFont26());
            LinkedHashMap<String, Integer> laws = map.getLaws();
            LinkedHashMap<String, String> lawsInfo = map.getLawsInfo();
            for (java.util.Map.Entry<String, Integer> lawEntry : laws.entrySet()) {
                y += 30;
                String lawID = lawEntry.getKey();
                String text = "(" + lawEntry.getValue() + ") ";
                String optionalText = lawsInfo.get(lawID);
                if (optionalText != null) {
                    text += "Elected: " + optionalText + " - ";
                }
                graphics.drawString(text + Mapper.getAgendaForOnly(lawID), x, y);
            }
        }
    }

    private void planetInfo(Player player, Map map, int x, int y) {
        HashMap<String, UnitHolder> planetsInfo = map.getPlanetsInfo();
        List<String> planets = player.getPlanets();
        List<String> exhaustedPlanets = player.getExhaustedPlanets();
        List<String> exhaustedPlanetsAbilities = player.getExhaustedPlanetsAbilities();

        int deltaX = 0;

        Graphics2D g2 = (Graphics2D) graphics;
        g2.setStroke(new BasicStroke(2));

        for (String planet : planets) {

            UnitHolder unitHolder = planetsInfo.get(planet);
            if (!(unitHolder instanceof Planet planetHolder)) {
                LoggerHandler.log("Planet unitHolder not found: " + planet);
                continue;
            }

            boolean isExhausted = exhaustedPlanets.contains(planet);
            if (isExhausted) {
                graphics.setColor(Color.GRAY);
            } else {
                graphics.setColor(Color.WHITE);
            }

            int resources = planetHolder.getResources();
            int influence = planetHolder.getInfluence();
            String statusOfPlanet = isExhausted ? "_exh" : "_rdy";
            String planetFileName = "pc_planetname_" + planet + statusOfPlanet + ".png";
            String resFileName = "pc_res_" + resources + statusOfPlanet + ".png";
            String infFileName = "pc_inf_" + influence + statusOfPlanet + ".png";

            graphics.drawRect(x + deltaX - 2, y - 2, 52, 152);

            if (unitHolder.getTokenList().contains("titanspn")) {
                String planetTypeName = "pc_attribute_titanspn.png";
                drawImage(x + deltaX + 2, y + 2, planet, planetTypeName);
            } else {
                String originalPlanetType = planetHolder.getOriginalPlanetType();
                if (!originalPlanetType.isEmpty()) {
                    String planetTypeName = "pc_attribute_" + originalPlanetType + ".png";
                    drawImage(x + deltaX + 2, y + 2, planet, planetTypeName);
                }
            }

            boolean hasAttachment = planetHolder.hasAttachment();
            if (hasAttachment) {
                String planetTypeName = "pc_upgrade.png";
                drawImage(x + deltaX + 26, y + 40, planet, planetTypeName);
            }

            boolean hasAbility = planetHolder.isHasAbility();
            if (hasAbility){
                String statusOfAbility = exhaustedPlanetsAbilities.contains(planet) ? "_exh" : "_rdy";
                String planetTypeName = "pc_legendary"+ statusOfAbility +".png";
                drawImage(x + deltaX + 26, y + 60, planet, planetTypeName);
            }
            String originalTechSpeciality = planetHolder.getOriginalTechSpeciality();
            int deltaY = 175;
            if (!originalTechSpeciality.isEmpty()) {
                String planetTypeName = "pc_tech_" + originalTechSpeciality + statusOfPlanet + ".png";
                drawImage(x + deltaX + 26, y + 82, planet, planetTypeName);
            } else {
                ArrayList<String> techSpeciality = planetHolder.getTechSpeciality();
                for (String techSpec : techSpeciality) {
                    String planetTypeName = "pc_tech_" + techSpec + statusOfPlanet + ".png";
                    drawImage(x + deltaX + 26, y + 82, planet, planetTypeName);
                    deltaY -= 20;
                }
            }


            drawImage(x + deltaX + 26, y + 103, planet, resFileName);
            drawImage(x + deltaX + 26, y + 125, planet, infFileName);
            drawImage(x + deltaX, y, planet, planetFileName);







            deltaX += 56;
        }
    }

    private void drawImage(int x, int y, String planet, String resourceName) {
        try {
            String resourcePath = ResourceHelper.getInstance().getPlanetResource(resourceName);
            @SuppressWarnings("ConstantConditions")
            BufferedImage resourceBufferedImage = ImageIO.read(new File(resourcePath));
            graphics.drawImage(resourceBufferedImage, x, y, null);
        } catch (Exception e) {
            LoggerHandler.log("Could not display planet: " + resourceName, e);
        }
    }

    private void scoreTrack(Map map, int y) {

        Graphics2D g2 = (Graphics2D) graphics;
        g2.setStroke(new BasicStroke(5));
        graphics.setFont(Storage.getFont50());
        int height = 140;
        int width = 150;
        for (int i = 0; i <= map.getVp(); i++) {
            graphics.setColor(Color.WHITE);
            graphics.drawString(Integer.toString(i), i * width + 55, y + (height / 2) + 25);
            g2.setColor(Color.RED);
            g2.drawRect(i * width, y, width, height);
        }

        Collection<Player> players = map.getPlayers().values();
        int tempCounter = 0;
        int tempX = 0;
        int tempWidth = 0;
        for (Player player : players) {
            try {
                String controlID = Mapper.getControlID(player.getColor());
                BufferedImage bufferedImage = resizeImage(ImageIO.read(new File(Mapper.getCCPath(controlID))), 0.7f);
                tempWidth = bufferedImage.getWidth();
                Integer vpCount = userVPs.get(player);
                if (vpCount == null) {
                    vpCount = 0;
                }
                int x = vpCount * width + 5 + tempX;
                graphics.drawImage(bufferedImage, x, y + (tempCounter * bufferedImage.getHeight()), null);
            } catch (Exception e) {
                LoggerHandler.log("Could not display player: " + player.getUserName() + " VP count", e);
            }
            tempCounter++;
            if (tempCounter >= 4) {
                tempCounter = 0;
                tempX = tempWidth;
            }
        }
    }

    public static String getSCNumberIfNaaluInPlay(Player player, Map map, String scText) {
        if (Constants.NAALU.equals(player.getFaction())) {
            boolean giftPlayed = false;
            for (Player player_ : map.getPlayers().values()) {
                if (player != player_ && player_.getPromissoryNotesInPlayArea().contains(Constants.NAALU_PN)) {
                    giftPlayed = true;
                    break;
                }
            }
            if (!giftPlayed) {
                scText = "0/" + scText;
            }
        } else if (player.getPromissoryNotesInPlayArea().contains(Constants.NAALU_PN)) {
            scText = "0/" + scText;
        }
        return scText;
    }

    private int strategyCards(Map map, int y) {
        y += 80;
        LinkedHashMap<Integer, Integer> scTradeGoods = map.getScTradeGoods();
        Collection<Player> players = map.getPlayers().values();
        Set<Integer> scPicked = players.stream().map(Player::getSC).collect(Collectors.toSet());
        int x = 20;
        for (java.util.Map.Entry<Integer, Integer> scTGs : scTradeGoods.entrySet()) {
            Integer sc = scTGs.getKey();
            if (!scPicked.contains(sc)) {
                graphics.setColor(getSCColor(sc));
                graphics.setFont(Storage.getFont64());
                graphics.drawString(Integer.toString(sc), x, y);
                Integer tg = scTGs.getValue();
                if (tg > 0) {
                    graphics.setFont(Storage.getFont26());
                    graphics.setColor(Color.WHITE);
                    graphics.drawString("TG:" + tg, x, y + 30);
                }
            }
            x += 80;
        }
        return y + 40;
    }

    private void playerInfo(Map map) {
        int playerPosition = 1;
        graphics.setFont(Storage.getFont32());
        graphics.setColor(Color.WHITE);
        Player speaker = map.getPlayer(map.getSpeaker());
        for (java.util.Map.Entry<String, Player> playerEntry : map.getPlayers().entrySet()) {
            ArrayList<Point> points = PositionMapper.getPlayerPosition(playerPosition, map);
            if (points.isEmpty()) {
                continue;
            }
            Player player = playerEntry.getValue();
            String userName = player.getUserName();

            graphics.drawString(userName.substring(0, Math.min(userName.length(), 11)), points.get(0).x, points.get(0).y);
            Integer vpCount = userVPs.get(player);
            vpCount = vpCount == null ? 0 : vpCount;
            graphics.drawString("VP - " + vpCount, points.get(1).x, points.get(1).y);

            int sc = player.getSC();
            String scText = sc == 0 ? " " : Integer.toString(sc);
            scText = getSCNumberIfNaaluInPlay(player, map, scText);
            graphics.setColor(getSCColor(sc, map));
            graphics.setFont(Storage.getFont64());
            graphics.drawString(scText, points.get(4).x, points.get(4).y);

            graphics.setColor(Color.WHITE);
            graphics.setFont(Storage.getFont32());
            String ccID = Mapper.getCCID(player.getColor());
            String fleetCCID = Mapper.getFleeCCID(player.getColor());
            int x = points.get(2).x;
            int y = points.get(2).y;
            drawCCOfPlayer(ccID, x, y, player.getTacticalCC(), false);
//            drawCCOfPlayer(fleetCCID, x, y + 65, player.getFleetCC(), "letnev".equals(player.getFaction()));
            drawCCOfPlayer(fleetCCID, x, y + 65, player.getFleetCC(), false);
            drawCCOfPlayer(ccID, x, y + 130, player.getStrategicCC(), false);

            if (player == speaker) {
                String speakerID = Mapper.getTokenID(Constants.SPEAKER);
                String speakerFile = ResourceHelper.getInstance().getTokenFile(speakerID);
                if (speakerFile != null) {
                    BufferedImage bufferedImage = null;
                    try {
                        bufferedImage = ImageIO.read(new File(speakerFile));
                    } catch (IOException e) {
                        LoggerHandler.log("Could not read speaker file");
                    }
                    graphics.drawImage(bufferedImage, points.get(3).x, points.get(3).y, null);
                    graphics.setColor(Color.WHITE);
                }
            }
            if (player.isPassed()) {
                graphics.setColor(new Color(238, 58, 80));
                graphics.drawString("PASSED", points.get(5).x, points.get(5).y);
                graphics.setColor(Color.WHITE);
            }

            playerPosition++;
        }


    }

    private void drawCCOfPlayer(String ccID, int x, int y, int ccCount, boolean isLetnev) {
        String ccPath = Mapper.getCCPath(ccID);
        try {
            BufferedImage ccImage = resizeImage(ImageIO.read(new File(ccPath)), 0.75f);
            int delta = 20;
            if (isLetnev) {
                for (int i = 0; i < 2; i++) {
                    graphics.drawImage(ccImage, x + (delta * i), y, null);
                }
                x += 20;
                for (int i = 2; i < ccCount + 2; i++) {
                    graphics.drawImage(ccImage, x + (delta * i), y, null);
                }
            } else {
                for (int i = 0; i < ccCount; i++) {
                    graphics.drawImage(ccImage, x + (delta * i), y, null);
                }
            }
        } catch (Exception e) {
            LoggerHandler.log("Could not parse cc file for: " + ccID, e);
        }
    }

    private int techs(Player player, int y) {
        int x = 230;
        Graphics2D g2 = (Graphics2D) graphics;
        g2.setStroke(new BasicStroke(3));
        graphics.setFont(Storage.getFont26());
        Integer[] column = new Integer[1];
        column[0] = 0;
        List<String> techs = player.getTechs();
        List<String> exhaustedTechs = player.getExhaustedTechs();
        if (techs.isEmpty()) {
            return y;
        }
        techs.sort(Comparator.comparing(Mapper::getTechType));
        HashMap<String, String> techInfo = Mapper.getTechs();
        y += 25;
        for (String tech : techs) {
            switch (column[0]) {
                case 0 -> x = 230;
                case 1 -> x = 630;
                case 2 -> x = 1030;
                case 3 -> x = 1430;
                case 4 -> x = 1830;
            }
            if (exhaustedTechs.contains(tech)) {
                graphics.setColor(Color.GRAY);
            } else {
                graphics.setColor(getTechColor(Mapper.getTechType(tech)));
            }
            String techName = techInfo.get(tech);
            if (techName != null) {
                graphics.drawString(techName, x, y);
            }
            column[0]++;
            if (column[0] > 4) {
                column[0] = 0;
                y += 25;
            }
        }
        return y;
    }

    private int objectives(Map map, int y) {
        int x = 5;
        Graphics2D g2 = (Graphics2D) graphics;
        g2.setStroke(new BasicStroke(3));
        userVPs = new HashMap<>();

        LinkedHashMap<String, List<String>> scoredPublicObjectives = new LinkedHashMap<>(map.getScoredPublicObjectives());
        LinkedHashMap<String, Integer> revealedPublicObjectives = new LinkedHashMap<>(map.getRevealedPublicObjectives());
        LinkedHashMap<String, Player> players = map.getPlayers();
        HashMap<String, String> publicObjectivesState1 = Mapper.getPublicObjectivesState1();
        HashMap<String, String> publicObjectivesState2 = Mapper.getPublicObjectivesState2();
        HashMap<String, String> secretObjectives = Mapper.getSecretObjectivesJustNames();
        LinkedHashMap<String, Integer> customPublicVP = map.getCustomPublicVP();
        LinkedHashMap<String, String> customPublics = customPublicVP.keySet().stream().collect(Collectors.toMap(key -> key, name -> name, (key1, key2) -> key1, LinkedHashMap::new));
        Set<String> po1 = publicObjectivesState1.keySet();
        Set<String> po2 = publicObjectivesState2.keySet();
        Set<String> customVP = customPublicVP.keySet();
        Set<String> secret = secretObjectives.keySet();

        graphics.setFont(Storage.getFont26());
        graphics.setColor(new Color(230, 126, 34));
        Integer[] column = new Integer[1];
        column[0] = 0;
        y = displayObjectives(y, x, scoredPublicObjectives, revealedPublicObjectives, players, publicObjectivesState1, po1, 1, column, null);

        graphics.setColor(new Color(93, 173, 226));
        y = displayObjectives(y, x, scoredPublicObjectives, revealedPublicObjectives, players, publicObjectivesState2, po2, 2, column, null);

        graphics.setColor(Color.WHITE);
        y = displayObjectives(y, x, scoredPublicObjectives, revealedPublicObjectives, players, customPublics, customVP, null, column, customPublicVP);

        revealedPublicObjectives = new LinkedHashMap<>();
        scoredPublicObjectives = new LinkedHashMap<>();
        for (java.util.Map.Entry<String, Player> playerEntry : players.entrySet()) {
            Player player = playerEntry.getValue();
            LinkedHashMap<String, Integer> secretsScored = player.getSecretsScored();
            revealedPublicObjectives.putAll(secretsScored);
            for (String id : secretsScored.keySet()) {
                scoredPublicObjectives.put(id, List.of(player.getUserID()));
            }
        }

        graphics.setColor(Color.RED);
        y = displayObjectives(y, x, scoredPublicObjectives, revealedPublicObjectives, players, secretObjectives, secret, 1, column, customPublicVP);

        graphics.setColor(Color.green);
        y = displaySftT(y, x, players, column);

        if (column[0] != 0) {
            y += 40;
        }

        return y;
    }

    private int displaySftT(int y, int x, LinkedHashMap<String, Player> players, Integer[] column) {
        for (Player player : players.values()) {
            List<String> promissoryNotesInPlayArea = player.getPromissoryNotesInPlayArea();
            for (String id : promissoryNotesInPlayArea) {
                if (id.endsWith("_sftt")) {
                    Set<String> keysToRemove = new HashSet<>();

                    switch (column[0]) {
                        case 0 -> x = 5;
                        case 1 -> x = 801;
                        case 2 -> x = 1598;
                    }
                    String[] pnSplit = Mapper.getPromissoryNote(id).split(";");
                    String promissoryNoteOwner = Mapper.getPromissoryNoteOwner(id);
                    StringBuilder name = new StringBuilder(pnSplit[0] + " - ");
                    for (Player player_ : players.values()) {
                        if (player_ != player) {
                            String playerColor = player_.getColor();
                            String playerFaction = player_.getFaction();
                            if (playerColor != null && playerColor.equals(promissoryNoteOwner) ||
                                    playerFaction != null && playerFaction.equals(promissoryNoteOwner)) {
                                name.append(playerFaction).append(" (").append(playerColor).append(")");
                            }
                        }
                    }

                    graphics.drawString(name + " - " + 1 + " VP", x, y + 23);
                    boolean multiScoring = false;
                    drawScoreControlMarkers(x + 515, y, players, Collections.singletonList(player.getUserID()), multiScoring, 1);
                    graphics.drawRect(x - 4, y - 5, 785, 38);
                    column[0]++;
                    if (column[0] > 2) {
                        column[0] = 0;
                        y += 43;
                    }
                }
            }
        }
        return y;
    }

    private int displayObjectives(int y, int x, LinkedHashMap<String, List<String>> scoredPublicObjectives, LinkedHashMap<String, Integer> revealedPublicObjectives,
                                  LinkedHashMap<String, Player> players, HashMap<String, String> publicObjectivesState, Set<String> po, Integer objectiveWorth, Integer[] column, LinkedHashMap<String, Integer> customPublicVP) {
        Set<String> keysToRemove = new HashSet<>();
        for (java.util.Map.Entry<String, Integer> revealed : revealedPublicObjectives.entrySet()) {
            switch (column[0]) {
                case 0 -> x = 5;
                case 1 -> x = 801;
                case 2 -> x = 1598;
            }

            String key = revealed.getKey();
            if (!po.contains(key)) {
                continue;
            }
            String name = publicObjectivesState.get(key);
            Integer index = revealedPublicObjectives.get(key);
            if (index == null) {
                continue;
            }
            keysToRemove.add(key);
            if (customPublicVP != null) {
                objectiveWorth = customPublicVP.get(key);
                if (objectiveWorth == null) {
                    objectiveWorth = 1;
                }
            }
            graphics.drawString("(" + index + ") " + name + " - " + objectiveWorth + " VP", x, y + 23);
            List<String> scoredPlayerID = scoredPublicObjectives.get(key);
            boolean multiScoring = Constants.CUSTODIAN.equals(key);
            if (scoredPlayerID != null) {
                drawScoreControlMarkers(x + 515, y, players, scoredPlayerID, multiScoring, objectiveWorth);
            }
            graphics.drawRect(x - 4, y - 5, 785, 38);
            column[0]++;
            if (column[0] > 2) {
                column[0] = 0;
                y += 43;
            }
        }
        keysToRemove.forEach(revealedPublicObjectives::remove);

        return y;
    }

    private void drawScoreControlMarkers(int x, int y, LinkedHashMap<String, Player> players, List<String> scoredPlayerID, boolean multiScoring, Integer objectiveWorth) {
        try {
            int tempX = 0;
            for (java.util.Map.Entry<String, Player> playerEntry : players.entrySet()) {
                Player player = playerEntry.getValue();
                String userID = player.getUserID();
                if (scoredPlayerID.contains(userID)) {
                    String controlID = Mapper.getControlID(player.getColor());
                    if (controlID.contains("null")) {
                        continue;
                    }
                    BufferedImage bufferedImage = resizeImage(ImageIO.read(new File(Mapper.getCCPath(controlID))), 0.55f);
                    Integer vpCount = userVPs.get(player);
                    if (vpCount == null) {
                        vpCount = 0;
                    }
                    if (multiScoring) {
                        int frequency = Collections.frequency(scoredPlayerID, userID);
                        vpCount += frequency * objectiveWorth;
                        for (int i = 0; i < frequency; i++) {
                            graphics.drawImage(bufferedImage, x + tempX, y, null);
                            tempX += scoreTokenWidth;
                        }
                    } else {
                        vpCount += objectiveWorth;
                        graphics.drawImage(bufferedImage, x + tempX, y, null);
                    }
                    userVPs.put(player, vpCount);
                }
                if (!multiScoring) {
                    tempX += scoreTokenWidth;
                }
            }
        } catch (Exception e) {
            LoggerHandler.log("Could not parse custodian CV token file", e);
        }
    }

    private Color getSCColor(int sc, Map map) {
        HashMap<Integer, Boolean> scPlayed = map.getScPlayed();
        if (scPlayed.get(sc) != null) {
            if (scPlayed.get(sc)) {
                return Color.GRAY;
            }
        }
        return getSCColor(sc);
    }

    private Color getSCColor(int sc) {
        return switch (sc) {
            case 1 -> new Color(255, 38, 38);
            case 2 -> new Color(253, 168, 24);
            case 3 -> new Color(247, 237, 28);
            case 4 -> new Color(46, 204, 113);
            case 5 -> new Color(26, 188, 156);
            case 6 -> new Color(52, 152, 171);
            case 7 -> new Color(155, 89, 182);
            case 8 -> new Color(124, 0, 192);
            case 9 -> new Color(251, 96, 213);
            case 10 -> new Color(165, 211, 34);
            default -> Color.WHITE;
        };
    }

    private Color getColor(String color) {
        if (color == null) {
            return Color.WHITE;
        }
        switch (color) {
            case "black":
                return Color.DARK_GRAY;
            case "blue":
                return Color.BLUE;
            case "green":
                return Color.GREEN;
            case "gray":
                return Color.GRAY;
            case "grey":
                return Color.GRAY;
            case "orange":
                return Color.ORANGE;
            case "pink":
                return new Color(246, 153, 205);
            case "purple":
                return new Color(166, 85, 247);
            case "red":
                return Color.RED;
            case "yellow":
                return Color.YELLOW;
            default:
                return Color.WHITE;
        }
    }

    private Color getTechColor(String type) {
        if (type == null) {
            return Color.WHITE;
        }
        return switch (type) {
            case "propulsion" -> new Color(102, 153, 255);
            case "biotic" -> new Color(0, 204, 0);
            case "warfare" -> new Color(204, 0, 0);
            case "cybernetics" -> new Color(230, 230, 0);
            default -> Color.WHITE;
        };
    }


    private void addTile(Tile tile, Map map) {
        try {
            BufferedImage image = ImageIO.read(new File(tile.getTilePath()));
            Point positionPoint = PositionMapper.getTilePosition(tile.getPosition(), map);
            if (positionPoint == null) {
                System.out.println();
            }
            int tileX = positionPoint.x + extraWidth;
            int tileY = positionPoint.y;
            graphics.drawImage(image, tileX, tileY, null);

            graphics.setFont(Storage.getFont20());
            graphics.setColor(Color.WHITE);
            graphics.drawString(tile.getPosition(), tileX + tilePositionPoint.x, tileY + tilePositionPoint.y);

            ArrayList<Rectangle> rectangles = new ArrayList<>();

            Collection<UnitHolder> unitHolders = new ArrayList<>(tile.getUnitHolders().values());
            UnitHolder spaceUnitHolder = unitHolders.stream().filter(unitHolder -> unitHolder.getName().equals(Constants.SPACE)).findFirst().orElse(null);
            if (spaceUnitHolder != null) {
                image = addToken(tile, image, tileX, tileY, spaceUnitHolder);
                unitHolders.remove(spaceUnitHolder);
                unitHolders.add(spaceUnitHolder);
            }
            int degree;
            int degreeChange = 5;
            for (UnitHolder unitHolder : unitHolders) {
                image = addSleeperToken(tile, image, tileX, tileY, unitHolder);
                image = addControl(tile, image, tileX, tileY, unitHolder, rectangles);
            }
            if (spaceUnitHolder != null) {
                image = addCC(tile, image, tileX, tileY, spaceUnitHolder);
            }
            for (UnitHolder unitHolder : unitHolders) {
                degree = 180;
                int radius = unitHolder.getName().equals(Constants.SPACE) ? Constants.SPACE_RADIUS : Constants.RADIUS;
                if (unitHolder != spaceUnitHolder) {
                    image = addPlanetToken(tile, image, tileX, tileY, unitHolder, rectangles);
                }
                image = addUnits(tile, image, tileX, tileY, rectangles, degree, degreeChange, unitHolder, radius);
            }

        } catch (IOException e) {
            LoggerHandler.log("Error drawing tile: " + tile.getTileID(), e);
        }
    }

    private BufferedImage resizeImage(BufferedImage originalImage, float percent) throws IOException {
        int scaledWidth = (int) (originalImage.getWidth() * percent);
        int scaledHeight = (int) (originalImage.getHeight() * percent);
        Image resultingImage = originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_DEFAULT);
        BufferedImage outputImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
        return outputImage;
    }

    private BufferedImage addCC(Tile tile, BufferedImage image, int tileX, int tileY, UnitHolder unitHolder) {
        HashSet<String> ccList = unitHolder.getCCList();
        int deltaX = 0;//ccList.size() * 20;
        int deltaY = 0;//ccList.size() * 20;
        for (String ccID : ccList) {
            String ccPath = tile.getCCPath(ccID);
            if (ccPath == null) {
                LoggerHandler.log("Could not parse cc file for: " + ccID);
                continue;
            }
            try {
                image = resizeImage(ImageIO.read(new File(ccPath)), 0.85f);
            } catch (Exception e) {
                LoggerHandler.log("Could not parse cc file for: " + ccID, e);
            }
            Point centerPosition = unitHolder.getHolderCenterPosition();
            graphics.drawImage(image, tileX + 10 + deltaX, tileY + centerPosition.y - 40 + deltaY, null);
            deltaX += image.getWidth() / 5;
            deltaY += image.getHeight() / 4;
        }
        return image;
    }

    private BufferedImage addControl(Tile tile, BufferedImage image, int tileX, int tileY, UnitHolder unitHolder, ArrayList<Rectangle> rectangles) {
        ArrayList<String> controlList = new ArrayList<>(unitHolder.getControlList());
        PlanetTokenPosition planetTokenPosition = PositionMapper.getPlanetTokenPosition(unitHolder.getName());
        if (planetTokenPosition != null) {
            Point centerPosition = unitHolder.getHolderCenterPosition();
            int xDelta = 0;
            for (String controlID : controlList) {
                if (controlID.contains(Constants.SLEEPER)) {
                    continue;
                }
                String controlPath = tile.getCCPath(controlID);
                if (controlPath == null) {
                    LoggerHandler.log("Could not parse control token file for: " + controlID);
                    continue;
                }
                float scale = 1.00f;
                try {
                    image = resizeImage(ImageIO.read(new File(controlPath)), scale);
                } catch (Exception e) {
                    LoggerHandler.log("Could not parse control token file for: " + controlID, e);
                }
                Point position = planetTokenPosition.getPosition(controlID);
                if (position != null) {
                    graphics.drawImage(image, tileX + position.x, tileY + position.y, null);
                    rectangles.add(new Rectangle(tileX + position.x, tileY + position.y, image.getWidth(), image.getHeight()));
                } else {
                    graphics.drawImage(image, tileX + centerPosition.x + xDelta, tileY + centerPosition.y, null);
                    rectangles.add(new Rectangle(tileX + centerPosition.x + xDelta, tileY + centerPosition.y, image.getWidth(), image.getHeight()));
                    xDelta += 10;
                }
            }
            return image;
        } else {
            return oldFormatPlanetTokenAdd(tile, image, tileX, tileY, unitHolder, controlList);
        }
    }

    private BufferedImage addSleeperToken(Tile tile, BufferedImage image, int tileX, int tileY, UnitHolder unitHolder) {
        Point centerPosition = unitHolder.getHolderCenterPosition();
        ArrayList<String> tokenList = new ArrayList<>(unitHolder.getTokenList());
        tokenList.sort((o1, o2) -> {
            if ((o1.contains(Constants.SLEEPER) || o2.contains(Constants.SLEEPER))) {
                return -1;
            } else if (o1.contains(Constants.DMZ_LARGE) || o2.contains(Constants.DMZ_LARGE)) {
                return 1;
            }
            return o1.compareTo(o2);
        });
        for (String tokenID : tokenList) {
            if (tokenID.contains(Constants.SLEEPER) || tokenID.contains(Constants.DMZ_LARGE) || tokenID.contains(Constants.WORLD_DESTROYED)) {
                String tokenPath = tile.getTokenPath(tokenID);
                if (tokenPath == null) {
                    LoggerHandler.log("Could not sleeper token file for: " + tokenID);
                    continue;
                }
                float scale = 0.85f;
                if (tokenPath.contains(Constants.DMZ_LARGE)) {
                    scale = 0.6f;
                } else if (tokenPath.contains(Constants.WORLD_DESTROYED)) {
                    scale = 0.8f;
                }
                try {
                    image = resizeImage(ImageIO.read(new File(tokenPath)), scale);
                } catch (Exception e) {
                    LoggerHandler.log("Could not parse sleeper token file for: " + tokenID, e);
                }
                Point position = new Point(centerPosition.x - (image.getWidth() / 2), centerPosition.y - (image.getHeight() / 2));
                graphics.drawImage(image, tileX + position.x, tileY + position.y - 10, null);
            }
        }
        return image;
    }

    private BufferedImage addPlanetToken(Tile tile, BufferedImage image, int tileX, int tileY, UnitHolder unitHolder, ArrayList<Rectangle> rectangles) {
        ArrayList<String> tokenList = new ArrayList<>(unitHolder.getTokenList());
        tokenList.sort((o1, o2) -> {
            if ((o1.contains("nanoforge") || o1.contains("titanspn"))) {
                return -1;
            } else if ((o2.contains("nanoforge") || o2.contains("titanspn"))) {
                return -1;
            } else if (o1.contains(Constants.DMZ_LARGE) || o2.contains(Constants.DMZ_LARGE)) {
                return 1;
            }
            return o1.compareTo(o2);
        });
        PlanetTokenPosition planetTokenPosition = PositionMapper.getPlanetTokenPosition(unitHolder.getName());
        if (planetTokenPosition != null) {
            Point centerPosition = unitHolder.getHolderCenterPosition();
            int xDelta = 0;
            for (String tokenID : tokenList) {
                if (tokenID.contains(Constants.SLEEPER) || tokenID.contains(Constants.DMZ_LARGE) || tokenID.contains(Constants.WORLD_DESTROYED)) {
                    continue;
                }
                String tokenPath = tile.getTokenPath(tokenID);
                if (tokenPath == null) {
                    LoggerHandler.log("Could not parse token file for: " + tokenID);
                    continue;
                }
                float scale = 1.00f;
                try {
                    image = resizeImage(ImageIO.read(new File(tokenPath)), scale);
                } catch (Exception e) {
                    LoggerHandler.log("Could not parse control token file for: " + tokenID, e);
                }
                if (tokenPath.contains(Constants.DMZ_LARGE) || tokenPath.contains(Constants.WORLD_DESTROYED)) {
                    graphics.drawImage(image, tileX + centerPosition.x - (image.getWidth() / 2), tileY + centerPosition.y - (image.getHeight() / 2), null);
                } else {
                    Point position = planetTokenPosition.getPosition(tokenID);
                    if (position != null) {
                        graphics.drawImage(image, tileX + position.x, tileY + position.y, null);
                        rectangles.add(new Rectangle(tileX + position.x, tileY + position.y, image.getWidth(), image.getHeight()));
                    } else {
                        graphics.drawImage(image, tileX + centerPosition.x + xDelta, tileY + centerPosition.y, null);
                        rectangles.add(new Rectangle(tileX + centerPosition.x + xDelta, tileY + centerPosition.y, image.getWidth(), image.getHeight()));
                        xDelta += 10;
                    }
                }
            }
            return image;
        } else {
            return oldFormatPlanetTokenAdd(tile, image, tileX, tileY, unitHolder, tokenList);
        }
    }

    private BufferedImage oldFormatPlanetTokenAdd(Tile tile, BufferedImage image, int tileX, int tileY, UnitHolder unitHolder, ArrayList<String> tokenList) {
        int deltaY = 0;
        int offSet = 0;
        Point centerPosition = unitHolder.getHolderCenterPosition();
        int x = tileX + centerPosition.x;
        int y = tileY + centerPosition.y - (tokenList.size() > 1 ? 35 : 0);
        for (String tokenID : tokenList) {
            String tokenPath = tile.getTokenPath(tokenID);
            if (tokenPath == null) {
                LoggerHandler.log("Could not parse token file for: " + tokenID);
                continue;
            }
            try {
                image = resizeImage(ImageIO.read(new File(tokenPath)), 0.85f);
            } catch (Exception e) {
                LoggerHandler.log("Could not parse control token file for: " + tokenID, e);
            }
            graphics.drawImage(image, x - (image.getWidth() / 2), y + offSet + deltaY - (image.getHeight() / 2), null);
            y += image.getHeight();
        }
        return image;
    }

    private BufferedImage addToken(Tile tile, BufferedImage image, int tileX, int tileY, UnitHolder unitHolder) {
        HashSet<String> tokenList = unitHolder.getTokenList();
        Point centerPosition = unitHolder.getHolderCenterPosition();
        int x = tileX;
        int y = tileY;
        int deltaX = 80;
        int deltaY = 0;
        ArrayList<Point> spaceTokenPositions = PositionMapper.getSpaceTokenPositions(tile.getTileID());
        if (spaceTokenPositions.isEmpty()) {
            x = tileX + centerPosition.x;
            y = tileY + centerPosition.y;
        }
        int index = 0;
        for (String tokenID : tokenList) {
            String tokenPath = tile.getTokenPath(tokenID);
            if (tokenPath == null) {
                LoggerHandler.log("Could not parse token file for: " + tokenID);
                continue;
            }
            try {
                float scale = tokenPath.contains(Constants.MIRAGE) ? 1.0f : 0.80f;
                image = resizeImage(ImageIO.read(new File(tokenPath)), scale);
            } catch (Exception e) {
                LoggerHandler.log("Could not parse control token file for: " + tokenID, e);
            }

            if (tokenPath.contains(Constants.MIRAGE)) {
                graphics.drawImage(image, tileX + Constants.MIRAGE_POSITION.x, tileY + Constants.MIRAGE_POSITION.y, null);
            } else if (tokenPath.contains(Constants.SLEEPER)) {
                graphics.drawImage(image, tileX + centerPosition.x - (image.getWidth() / 2), tileY + centerPosition.y - (image.getHeight() / 2), null);
            } else {
                if (spaceTokenPositions.size() > index) {
                    Point point = spaceTokenPositions.get(index);
                    graphics.drawImage(image, x + point.x, y + point.y, null);
                    index++;
                } else {
                    graphics.drawImage(image, x + deltaX, y + deltaY, null);
                    deltaX += 30;
                    deltaY += 30;
                }
            }
        }
        return image;
    }

    private BufferedImage addUnits(Tile tile, BufferedImage image, int tileX, int tileY, ArrayList<Rectangle> rectangles, int degree, int degreeChange, UnitHolder unitHolder, int radius) {
        HashMap<String, Integer> tempUnits = new HashMap<>(unitHolder.getUnits());
        LinkedHashMap<String, Integer> units = new LinkedHashMap<>();

        for (java.util.Map.Entry<String, Integer> entry : tempUnits.entrySet()) {
            String id = entry.getKey();
            //contains mech image
            if (id != null && id.contains("mf")) {
                units.put(id, entry.getValue());
            }
        }
        for (String key : units.keySet()) {
            tempUnits.remove(key);
        }
        units.putAll(tempUnits);
        HashMap<String, Integer> unitDamage = unitHolder.getUnitDamage();
        float scaleOfUnit = 0.80f;
        PlanetTokenPosition planetTokenPosition = PositionMapper.getPlanetTokenPosition(unitHolder.getName());
        BufferedImage dmgImage = null;
        try {
            dmgImage = resizeImage(ImageIO.read(new File(Helper.getDamagePath())), scaleOfUnit);
        } catch (IOException e) {
            LoggerHandler.log("Could not parse damage token file.", e);
        }

        boolean isMirage = unitHolder.getName().equals(Constants.MIRAGE);

        for (java.util.Map.Entry<String, Integer> unitEntry : units.entrySet()) {
            String unitID = unitEntry.getKey();
            Integer unitCount = unitEntry.getValue();

            Integer unitDamageCount = unitDamage.get(unitID);

            Color groupUnitColor = Color.WHITE;
            Integer bulkUnitCount = null;
            if (unitID.startsWith("ylw")) {
                groupUnitColor = Color.BLACK;
            }
            if (unitID.endsWith(Constants.COLOR_FF)) {
                unitID = unitID.replace(Constants.COLOR_FF, Constants.BULK_FF);
                bulkUnitCount = unitCount;
            } else if (unitID.endsWith(Constants.COLOR_GF)) {
                unitID = unitID.replace(Constants.COLOR_GF, Constants.BULK_GF);
                bulkUnitCount = unitCount;
            }


            try {
                image = resizeImage(ImageIO.read(new File(tile.getUnitPath(unitID))), scaleOfUnit);
            } catch (Exception e) {
                LoggerHandler.log("Could not parse unit file for: " + unitID, e);
            }
            if (bulkUnitCount != null && bulkUnitCount > 0) {
                unitCount = 1;
            }


            Point centerPosition = unitHolder.getHolderCenterPosition();

            for (int i = 0; i < unitCount; i++) {
                Point position = planetTokenPosition != null ? planetTokenPosition.getPosition(unitID) : null;
                boolean searchPosition = true;
                int x = 0;
                int y = 0;
                while (searchPosition && position == null) {
                    x = (int) (radius * Math.sin(degree));
                    y = (int) (radius * Math.cos(degree));
                    int possibleX = tileX + centerPosition.x + x - (image.getWidth() / 2);
                    int possibleY = tileY + centerPosition.y + y - (image.getHeight() / 2);
                    BufferedImage finalImage = image;
                    if (rectangles.stream().noneMatch(rectangle -> rectangle.intersects(possibleX, possibleY, finalImage.getWidth(), finalImage.getHeight()))) {
                        searchPosition = false;
                    } else if (degree > 360) {
                        searchPosition = false;
                        degree += 3;//To change degree if we did not find place, might be better placement then
                    }
                    degree += degreeChange;
                    if (!searchPosition) {
                        rectangles.add(new Rectangle(possibleX, possibleY, finalImage.getWidth(), finalImage.getHeight()));
                    }
                }
                int xOriginal = tileX + centerPosition.x + x;
                int yOriginal = tileY + centerPosition.y + y;
                int imageX = position != null ? tileX + position.x : xOriginal - (image.getWidth() / 2);
                int imageY = position != null ? tileY + position.y : yOriginal - (image.getHeight() / 2);
                if (isMirage) {
                    imageX += Constants.MIRAGE_POSITION.x;
                    imageY += Constants.MIRAGE_POSITION.y;
                }
                graphics.drawImage(image, imageX, imageY, null);
                if (bulkUnitCount != null) {
                    graphics.setFont(Storage.getFont26());
                    graphics.setColor(groupUnitColor);
                    int scaledNumberPositionX = (int) (numberPositionPoint.x * scaleOfUnit);
                    int scaledNumberPositionY = (int) (numberPositionPoint.y * scaleOfUnit);
                    graphics.drawString(Integer.toString(bulkUnitCount), imageX + scaledNumberPositionX, imageY + scaledNumberPositionY);
                }

                if (unitDamageCount != null && unitDamageCount > 0 && dmgImage != null) {
                    int imageDmgX = position != null ? tileX + position.x + (image.getWidth() / 2) - (dmgImage.getWidth() / 2) : xOriginal - (dmgImage.getWidth() / 2);
                    int imageDmgY = position != null ? tileY + position.y + (image.getHeight() / 2) - (dmgImage.getHeight() / 2) : yOriginal - (dmgImage.getHeight() / 2);
                    graphics.drawImage(dmgImage, imageDmgX, imageDmgY, null);
                    unitDamageCount--;
                }
            }
        }
        return image;
    }
}
