package io.github.shadow578.tenshicontent.fouranime.webview;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import io.github.shadow578.tenshi.content.aidl.IContentAdapter;
import io.github.shadow578.tenshi.content.aidl.IContentAdapterCallback;

public class FourAnimeWebViewAdapterService extends Service {
    /**
     * action to signal that the callback should be invoked.
     * only valid if internal callback is set, otherwise ignored
     */
    public static final String ACTION_NOTIFY_RESULT = "io.github.shadow578.tenshicontent.fouranime.webview.NOTIFY_RESULT";

    /**
     * stream url to pass to callback, string
     */
    public static final String EXTRA_RESULT_STREAM_URL = "streamUrl";

    /**
     * persistent storage to pass to callback, string
     */
    public static final String EXTRA_RESULT_PERSISTENT_STORAGE = "persistentStorage";

    /**
     * internal callback reference
     */
    @Nullable
    private IContentAdapterCallback callback = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new Adapter();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent.getAction();
        if(callback != null && action.equals(ACTION_NOTIFY_RESULT))
        {
            // this is NOTIFY_RESULT action and we have a callback, call it!
            // get extras for the call
            final String streamUrl = intent.getStringExtra(EXTRA_RESULT_STREAM_URL);
            String persistentStor = intent.getStringExtra(EXTRA_RESULT_PERSISTENT_STORAGE);
            if(persistentStor == null)
                persistentStor = "";

            // invoke the callback
            try {
                callback.streamUriResult(streamUrl, persistentStor);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            // clear the callback so we don't call it another time
            callback = null;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * adapter class.
     * just opens {@link FourAnimeWebViewActivity} with required extras and sets {@link FourAnimeWebViewAdapterService#callback}
     */
    class Adapter extends IContentAdapter.Stub{
        @Override
        public void requestStreamUri(int malID, String enTitle, String jpTitle, int episode, String peristentStorage, IContentAdapterCallback cb)  {
            // set callback
            callback = cb;

            // start webview activity
            final Intent i = new Intent(getApplicationContext(), FourAnimeWebViewActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(FourAnimeWebViewActivity.EXTRA_MAL_ID, malID);
            i.putExtra(FourAnimeWebViewActivity.EXTRA_ANIME_TITLE_EN, enTitle);
            i.putExtra(FourAnimeWebViewActivity.EXTRA_ANIME_TITLE_JP, jpTitle);
            i.putExtra(FourAnimeWebViewActivity.EXTRA_TARGET_EPISODE, episode);
            i.putExtra(FourAnimeWebViewActivity.EXTRA_PERSISTENT_STORAGE, peristentStorage);
            getApplicationContext().startActivity(i);
        }
    }
}
