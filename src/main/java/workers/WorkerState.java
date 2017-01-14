package workers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author KPentaris - 13/1/2017.
 */
public enum WorkerState {

    INITIAL(0),
    OPERATIONAL(1),
    STOPPED(2);

    private int value;
    private static Map<Integer, WorkerState> lookup = new HashMap<>();

    WorkerState(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static WorkerState get(int value) {
        return lookup.get(value);
    }

    public static Collection<WorkerState> get() {
        return lookup.values();
    }

    public static Collection<Integer> getKeys() {
        return lookup.keySet();
    }

    static {
        for (WorkerState value : WorkerState.values()) {
            lookup.put(value.getValue(), value);
        }
    }

}
