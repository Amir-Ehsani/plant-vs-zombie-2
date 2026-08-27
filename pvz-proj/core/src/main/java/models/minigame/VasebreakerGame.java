package models.minigame;

import models.core.plant.Plant;
import models.core.plant.PlantFactory;
import models.core.projectile.Damage;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class VasebreakerGame extends MiniGameSession {
    private enum VaseKind {
        NORMAL,
        PLANT,
        GARGANTUAR
    }

    private enum ContentKind {
        EMPTY,
        PLANT_PACKET,
        ZOMBIE
    }

    private static final class Vase {
        private final VaseKind kind;
        private final ContentKind contentKind;
        private final String contentName;

        private Vase(VaseKind kind, ContentKind contentKind, String contentName) {
            this.kind = kind;
            this.contentKind = contentKind;
            this.contentName = contentName;
        }
    }

    private static final class SeedPacket {
        private final int id;
        private final String plantName;
        private final Position dropPosition;
        private int remainingTicks;

        private SeedPacket(int id, String plantName, Position dropPosition, int remainingTicks) {
            this.id = id;
            this.plantName = plantName;
            this.dropPosition = dropPosition;
            this.remainingTicks = remainingTicks;
        }
    }

    private final Board board;
    private final PlantFactory plantFactory;
    private final ZombieFactory zombieFactory;
    private final Random random;
    private final Map<Position, Vase> vases;
    private final Map<Integer, SeedPacket> packets;
    private final int packetLifeTicks;
    private int nextPacketId;
    private int brokenVases;
    private int expiredPackets;

    public VasebreakerGame(int stage) {
        super(MiniGameType.VASEBREAKER, stage);
        board = new Board();
        plantFactory = new PlantFactory();
        zombieFactory = new ZombieFactory();
        random = new Random(8_100L + stage);
        vases = new LinkedHashMap<>();
        packets = new LinkedHashMap<>();
        packetLifeTicks = 260;
        nextPacketId = 1;
        brokenVases = 0;
        expiredPackets = 0;
        disableMowers();
        initializeVases();
        success("Vasebreaker stage " + stage + " started with " + vases.size() + " vases.");
    }

    public boolean breakVase(Position position) {
        if (!isRunning()) {
            fail("The mini-game is already finished.");
            return false;
        }
        if (position == null || !board.isValidPosition(position)) {
            fail("Vase position is invalid.");
            return false;
        }

        Vase vase = vases.remove(position);
        if (vase == null) {
            fail("There is no unbroken vase at " + position + ".");
            return false;
        }

        brokenVases++;
        revealVase(vase, position);
        evaluateStatus();
        return true;
    }

    public boolean plantPacket(int packetId, Position target) {
        if (!isRunning()) {
            fail("The mini-game is already finished.");
            return false;
        }
        SeedPacket packet = packets.get(packetId);
        if (packet == null) {
            fail("Seed packet " + packetId + " does not exist or has expired.");
            return false;
        }
        if (target == null || !board.isValidPosition(target)) {
            fail("Plant position is invalid.");
            return false;
        }
        if (vases.containsKey(target)) {
            fail("Break the vase at " + target + " before planting there.");
            return false;
        }

        if (isInstantExplosive(packet.plantName)) {
            useExplosivePacket(packet, target);
            packets.remove(packetId);
            evaluateStatus();
            return true;
        }

        Plant plant;
        try {
            plant = plantFactory.createPlant(packet.plantName, target.getX(), target.getY());
        } catch (IllegalArgumentException exception) {
            fail("The packet contains an unknown plant.");
            return false;
        }
        if (!board.placePlant(plant, target)) {
            fail("The plant cannot be placed at " + target + ".");
            return false;
        }

        packets.remove(packetId);
        success("Seed packet " + packetId + " planted " + packet.plantName + " at " + target + ".");
        return true;
    }

    public String renderPackets() {
        if (packets.isEmpty()) {
            return "No usable seed packets are on the lawn.";
        }
        StringBuilder builder = new StringBuilder("Seed packets:\n");
        for (SeedPacket packet : packets.values()) {
            builder.append('#').append(packet.id)
                    .append(" | ").append(packet.plantName)
                    .append(" | dropped at ").append(packet.dropPosition)
                    .append(" | expires in ").append(packet.remainingTicks)
                    .append(" ticks\n");
        }
        return builder.toString().trim();
    }

    @Override
    protected void onTick() {
        updatePackets();
        board.updateTicks();
    }

    @Override
    protected void evaluateStatus() {
        if (!isRunning()) {
            return;
        }
        if (board.hasBrainBeenEaten()) {
            markLost("A zombie reached the house. Vasebreaker was lost.");
            return;
        }
        if (vases.isEmpty() && board.getActiveZombieCount() == 0) {
            markWon("All vases were broken and all released zombies were defeated.");
        }
    }

    @Override
    public String renderMap() {
        Map<Position, String> overlays = new LinkedHashMap<>();
        for (Map.Entry<Position, Vase> entry : vases.entrySet()) {
            overlays.put(entry.getKey(), vaseSymbol(entry.getValue()));
        }
        for (SeedPacket packet : packets.values()) {
            overlays.putIfAbsent(packet.dropPosition, "S" + Math.min(9, packet.id));
        }
        return renderBoard(board, overlays);
    }

    @Override
    public String renderStatus() {
        return compactStatus()
                + "\nremaining vases=" + vases.size()
                + "\nbroken vases=" + brokenVases
                + "\nactive zombies=" + board.getActiveZombieCount()
                + "\navailable packets=" + packets.size()
                + "\nexpired packets=" + expiredPackets;
    }

    @Override
    public String renderHelp() {
        return """
                Vasebreaker commands
                break vase -l <x, y>
                show packets
                plant packet -i <id> -l <x, y>
                show map
                show status
                advance time -t <count> ticks
                """.trim();
    }

    public Board getBoard() {
        return board;
    }

    public List<VaseView> getVases() {
        List<VaseView> result = new ArrayList<>();
        for (Map.Entry<Position, Vase> entry : vases.entrySet()) {
            result.add(new VaseView(entry.getKey(), entry.getValue().kind.name()));
        }
        return Collections.unmodifiableList(result);
    }

    public List<SeedPacketView> getSeedPackets() {
        List<SeedPacketView> result = new ArrayList<>();
        for (SeedPacket packet : packets.values()) {
            result.add(new SeedPacketView(packet.id, packet.plantName, packet.dropPosition, packet.remainingTicks));
        }
        return Collections.unmodifiableList(result);
    }

    public record VaseView(Position position, String kind) {
    }

    public record SeedPacketView(int id, String plantName, Position dropPosition, int remainingTicks) {
    }

    private void revealVase(Vase vase, Position position) {
        if (vase.contentKind == ContentKind.EMPTY) {
            success("The vase at " + position + " was empty.");
            return;
        }
        if (vase.contentKind == ContentKind.PLANT_PACKET) {
            SeedPacket packet = new SeedPacket(nextPacketId++, vase.contentName, position, packetLifeTicks);
            packets.put(packet.id, packet);
            success("The vase dropped seed packet #" + packet.id + " for " + packet.plantName + ".");
            return;
        }

        try {
            Zombie zombie = zombieFactory.createZombie(vase.contentName, position.getX(), position.getY());
            Tile tile = board.getTileAt(position);
            tile.addZombie(zombie);
            success("The vase released " + zombie.getName() + " at " + position + ".");
        } catch (IllegalArgumentException exception) {
            fail("The vase contained an unknown zombie type.");
        }
    }

    private void updatePackets() {
        List<Integer> expiredIds = new ArrayList<>();
        for (SeedPacket packet : packets.values()) {
            packet.remainingTicks--;
            if (packet.remainingTicks <= 0) {
                expiredIds.add(packet.id);
            }
        }
        for (Integer id : expiredIds) {
            packets.remove(id);
            expiredPackets++;
        }
    }

    private void initializeVases() {
        List<Position> positions = vasePositions();
        List<Vase> contents = vaseContents(positions.size());
        Collections.shuffle(positions, random);
        Collections.shuffle(contents, random);
        for (int index = 0; index < contents.size() && index < positions.size(); index++) {
            vases.put(positions.get(index), contents.get(index));
        }
    }

    private List<Position> vasePositions() {
        int firstColumn = getStage() == 3 ? 3 : 4;
        List<Position> positions = new ArrayList<>();
        for (int x = firstColumn; x <= board.getWidth(); x++) {
            for (int y = 1; y <= board.getHeight(); y++) {
                positions.add(new Position(x, y));
            }
        }
        return positions;
    }

    private List<Vase> vaseContents(int total) {
        int plantVases = getStage() == 3 ? 4 : 3;
        int gargantuarVases = getStage() == 1 ? 0 : getStage() == 2 ? 1 : 2;
        int normalVases = total - plantVases - gargantuarVases;
        int normalPlantPackets = getStage() == 1 ? 10 : getStage() == 2 ? 9 : 10;
        int normalZombies = getStage() == 1 ? 9 : getStage() == 2 ? 12 : 16;
        int emptyVases = Math.max(0, normalVases - normalPlantPackets - normalZombies);

        List<Vase> result = new ArrayList<>();
        List<String> packetPlants = packetPlantNames(plantVases + normalPlantPackets);
        for (int index = 0; index < plantVases; index++) {
            result.add(new Vase(VaseKind.PLANT, ContentKind.PLANT_PACKET, packetPlants.remove(0)));
        }
        for (int index = 0; index < gargantuarVases; index++) {
            result.add(new Vase(VaseKind.GARGANTUAR, ContentKind.ZOMBIE, "Gargantuar"));
        }
        for (int index = 0; index < normalPlantPackets; index++) {
            result.add(new Vase(VaseKind.NORMAL, ContentKind.PLANT_PACKET, packetPlants.remove(0)));
        }
        for (int index = 0; index < normalZombies; index++) {
            result.add(new Vase(VaseKind.NORMAL, ContentKind.ZOMBIE, randomZombieName()));
        }
        for (int index = 0; index < emptyVases; index++) {
            result.add(new Vase(VaseKind.NORMAL, ContentKind.EMPTY, ""));
        }
        while (result.size() < total) {
            result.add(new Vase(VaseKind.NORMAL, ContentKind.EMPTY, ""));
        }
        return result;
    }

    private List<String> packetPlantNames(int count) {
        List<String> names = new ArrayList<>();
        if (getStage() == 1) {
            Collections.addAll(names,
                    "Peashooter", "Peashooter", "Repeater", "Snow Pea", "Wall-nut",
                    "Cherry Bomb", "Peashooter", "Repeater", "Wall-nut", "Snow Pea",
                    "Peashooter", "Cherry Bomb", "Wall-nut");
        } else if (getStage() == 2) {
            Collections.addAll(names,
                    "Peashooter", "Repeater", "Snow Pea", "Wall-nut", "Cherry Bomb",
                    "Jalapeno", "Repeater", "Threepeater", "Peashooter", "Wall-nut",
                    "Snow Pea", "Repeater", "Tall-nut");
        } else {
            Collections.addAll(names,
                    "Peashooter", "Repeater", "Snow Pea", "Wall-nut", "Tall-nut",
                    "Cherry Bomb", "Jalapeno", "Threepeater", "Repeater", "Threepeater",
                    "Snow Pea", "Tall-nut", "Cherry Bomb", "Wall-nut");
        }
        while (names.size() < count) {
            names.add("Peashooter");
        }
        Collections.shuffle(names, random);
        return names;
    }

    private boolean isInstantExplosive(String plantName) {
        return "Cherry Bomb".equalsIgnoreCase(plantName)
                || "Jalapeno".equalsIgnoreCase(plantName);
    }

    private void useExplosivePacket(SeedPacket packet, Position target) {
        if ("Jalapeno".equalsIgnoreCase(packet.plantName)) {
            int hitCount = 0;
            for (Zombie zombie : new ArrayList<>(board.getAllZombies())) {
                if ((int) Math.round(zombie.getY()) == target.getY()) {
                    zombie.takeDamage(new Damage(1800, "jalapeno packet"));
                    hitCount++;
                }
            }
            board.removeDeadEntities();
            success("Jalapeno packet burned row " + target.getY() + " and hit " + hitCount + " zombies.");
            return;
        }

        int hitCount = 0;
        for (Zombie zombie : new ArrayList<>(board.getAllZombies())) {
            if (Math.abs(zombie.getX() - target.getX()) <= 1 && Math.abs(zombie.getY() - target.getY()) <= 1) {
                zombie.takeDamage(new Damage(1800, "cherry bomb packet"));
                hitCount++;
            }
        }
        board.removeDeadEntities();
        success("Cherry Bomb packet exploded at " + target + " and hit " + hitCount + " zombies.");
    }

    private String randomZombieName() {
        String[][] pools = {
                {"Default", "cone head", "Imp"},
                {"Default", "cone head", "bucket head", "Explorer", "Imp"},
                {"cone head", "bucket head", "brick head", "Explorer", "Hunter", "Imp"}
        };
        String[] pool = pools[getStage() - 1];
        return pool[random.nextInt(pool.length)];
    }

    private String vaseSymbol(Vase vase) {
        if (vase.kind == VaseKind.PLANT) {
            return "VP";
        }
        if (vase.kind == VaseKind.GARGANTUAR) {
            return "VG";
        }
        return "V?";
    }

    private void disableMowers() {
        for (Lane lane : board.getLanes()) {
            lane.getLawnMower().disable();
        }
    }
}
