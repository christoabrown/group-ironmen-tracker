package men.groupiron;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionPageState implements ConsumableState {
    private final ItemContainerState items;
    private final String pageName;
    private final int tabIdx;
    private final List<Integer> completionCounts;

    public CollectionPageState(int tabIdx, String pageName, ItemContainerState items, List<Integer> completionCounts) {
        this.tabIdx = tabIdx;
        this.pageName = pageName;
        this.items = items;
        this.completionCounts = completionCounts;
    }

    @Override
    public Object get() {
        Map<String, Object> result = new HashMap<>();
        result.put("tab", tabIdx);
        result.put("page_name", pageName);
        result.put("items", items.get());
        result.put("completion_counts", completionCounts);
        return result;
    }

    @Override
    public String whoOwnsThis() { return items.whoOwnsThis(); }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof CollectionPageState)) return false;
        CollectionPageState other = (CollectionPageState) o;
        boolean completionCountsEqual = completionCounts.size() == other.completionCounts.size();
        if (completionCountsEqual) {
            for (int i = 0; i < completionCounts.size(); ++i) {
                completionCountsEqual = completionCounts.get(i).equals(other.completionCounts.get(i));
                if (!completionCountsEqual) break;
            }
        }

        return (completionCountsEqual && tabIdx == other.tabIdx && pageName.equals(other.pageName) && items.equals(other.items));
    }
}
