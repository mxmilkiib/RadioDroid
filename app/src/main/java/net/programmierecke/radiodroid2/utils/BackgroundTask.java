package net.programmierecke.radiodroid2.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class BackgroundTask {
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static <T> Future<?> execute(Callable<T> background, Consumer<T> foreground) {
        return executor.submit(() -> {
            try {
                T result = background.call();
                mainHandler.post(() -> foreground.accept(result));
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    }
                    throw new RuntimeException(e);
                });
            }
        });
    }

    public static Future<?> execute(Runnable background, Runnable foreground) {
        return executor.submit(() -> {
            try {
                background.run();
                mainHandler.post(foreground);
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    }
                    throw new RuntimeException(e);
                });
            }
        });
    }
}
