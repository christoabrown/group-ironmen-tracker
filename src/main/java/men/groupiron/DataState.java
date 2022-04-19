package men.groupiron;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class DataState {
    private final AtomicReference<ConsumableState> state = new AtomicReference<>();
    private ConsumableState previousState;
    private final String key;
    private final boolean transactionBased;

    DataState(String key, boolean transactionBased) {
        this.key = key;
        this.transactionBased = transactionBased;
    }

    public void update(ConsumableState o) {
        if (!o.equals(previousState)) {
            previousState = o;

            if (!transactionBased) {
                state.set(o);
            }
        }
    }

    public void consumeState(Map<String, Object> output) {
        final ConsumableState consumedState = state.getAndSet(null);
        if (consumedState != null) output.put(key, consumedState.get());
    }

    public void commitTransaction() {
        state.set(previousState);
    }
}
