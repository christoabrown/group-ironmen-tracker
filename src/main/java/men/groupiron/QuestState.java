package men.groupiron;

import net.runelite.api.Client;
import net.runelite.api.Quest;

import java.util.*;
import java.util.stream.Collectors;

public class QuestState implements ConsumableState {
    private final Map<Integer, net.runelite.api.QuestState> questStateMap;
    private transient final String playerName;
    private List<Integer> sortedQuestIds = Arrays.stream(Quest.values()).map(Quest::getId).sorted().collect(Collectors.toList());

    public QuestState(String playerName, Client client) {
        this.playerName = playerName;
        this.questStateMap = new HashMap<>();
        for (Quest quest : Quest.values()) {
            questStateMap.put(quest.getId(), quest.getState(client));
        }
    }

    @Override
    public Object get() {
        List<Integer> result = new ArrayList<>(questStateMap.size());
        for (Integer questId : sortedQuestIds) {
            result.add(questStateMap.get(questId).ordinal());
        }

        return result;
    }

    @Override
    public String whoOwnsThis() {
        return playerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof QuestState)) return false;

        QuestState other = (QuestState) o;
        for (Quest quest : Quest.values()) {
            Integer questId = quest.getId();
            if (questStateMap.get(questId) != other.questStateMap.get(questId)) {
                return false;
            }
        }

        return true;
    }
}
