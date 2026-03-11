package encantadia.battle.result;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable-ish result object returned by TurnManager.executeSkill().
 *
 * BattleFrame reads this to update the UI: health bars, log messages,
 * cooldown indicators, and turn flow (e.g. turn steal).
 */
public class TurnResult {

    private final String attackerName;
    private final String skillUsed;

    private final int baseDamageOrHeal;     // the rolled value before any modifiers
    private final int totalDamageDealt;     // final damage after reductions, for UI display
    private final int totalHealApplied;     // if Adamus or Tera heal effect, amount healed
    private final int recoilDamage;         // damage the attacker took from RECOIL (0 if none)
    private final int extraDamage;          // bonus damage from EXTRA_DAMAGE effect (0 if none)

    private final boolean effectTriggered;  // did the skill's secondary effect fire?
    private final String effectDescription; // human-readable effect message for the battle log
    private final boolean turnStolen;       // true = attacker gets another turn immediately
    private final boolean targetDefeated;   // true = target HP reached 0

    // Battle log lines accumulated during execution (for BattleFrame to display)
    private final List<String> logMessages;

    // Package-private constructor — built by TurnManager via Builder
    private TurnResult(Builder b) {
        this.attackerName       = b.attackerName;
        this.skillUsed          = b.skillUsed;
        this.baseDamageOrHeal   = b.baseDamageOrHeal;
        this.totalDamageDealt   = b.totalDamageDealt;
        this.totalHealApplied   = b.totalHealApplied;
        this.recoilDamage       = b.recoilDamage;
        this.extraDamage        = b.extraDamage;
        this.effectTriggered    = b.effectTriggered;
        this.effectDescription  = b.effectDescription;
        this.turnStolen         = b.turnStolen;
        this.targetDefeated     = b.targetDefeated;
        this.logMessages        = List.copyOf(b.logMessages);
    }



    // ── Getters ──────────────────────────────────────────────

    public String getAttackerName()      { return attackerName; }
    public String getSkillUsed()         { return skillUsed; }
    public int getBaseDamageOrHeal()     { return baseDamageOrHeal; }
    public int getTotalDamageDealt()     { return totalDamageDealt; }
    public int getTotalHealApplied()     { return totalHealApplied; }
    public int getRecoilDamage()         { return recoilDamage; }
    public int getExtraDamage()          { return extraDamage; }
    public boolean isEffectTriggered()   { return effectTriggered; }
    public String getEffectDescription() { return effectDescription; }
    public boolean isTurnStolen()        { return turnStolen; }
    public boolean isTargetDefeated()    { return targetDefeated; }
    public List<String> getLogMessages() { return logMessages; }

    // ── Builder ───────────────────────────────────────────────

    public static class Builder {

        private String attackerName    = "";
        private String skillUsed       = "";
        private int baseDamageOrHeal   = 0;
        private int totalDamageDealt   = 0;
        private int totalHealApplied   = 0;
        private int recoilDamage       = 0;
        private int extraDamage        = 0;
        private boolean effectTriggered   = false;
        private String effectDescription  = "";
        private boolean turnStolen        = false;
        private boolean targetDefeated    = false;
        private final List<String> logMessages = new ArrayList<>();

        public Builder attackerName(String v)      { this.attackerName = v;      return this; }
        public Builder skillUsed(String v)         { this.skillUsed = v;         return this; }
        public Builder baseDamageOrHeal(int v)     { this.baseDamageOrHeal = v;  return this; }
        public Builder totalDamageDealt(int v)     { this.totalDamageDealt = v;  return this; }
        public Builder totalHealApplied(int v)     { this.totalHealApplied = v;  return this; }
        public Builder recoilDamage(int v)         { this.recoilDamage = v;      return this; }
        public Builder extraDamage(int v)          { this.extraDamage = v;       return this; }
        public Builder effectTriggered(boolean v)  { this.effectTriggered = v;   return this; }
        public Builder effectDescription(String v) { this.effectDescription = v; return this; }
        public Builder turnStolen(boolean v)       { this.turnStolen = v;        return this; }
        public Builder targetDefeated(boolean v)   { this.targetDefeated = v;    return this; }
        public Builder log(String msg)             { this.logMessages.add(msg);  return this; }

        public TurnResult build() { return new TurnResult(this); }
    }
}