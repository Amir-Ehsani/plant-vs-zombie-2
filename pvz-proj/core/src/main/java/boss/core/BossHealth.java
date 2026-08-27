package boss.core;

import java.util.Arrays;

public final class BossHealth {
    public enum DamageResult {
        IGNORED,
        DAMAGED,
        SECTION_BROKEN,
        DEFEATED
    }

    private final int[] sectionMaximums;
    private final int maximumHp;
    private int sectionIndex;
    private int sectionHp;
    private int sectionBreakSerial;

    public BossHealth(int... sectionMaximums) {
        if (sectionMaximums == null || sectionMaximums.length != 3) {
            throw new IllegalArgumentException("Boss health must contain exactly three sections.");
        }
        this.sectionMaximums = Arrays.copyOf(sectionMaximums, sectionMaximums.length);
        int total = 0;
        for (int value : this.sectionMaximums) {
            if (value <= 0) {
                throw new IllegalArgumentException("Boss health sections must be positive.");
            }
            total += value;
        }
        maximumHp = total;
        sectionIndex = 0;
        sectionHp = this.sectionMaximums[0];
        sectionBreakSerial = 0;
    }

    public DamageResult damage(int amount) {
        if (amount <= 0 || isDepleted()) {
            return DamageResult.IGNORED;
        }
        sectionHp = Math.max(0, sectionHp - amount);
        if (sectionHp > 0) {
            return DamageResult.DAMAGED;
        }
        if (sectionIndex == sectionMaximums.length - 1) {
            return DamageResult.DEFEATED;
        }
        sectionIndex++;
        sectionHp = sectionMaximums[sectionIndex];
        sectionBreakSerial++;
        return DamageResult.SECTION_BROKEN;
    }

    public int getMaximumHp() {
        return maximumHp;
    }

    public int getCurrentHp() {
        if (isDepleted()) {
            return 0;
        }
        int total = sectionHp;
        for (int i = sectionIndex + 1; i < sectionMaximums.length; i++) {
            total += sectionMaximums[i];
        }
        return total;
    }

    public float getHealthFraction() {
        return maximumHp <= 0 ? 0f : getCurrentHp() / (float) maximumHp;
    }

    public int getCurrentSectionNumber() {
        return Math.min(sectionMaximums.length, sectionIndex + 1);
    }

    public int getCurrentSectionHp() {
        return isDepleted() ? 0 : sectionHp;
    }

    public int getCurrentSectionMaximum() {
        return sectionMaximums[Math.min(sectionIndex, sectionMaximums.length - 1)];
    }

    public int[] getSectionMaximums() {
        return Arrays.copyOf(sectionMaximums, sectionMaximums.length);
    }

    public int getSectionBreakSerial() {
        return sectionBreakSerial;
    }

    public boolean isDepleted() {
        return sectionIndex == sectionMaximums.length - 1 && sectionHp <= 0;
    }
}
