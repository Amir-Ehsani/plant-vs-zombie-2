package models.engine.combat;

import models.core.plant.Plant;
import models.core.projectile.Damage;
import models.core.zombie.Armor;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.core.zombie.ZombieType;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.board.TileType;
import models.engine.events.GameEvent;
import models.entities.LawnMower;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;


abstract class LaneCombatState {
    protected static final double MELEE_RANGE = 1.35;
    protected static final double GLOBAL_ZOMBIE_SPEED_SCALE = 0.055;
    protected static final int TICKS_PER_SECOND = 10;
    protected static final int DEFAULT_CHILL_TICKS = 3 * TICKS_PER_SECOND;
    protected static final int DEFAULT_FREEZE_TICKS = 5 * TICKS_PER_SECOND;
    protected static final int DEFAULT_POISON_TICKS = 5 * TICKS_PER_SECOND;
    protected static final int DEFAULT_BUTTER_TICKS = 3 * TICKS_PER_SECOND;
    protected static final int DEFAULT_POTATO_ARM_TICKS = 14 * TICKS_PER_SECOND;
    protected static final int DEFAULT_PRIMAL_POTATO_ARM_TICKS = 5 * TICKS_PER_SECOND;
    protected static final int DEFAULT_SHROOM_LIFESPAN_TICKS = 60 * TICKS_PER_SECOND;
    protected static final int DEFAULT_CHOMPER_DIGEST_TICKS = 40 * TICKS_PER_SECOND;

    protected static final class ZombieRuntimeState {
        protected int frozenTicks;
        protected int chilledTicks;
        protected int poisonTicks;
        protected int poisonDamage;
        protected String poisonSourcePlantName;
        protected String poisonSourcePlantCategory;
        protected int butterTicks;
        protected int electricStrikeTicks;
        protected boolean hypnotized;
        protected int pendingLaneShift;
        protected int ageTicks;
        protected int lastDamageRevision;
        protected boolean gargantuarImpThrown;
        protected boolean allstarCharging = true;
        protected boolean prospectorReversed;
        protected boolean prospectorDynamiteExtinguished;
        protected boolean torchLit = true;
        protected int turquoiseChannelTicks;
        protected int jugglerSpinTicks;
        protected boolean frontObjectObserved;
        protected boolean frontObjectBrokenHandled;
        protected boolean deathHandled;
    }

    protected static final class PlantRuntimeState {
        protected int ageTicks;
        protected int digestTicks;
        protected int shotCycle;
        protected int bowlingBlueRechargeTicks;
        protected int bowlingOrangeRechargeTicks;
        protected int bowlingShotTier;
        protected int crushCount;
        protected boolean hasAttacked;
        protected boolean actionPending;
        protected boolean deathEffectHandled;
    }

    protected final Board board;
    protected final Random random;
    protected final Map<Zombie, ZombieRuntimeState> zombieStates;
    protected final Map<Plant, PlantRuntimeState> plantStates;
    protected final Map<String, Integer> familyBoostTicks;
    protected final Set<Zombie> processedZombiesThisBoardTick;
    private final List<PendingCombatAction> pendingCombatActions;
    private int combatTick;


    protected LaneCombatState() {
        this(null, new Random());
    }

    protected LaneCombatState(Board board) {
        this(board, new Random());
    }

    protected LaneCombatState(Board board, Random random) {
        this.board = board;
        this.random = random == null ? new Random() : random;
        this.zombieStates = new IdentityHashMap<>();
        this.plantStates = new IdentityHashMap<>();
        this.familyBoostTicks = new LinkedHashMap<>();
        this.processedZombiesThisBoardTick = Collections.newSetFromMap(new IdentityHashMap<>());
        this.pendingCombatActions = new ArrayList<>();
        this.combatTick = 0;
    }


    public void beginBoardTick() {
        combatTick++;
        runReadyCombatActions();
        processedZombiesThisBoardTick.clear();
        for (String category : new ArrayList<>(familyBoostTicks.keySet())) {
            int remaining = familyBoostTicks.get(category) - 1;
            if (remaining <= 0) {
                familyBoostTicks.remove(category);
            } else {
                familyBoostTicks.put(category, remaining);
            }
        }
        if (board != null) {
            for (Zombie zombie : board.getAllZombies()) {
                if (zombie == null || !zombie.isAlive()) {
                    continue;
                }
                String name = normalizeText(zombie.getName());
                if (name.equals("snorkel")) {
                    Tile tile = board.getTileContainingZombie(zombie);
                    zombie.setSubmerged(tile != null
                        && tile.getTileType() == TileType.WATER
                        && !tile.hasPlant());
                }
            }
        }
        for (Map.Entry<Zombie, ZombieRuntimeState> entry : new ArrayList<>(zombieStates.entrySet())) {
            Zombie zombie = entry.getKey();
            if (zombie != null && !zombie.isAlive()) {
                handleSpecialZombieDeath(zombie, entry.getValue());
            }
        }
        zombieStates.keySet().removeIf(zombie -> zombie == null || !zombie.isAlive());
        plantStates.keySet().removeIf(plant -> plant == null || !plant.isAlive());
    }


    protected void scheduleCombatAction(int delayTicks, Runnable action) {
        if (action == null) {
            return;
        }
        if (delayTicks <= 0) {
            action.run();
            return;
        }
        pendingCombatActions.add(new PendingCombatAction(combatTick + delayTicks, action));
    }

    private void runReadyCombatActions() {
        if (pendingCombatActions.isEmpty()) {
            return;
        }
        List<PendingCombatAction> ready = new ArrayList<>();
        for (PendingCombatAction pending : pendingCombatActions) {
            if (pending.dueTick <= combatTick) {
                ready.add(pending);
            }
        }
        pendingCombatActions.removeAll(ready);
        for (PendingCombatAction pending : ready) {
            pending.action.run();
        }
    }

    private static final class PendingCombatAction {
        private final int dueTick;
        private final Runnable action;

        private PendingCombatAction(int dueTick, Runnable action) {
            this.dueTick = dueTick;
            this.action = action;
        }
    }

    public void applyFreeze(Zombie zombie, int ticks) {
        if (zombie == null || !zombie.isAlive() || ticks <= 0
            || zombie.isIceImmune()) {
            return;
        }
        ZombieRuntimeState state = stateOf(zombie);
        state.frozenTicks = Math.max(state.frozenTicks, ticks);
        zombie.setCurrentSpeed(0);
    }

    public void applyChill(Zombie zombie, int ticks) {
        if (zombie == null || !zombie.isAlive() || ticks <= 0
            || zombie.isIceImmune()) {
            return;
        }
        ZombieRuntimeState state = stateOf(zombie);
        state.chilledTicks = Math.max(state.chilledTicks, ticks);
    }

    public void applyPoison(Zombie zombie, int damagePerTick, int ticks) {
        applyPoison(zombie, damagePerTick, ticks, null);
    }

    protected void applyPoison(Zombie zombie, int damagePerTick, int ticks, Plant source) {
        if (zombie == null || !zombie.isAlive() || damagePerTick <= 0 || ticks <= 0) {
            return;
        }

        ZombieRuntimeState state = stateOf(zombie);
        state.poisonDamage = Math.max(state.poisonDamage, damagePerTick);
        state.poisonTicks = Math.max(state.poisonTicks, ticks);

        if (source != null) {
            state.poisonSourcePlantName = source.getName();
            state.poisonSourcePlantCategory = source.getType() == null ? "" : source.getType().getCategory();
        }
    }

    public void applyButterStun(Zombie zombie, int ticks) {
        if (zombie == null || !zombie.isAlive() || ticks <= 0) {
            return;
        }
        ZombieRuntimeState state = stateOf(zombie);
        state.butterTicks = Math.max(state.butterTicks, ticks);
        zombie.setCurrentSpeed(0);
    }

    protected void markElectricStrike(Zombie zombie, int ticks) {
        if (zombie == null || !zombie.isAlive() || ticks <= 0) {
            return;
        }
        ZombieRuntimeState state = stateOf(zombie);
        state.electricStrikeTicks = Math.max(state.electricStrikeTicks, ticks);
    }

    public void hypnotize(Zombie zombie) {
        if (zombie == null || !zombie.isAlive()) {
            return;
        }
        stateOf(zombie).hypnotized = true;
    }

    public boolean isHypnotized(Zombie zombie) {
        ZombieRuntimeState state = zombieStates.get(zombie);
        return state != null && state.hypnotized;
    }

    public List<String> getActiveEffects(Zombie zombie) {
        ZombieRuntimeState state = zombieStates.get(zombie);
        if (state == null || zombie == null || !zombie.isAlive()) {
            return Collections.emptyList();
        }
        List<String> effects = new ArrayList<>();
        if (state.frozenTicks > 0) {
            effects.add("frozen(" + state.frozenTicks + " ticks)");
        }
        if (state.chilledTicks > 0) {
            effects.add("chilled(" + state.chilledTicks + " ticks)");
        }
        if (state.poisonTicks > 0) {
            effects.add("poisoned(" + state.poisonTicks + " ticks, "
                + state.poisonDamage + " damage/tick)");
        }
        if (state.butterTicks > 0) {
            effects.add("buttered(" + state.butterTicks + " ticks)");
        }
        if (state.electricStrikeTicks > 0) {
            effects.add("electric-strike(" + state.electricStrikeTicks + " ticks)");
        }
        if (state.hypnotized) {
            effects.add("hypnotized");
        }
        return Collections.unmodifiableList(effects);
    }

    public void activateFamilyBoost(String category, int ticks) {
        String normalized = normalizeText(category);
        if (normalized.isEmpty() || ticks <= 0) {
            return;
        }
        familyBoostTicks.put(normalized,
            Math.max(familyBoostTicks.getOrDefault(normalized, 0), ticks));
    }

    public boolean isFamilyBoosted(String category) {
        return familyBoostTicks.getOrDefault(normalizeText(category), 0) > 0;
    }

    protected int clampX(Lane lane, Zombie zombie) {
        return Math.max(1, Math.min(lane.getWidth(), (int) Math.ceil(zombie.getX())));
    }

    protected Tile tileForZombie(Lane lane, Zombie zombie) {
        if (lane == null || zombie == null || zombie.getX() > lane.getWidth()) {
            return null;
        }
        return lane.getTileAt(clampX(lane, zombie));
    }

    protected List<Zombie> collectLivingZombies(Lane lane) {
        List<Zombie> zombies = new ArrayList<>();
        Set<Zombie> seen = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Tile tile : lane.getTiles()) {
            for (Zombie zombie : tile.getZombies()) {
                if (zombie != null && zombie.isAlive() && seen.add(zombie)) {
                    zombies.add(zombie);
                }
            }
        }
        return zombies;
    }

    protected void consumePlant(Tile tile, Plant plant, Set<Plant> consumedPlants) {
        if (tile.removePlant(plant)) {
            consumedPlants.add(plant);
            plantStates.remove(plant);
        }
    }

    protected int effectiveDamage(Plant plant, int baseDamage) {
        int damage = Math.max(baseDamage, plant.getAttackDamage());
        return damage * Math.max(1, plant.getPlantFoodDamageMultiplier());
    }

    protected boolean isFirePlant(Plant plant) {
        if (plant == null || plant.getType() == null) {
            return false;
        }
        String tags = normalizeText(plant.getType().getTags());
        if (tags.contains("fire")) {
            return true;
        }
        String name = normalizeText(plant.getName());
        return name.contains("fire")
            || name.contains("pepper")
            || name.contains("jalapeno")
            || name.contains("torchwood")
            || name.contains("wasabi")
            || name.contains("hot potato");
    }

    protected String resolveDamageType(Plant plant) {
        String category = normalizeCategory(plant);
        if (category.equals("strike through")) {
            return "piercing";
        }
        if (category.equals("homing")) {
            return "homing";
        }
        if (category.equals("lobber")) {
            return "lobber";
        }
        if (category.equals("melee")) {
            return "melee";
        }
        return "normal";
    }

    protected ZombieRuntimeState stateOf(Zombie zombie) {
        return zombieStates.computeIfAbsent(zombie, ignored -> new ZombieRuntimeState());
    }

    protected PlantRuntimeState plantStateOf(Plant plant) {
        return plantStates.computeIfAbsent(plant, ignored -> new PlantRuntimeState());
    }

    protected String normalizeCategory(Plant plant) {
        if (plant == null || plant.getType() == null) {
            return "";
        }
        return normalizeText(plant.getType().getCategory());
    }

    protected String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
            .toLowerCase(Locale.ROOT)
            .replace('-', ' ')
            .replace('_', ' ')
            .replaceAll("\\s+", " ");
    }

    protected abstract void handleSpecialZombieDeath(
        Zombie zombie,
        ZombieRuntimeState state
    );

    protected abstract void damageArea(
        Position center,
        int xRadius,
        int yRadius,
        int damage,
        String damageType,
        Plant source,
        Zombie excluded
    );

}
