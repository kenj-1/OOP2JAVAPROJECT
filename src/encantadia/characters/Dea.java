package encantadia.characters;

import encantadia.battle.Skill;

public class Dea extends Character {

    public Dea() {




        super(
                "Dea",
                "Daughter of the Northern Winds",
                5000,
                "Born of the northern winds and nurtured by the whispers of the sky, Dea commands the currents " +
                        "with precision and foresight. Calm and observant, she senses disturbances across the realms long " +
                        "before they manifest. The disappearance of Jelian weighs heavily on her, as the winds now carry " +
                        "only silence where once they carried guidance."
        );

        // Skill 1: Wind Slash — no effect
        skills.add(new Skill("Wind Slash", 220, 300, 0,
                Skill.EffectType.NONE, 0, 0));

        // Skill 2: Storm Fury — no effect
        skills.add(new Skill("Storm Fury", 320, 400, 2,
                Skill.EffectType.NONE, 0, 0));

        // Skill 3: Whirlwind — 45% chance to increase opponent's cooldown by 1 turn
        skills.add(new Skill("Whirlwind", 500, 620, 3,
                Skill.EffectType.COOLDOWN_INCREASE, 1, 0.45));
    }
}