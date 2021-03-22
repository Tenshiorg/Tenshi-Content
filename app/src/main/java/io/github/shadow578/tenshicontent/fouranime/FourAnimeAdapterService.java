package io.github.shadow578.tenshicontent.fouranime;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class FourAnimeAdapterService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new FourAnimeAdapter(getApplicationContext());
    }
}
