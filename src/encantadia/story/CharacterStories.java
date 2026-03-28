package encantadia.story;

import encantadia.battle.skill.Skill;
import encantadia.characters.Character;

import java.util.ArrayList;
import java.util.List;

/**
 * CharacterStories
 *
 * Builds the story paragraphs shown in BackstoryShowcase for each
 * character and their matched enemy.
 *
 * IMPORTANT: Each element in the returned String[] must be ONE <p>...</p>
 * block — exactly the same format that GameStories uses.  BackstoryShowcase
 * paginates by counting array elements (PARAGRAPHS_PER_PAGE = 10), so
 * returning one giant concatenated string breaks pagination AND the
 * drop-cap / animation logic.
 */
public class CharacterStories {

    // ══════════════════════════════════════════════════════════
    //  Character story — backstory sentences + skills page
    // ══════════════════════════════════════════════════════════
    public static String[] getCharacterStory(Character character) {
        List<String> parts = new ArrayList<>();

        // ── Lore: one <p> per sentence ──────────────────────────
        String backstory = character.getBackstory();
        if (backstory != null && !backstory.isBlank()) {
            String[] sentences = backstory.trim().split("(?<=[.!?])\\s+");
            for (String s : sentences) {
                String t = s.trim();
                if (!t.isEmpty()) {
                    parts.add("<p>" + escapeHtml(t) + "</p>");
                }
            }
        }

        // ── Abilities header ─────────────────────────────────────
        parts.add("<p><strong>&#x2726; Abilities</strong></p>");

        // ── One <p> per skill ────────────────────────────────────
        int idx = 1;
        for (Skill skill : character.getSkills()) {
            parts.add("<p>"
                    + "<strong>" + idx + ". " + escapeHtml(skill.getName()) + "</strong>"
                    + " &mdash; "
                    + skillTypeLine(skill)
                    + (skill.getCooldown() > 0
                    ? " &nbsp;|&nbsp; Cooldown: " + skill.getCooldown() + " turn(s)"
                    : " &nbsp;|&nbsp; No cooldown")
                    + effectLine(skill)
                    + "</p>");
            idx++;
        }

        return parts.toArray(new String[0]);
    }

    // ══════════════════════════════════════════════════════════
    //  Enemy story — backstory sentences + known-abilities page
    // ══════════════════════════════════════════════════════════
    public static String[] getEnemyStory(Character enemy) {
        if (enemy == null) return defaultEnemyStory();

        String backstory = enemy.getBackstory();
        if (backstory == null || backstory.isBlank()) return defaultEnemyStory();

        List<String> parts = new ArrayList<>();

        // ── Lore ────────────────────────────────────────────────
        String[] sentences = backstory.trim().split("(?<=[.!?])\\s+");
        for (String s : sentences) {
            String t = s.trim();
            if (!t.isEmpty()) {
                parts.add("<p>" + escapeHtml(t) + "</p>");
            }
        }

        // ── Known abilities header ───────────────────────────────
        parts.add("<p><strong>&#x26A0; Known Abilities</strong></p>");

        int idx = 1;
        for (Skill skill : enemy.getSkills()) {
            parts.add("<p>"
                    + "<strong>" + idx + ". " + escapeHtml(skill.getName()) + "</strong>"
                    + " &mdash; "
                    + skillTypeLine(skill)
                    + effectLine(skill)
                    + "</p>");
            idx++;
        }

        // ── Closing warning ──────────────────────────────────────
        parts.add("<p><em>Prepare yourself. Once you enter the Gem Void, "
                + "there is no guarantee of what you will find "
                + "&mdash; or what will find you.</em></p>");

        return parts.toArray(new String[0]);
    }

    // ── Title helpers ─────────────────────────────────────────

    public static String getCharacterTitle(Character character) {
        return character.getName() + " \u2014 " + character.getTitle();
    }

    public static String getEnemyTitle(Character enemy) {
        if (enemy == null) return "A Creature of the Gem Void";
        return enemy.getName() + " \u2014 " + enemy.getTitle();
    }

    // ── Private helpers ───────────────────────────────────────

    /**
     * Shows "Damage: min–max" or "Heals: min–max" based on SkillType.
     */
    private static String skillTypeLine(Skill skill) {
        String range = skill.getMinDamage() + "&ndash;" + skill.getMaxDamage();
        if (skill.getSkillType() == Skill.SkillType.HEAL) {
            return "Heals: " + range;
        }
        return "Damage: " + range;
    }

    private static String effectLine(Skill skill) {
        switch (skill.getEffectType()) {
            case DAMAGE_REDUCTION:
                return " &nbsp;|&nbsp; <em>Reduces enemy damage by "
                        + (int)(skill.getEffectValue() * 100) + "%</em>";
            case HEAL:
                return " &nbsp;|&nbsp; <em>Heals " + (int)skill.getEffectValue() + " HP</em>";
            case EXTRA_DAMAGE:
                return " &nbsp;|&nbsp; <em>Deals +" + (int)skill.getEffectValue() + " bonus damage</em>";
            case TURN_STEAL:
                return " &nbsp;|&nbsp; <em>"
                        + (int)(skill.getProcChance() * 100) + "% chance to steal a turn</em>";
            case COOLDOWN_INCREASE:
                return " &nbsp;|&nbsp; <em>"
                        + (int)(skill.getProcChance() * 100)
                        + "% chance to extend opponent cooldowns by "
                        + (int)skill.getEffectValue() + "</em>";
            case COOLDOWN_REDUCTION:
                return " &nbsp;|&nbsp; <em>Resets all other skill cooldowns</em>";
            case RECOIL:
                return " &nbsp;|&nbsp; <em>Takes "
                        + (int)(skill.getEffectValue() * 100) + "% recoil &amp; -30% damage for 2 turns</em>";
            case NONE:
            default:
                return "";
        }
    }

    private static String[] defaultEnemyStory() {
        return new String[]{
                "<p>This creature was not always what it is now.</p>",
                "<p>The Gem Void has a way of taking things that once served a purpose "
                        + "and hollowing them out, leaving only the instinct for opposition.</p>",
                "<p>Whatever it was before &mdash; guardian, wanderer, or something older "
                        + "&mdash; that version is gone.</p>",
                "<p>What stands before you is what the Void decided to keep.</p>",
                "<p><em>Engage carefully. Retreat is not always an option "
                        + "inside the Gem Void.</em></p>"
        };
    }

    /** Escapes the minimum HTML characters needed for safe inline insertion. */
    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}