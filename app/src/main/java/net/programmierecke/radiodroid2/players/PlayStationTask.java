package net.programmierecke.radiodroid2.players;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import net.programmierecke.radiodroid2.ActivityMain;
import net.programmierecke.radiodroid2.CastHandler;
import net.programmierecke.radiodroid2.FavouriteManager;
import net.programmierecke.radiodroid2.HistoryManager;
import net.programmierecke.radiodroid2.R;
import net.programmierecke.radiodroid2.RadioDroidApp;
import net.programmierecke.radiodroid2.Utils;
import net.programmierecke.radiodroid2.players.mpd.MPDClient;
import net.programmierecke.radiodroid2.players.mpd.MPDServerData;
import net.programmierecke.radiodroid2.players.mpd.tasks.MPDPlayTask;
import net.programmierecke.radiodroid2.station.DataRadioStation;
import net.programmierecke.radiodroid2.utils.BackgroundTask;

import java.lang.ref.WeakReference;
import java.util.concurrent.Future;

public class PlayStationTask {
    public interface PlayFunc {
        void play(String url);
    }

    public enum ExecutionResult {
        FAILURE,
        SUCCESS,
    }

    public interface PostExecuteTask {
        void onPostExecute(ExecutionResult executionResult);
    }

    private PlayFunc playFunc;
    private PostExecuteTask postExecuteTask;
    private DataRadioStation stationToPlay;
    private WeakReference<Context> contextWeakReference;
    private Future<?> future;
    private boolean cancelled = false;

    public PlayStationTask(@NonNull DataRadioStation stationToPlay, @NonNull Context ctx,
                           @NonNull PlayFunc playFunc, @Nullable PostExecuteTask postExecuteTask) {
        this.stationToPlay = stationToPlay;
        this.contextWeakReference = new WeakReference<>(ctx);
        this.playFunc = playFunc;
        this.postExecuteTask = postExecuteTask;
    }

    public static PlayStationTask playMPD(MPDClient mpdClient, MPDServerData mpdServerData, DataRadioStation stationToPlay, Context ctx) {
        return new PlayStationTask(stationToPlay, ctx, url -> mpdClient.enqueueTask(mpdServerData, new MPDPlayTask(url, null)), null);
    }

    public static PlayStationTask playExternal(DataRadioStation stationToPlay, Context ctx) {
        return new PlayStationTask(stationToPlay, ctx, url -> {
            Intent share = new Intent(Intent.ACTION_VIEW);
            share.setDataAndType(Uri.parse(url), "audio/*");
            ctx.startActivity(share);
        }, null);
    }

    public static PlayStationTask playCAST(DataRadioStation stationToPlay, Context ctx) {
        RadioDroidApp radioDroidApp = (RadioDroidApp) ctx.getApplicationContext();
        CastHandler castHandler = radioDroidApp.getCastHandler();
        return new PlayStationTask(stationToPlay, ctx, url -> castHandler.playRemote(stationToPlay.Name, url, stationToPlay.IconUrl), null);
    }

    private void onPreExecute() {
        Context ctx = contextWeakReference.get();
        if (ctx == null) {
            return;
        }

        LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(ActivityMain.ACTION_SHOW_LOADING));

        RadioDroidApp radioDroidApp = (RadioDroidApp) ctx.getApplicationContext();

        HistoryManager historyManager = radioDroidApp.getHistoryManager();
        historyManager.add(stationToPlay);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean autoFavorite = sharedPref.getBoolean("auto_favorite", false);

        if (autoFavorite) {
            FavouriteManager favouriteManager = radioDroidApp.getFavouriteManager();
            if (!favouriteManager.has(stationToPlay.StationUuid)) {
                favouriteManager.add(stationToPlay);
                Toast toast = Toast.makeText(ctx, ctx.getString(R.string.notify_autostarred), Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    public void execute() {
        onPreExecute();

        future = BackgroundTask.execute(
                () -> {
                    Context ctx = contextWeakReference.get();
                    if (ctx != null) {
                        RadioDroidApp radioDroidApp = (RadioDroidApp) ctx.getApplicationContext();

                        if (!stationToPlay.hasValidUuid()) {
                            if (!stationToPlay.refresh(radioDroidApp.getHttpClient(), ctx)) {
                                return null;
                            }
                        }

                        if (cancelled) {
                            return null;
                        }

                        return Utils.getRealStationLink(radioDroidApp.getHttpClient(), ctx.getApplicationContext(), stationToPlay.StationUuid);
                    } else {
                        return null;
                    }
                },
                result -> onPostExecute(result)
        );
    }

    private void onPostExecute(String result) {
        Context ctx = contextWeakReference.get();
        if (ctx == null) {
            return;
        }

        LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent(ActivityMain.ACTION_HIDE_LOADING));

        if (result != null) {
            stationToPlay.playableUrl = result;
            playFunc.play(result);
        } else {
            Toast toast = Toast.makeText(ctx.getApplicationContext(),
                    ctx.getResources()
                            .getText(R.string.error_station_load), Toast.LENGTH_SHORT);
            toast.show();
        }

        if (postExecuteTask != null) {
            postExecuteTask.onPostExecute(result != null ? ExecutionResult.SUCCESS : ExecutionResult.FAILURE);
        }
    }

    public void cancel(boolean mayInterruptIfRunning) {
        cancelled = true;
        if (future != null) {
            future.cancel(mayInterruptIfRunning);
        }
    }
}