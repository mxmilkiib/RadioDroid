package net.programmierecke.radiodroid2.utils;

import java.util.concurrent.CopyOnWriteArrayList;

public class ChangeNotifier {
    public interface ChangeListener {
        void onChanged();
    }

    private final CopyOnWriteArrayList<ChangeListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(ChangeListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeListener(ChangeListener listener) {
        listeners.remove(listener);
    }

    public void notifyListeners() {
        for (ChangeListener listener : listeners) {
            listener.onChanged();
        }
    }
}
