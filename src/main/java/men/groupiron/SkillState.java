package men.groupiron;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;

import java.util.*;

@Slf4j
public class SkillState implements ConsumableState {
    private final Map<String, Integer> skillXpMap;
    private transient final String playerName;

    public SkillState(String playerName, Client client) {
        this.playerName = playerName;
        skillXpMap = new HashMap<>();
        for (Skill skill : Skill.values()) {
            skillXpMap.put(skill.getName(), client.getSkillExperience(skill));
        }
    }

    @Override
    public Object get() {
        return new int[] {
                skillXpMap.get("Agility"),
                skillXpMap.get("Attack"),
                skillXpMap.get("Construction"),
                skillXpMap.get("Cooking"),
                skillXpMap.get("Crafting"),
                skillXpMap.get("Defence"),
                skillXpMap.get("Farming"),
                skillXpMap.get("Firemaking"),
                skillXpMap.get("Fishing"),
                skillXpMap.get("Fletching"),
                skillXpMap.get("Herblore"),
                skillXpMap.get("Hitpoints"),
                skillXpMap.get("Hunter"),
                skillXpMap.get("Magic"),
                skillXpMap.get("Mining"),
                skillXpMap.get("Prayer"),
                skillXpMap.get("Ranged"),
                skillXpMap.get("Runecraft"),
                skillXpMap.get("Slayer"),
                skillXpMap.get("Smithing"),
                skillXpMap.get("Strength"),
                skillXpMap.get("Thieving"),
                skillXpMap.get("Woodcutting")
        };
    }

    @Override
    public String whoOwnsThis() {
        return playerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof SkillState)) return false;

        SkillState other = (SkillState) o;
        for (Skill skill : Skill.values()) {
            String skillName = skill.getName();
            if (!skillXpMap.get(skillName).equals(other.skillXpMap.get(skillName))) return false;
        }

        return true;
    }
}
