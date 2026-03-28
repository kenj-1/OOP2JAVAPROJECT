package encantadia.characters;

import encantadia.battle.skill.Skill;

import java.util.ArrayList;
import java.util.List;

public abstract class Character {

    protected String name;
    protected String title;
    protected String backstory;

    protected int maxHP;
    protected int currentHP;

    protected List<Skill> skills;

    public Character(String name, String title, int maxHP, String backstory) {
        this.name      = name;
        this.title     = title;
        this.maxHP     = maxHP;
        this.currentHP = maxHP;
        this.backstory = backstory;
        this.skills    = new ArrayList<>();
    }

    public void takeDamage(int damage) {
        currentHP -= damage;
        if (currentHP < 0) currentHP = 0;
    }

    public void heal(int amount) {
        currentHP += amount;
        if (currentHP > maxHP) currentHP = maxHP;
    }

    public boolean isAlive() {
        return currentHP > 0;
    }

    // ── Getters ──────────────────────────────────────────────

    public List<Skill> getSkills()  { return skills; }
    public String getName()         { return name; }
    public String getTitle()        { return title; }
    public String getBackstory()    { return backstory; }
    public int getCurrentHP()       { return currentHP; }
    public int getMaxHP()           { return maxHP; }   // ← required by TurnManager

}