package net.programmierecke.radiodroid2.utils;

import android.content.Context;
import android.os.RemoteException;

import net.programmierecke.radiodroid2.IPlayerService;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.station.DataRadioStation;

import java.lang.ref.WeakReference;
import java.util.concurrent.Future;

import okhttp3.OkHttpClient;

public class GetRealLinkAndPlayTask {
    private WeakReference<Context> contextRef;
    private DataRadioStation station;
    private WeakReference<IPlayerService> playerServiceRef;

    private OkHttpClient httpClient;
    private Future<?> future;
    private boolean cancelled = false;

    public GetRealLinkAndPlayTask(Context context, DataRadioStation station, IPlayerService playerService) {
        this.contextRef = new WeakReference<>(context);
        this.station = station;
        this.playerServiceRef = new WeakReference<>(playerService);

        RadioDroidApp radioDroidApp = (RadioDroidApp) context.getApplicationContext();
        httpClient = radioDroidApp.getHttpClient();
    }

    public void execute() {
        future = BackgroundTask.execute(
                () -> {
                    Context context = contextRef.get();
                    if (context != null) {
                        return Utils.getRealStationLink(httpClient, context.getApplicationContext(), station.StationUuid);
                    }
                    return null;
                },
                result -> {
                    IPlayerService playerService = playerServiceRef.get();
                    if (result != null && playerService != null && !cancelled) {
                        try {
                            station.playableUrl = result;
                            playerService.SetStation(station);
                            playerService.Play(false);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    public void cancel(boolean mayInterruptIfRunning) {
        cancelled = true;
        if (future != null) {
            future.cancel(mayInterruptIfRunning);
        }
    }
}
