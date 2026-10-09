package net.programmierecke.radiodroid2.cast;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.mediarouter.app.MediaRouteChooserDialog;
import androidx.mediarouter.media.MediaRouter;

public class CustomRouteChooserDialog extends MediaRouteChooserDialog {

    public CustomRouteChooserDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    public boolean onFilterRoute(@NonNull MediaRouter.RouteInfo route) {
        String provider = route.getProvider().getPackageName();
        if (provider != null && !provider.contains("com.google.android.gms")) {
            return false;
        }
        return super.onFilterRoute(route);
    }
}
