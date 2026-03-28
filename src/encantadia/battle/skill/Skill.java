package encantadia.battle.skill;

import java.util.Random;

public class Skill {

    // ── Enums ─────────────────────────────────────────────────

    public enum SkillType {
        DAMAGE,
        HEAL
    }

    public enum EffectType {
        NONE,
        DAMAGE_REDUCTION,
        HEAL,
        EXTRA_DAMAGE,
        TURN_STEAL,
        COOLDOWN_INCREASE,
        COOLDOWN_REDUCTION,
        RECOIL
    }

    // ── Fields ────────────────────────────────────────────────

    private final String     name;
    private final int        minDamage;
    private final int        maxDamage;
    private final SkillType  skillType;
    private final EffectType effectType;
    private final double     effectValue;
    private final double     procChance;

    private int cooldown;
    private int currentCooldown;

    private static final Random random = new Random();

    // ── Constructor A: 7 args (no SkillType — defaults to DAMAGE) ──

    public Skill(String name,
                 int minDamage, int maxDamage,
                 int cooldown,
                 EffectType effectType,
                 double effectValue, double procChance) {

        this(name, minDamage, maxDamage, cooldown,
                SkillType.DAMAGE, effectType, effectValue, procChance);
    }

    // ── Constructor B: 8 args (explicit SkillType) ────────────

    public Skill(String name,
                 int minDamage, int maxDamage,
                 int cooldown,
                 SkillType skillType,
                 EffectType effectType,
                 double effectValue, double procChance) {

        this.name        = name;
        this.minDamage   = minDamage;
        this.maxDamage   = maxDamage;
        this.cooldown    = cooldown;
        this.skillType   = skillType;
        this.effectType  = effectType;
        this.effectValue = effectValue;
        this.procChance  = procChance;
        this.currentCooldown = 0;
    }

    // ── Damage roll — called by TurnManager ──────────────────

    public int rollValue() {
        if (minDamage == maxDamage) return minDamage;
        return minDamage + random.nextInt(maxDamage - minDamage + 1);
    }

    // ── Proc check — called by TurnManager ───────────────────

    public boolean effectTriggered() {
        if (procChance >= 1.0) return true;
        if (procChance <= 0.0) return false;
        return random.nextDouble() < procChance;
    }

    // ── Cooldown logic ────────────────────────────────────────

    public boolean isReady() {
        return currentCooldown == 0;
    }

    public void triggerCooldown() {
        currentCooldown = cooldown;
    }

    public void reduceCooldown(int amount) {
        currentCooldown = Math.max(0, currentCooldown - amount);
    }

    public void tickCooldown() {
        if (currentCooldown > 0) currentCooldown--;
    }

    // ── Getters ───────────────────────────────────────────────

    public String     getName()           { return name; }
    public int        getMinDamage()      { return minDamage; }
    public int        getMaxDamage()      { return maxDamage; }
    public int        getCooldown()       { return cooldown; }
    public int        getCurrentCooldown(){ return currentCooldown; }
    public SkillType  getSkillType()      { return skillType; }
    public EffectType getEffectType()     { return effectType; }
    public double     getEffectValue()    { return effectValue; }
    public double     getProcChance()     { return procChance; }
}