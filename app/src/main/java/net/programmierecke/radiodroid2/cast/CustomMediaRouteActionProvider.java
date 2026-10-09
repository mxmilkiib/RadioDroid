package net.programmierecke.radiodroid2.cast;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.mediarouter.app.MediaRouteActionProvider;
import androidx.mediarouter.app.MediaRouteButton;
import androidx.mediarouter.app.MediaRouteChooserDialog;
import androidx.mediarouter.app.MediaRouteChooserDialogFragment;
import androidx.mediarouter.app.MediaRouteDialogFactory;

public class CustomMediaRouteActionProvider extends MediaRouteActionProvider {

    private static final MediaRouteDialogFactory dialogFactory = new MediaRouteDialogFactory() {
        @NonNull
        @Override
        public MediaRouteChooserDialogFragment onCreateChooserDialogFragment() {
            return new CustomChooserDialogFragment();
        }
    };

    public CustomMediaRouteActionProvider(@NonNull Context context) {
        super(context);
    }

    @Override
    public MediaRouteButton onCreateMediaRouteButton() {
        MediaRouteButton button = super.onCreateMediaRouteButton();
        button.setDialogFactory(dialogFactory);
        return button;
    }

    public static class CustomChooserDialogFragment extends MediaRouteChooserDialogFragment {
        @NonNull
        @Override
        public MediaRouteChooserDialog onCreateChooserDialog(@NonNull Context context, @Nullable Bundle savedInstanceState) {
            return new CustomRouteChooserDialog(context);
        }
    }
}
