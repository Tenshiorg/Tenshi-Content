package io.github.shadow578.tenshicontent.fouranime;

import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import io.github.shadow578.tenshicontent.R;
import io.github.shadow578.tenshicontent.util.WebViewAdapterActivity;

/**
 * ActivityAdapter activity for {@link FourAnimeAdapterService}.
 * Shows a webview to the user for episode selection from 4anime.to
 */
public class FourAnimeAdapterActivity extends WebViewAdapterActivity<FourAnimeAdapterService> {
    //region URL Constants
    /**
     * 4anime base url
     */
    private final String BASE_URL = "https://4anime.to/";

    /**
     * 4anime search url
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String SEARCH_URL = BASE_URL + "?s=";
    //endregion

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if(!loadAdapterParams()){
            Toast.makeText(this, "failed to load params!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        super.onCreate(savedInstanceState);
    }

    /**
     * get the initial url to navigate to
     *
     * @return the initial url
     */
    @NonNull
    @Override
    protected String getInitialUrl() {
        if (persistentStorage.trim().isEmpty()) {
            // search for the anime
            try {
                return SEARCH_URL + URLEncoder.encode(enTitle, "UTF-8");
            } catch (UnsupportedEncodingException ignored) {
                // really bad fallback, but shouldn't happen anyway
                return SEARCH_URL + enTitle.replace(" ", "+");
            }
        } else {
            // directly to the episode page
            return String.format(Locale.ENGLISH, "%s%s-episode-%02d", BASE_URL, persistentStorage, episode);
        }
    }

    /**
     * get the javascript payload.
     * this is loaded and called by the loader returned by {@link #getJSLoader()}.
     * unless you have changed the loader, you can just act like this is inside a script tag (including line breaks and comments)
     *
     * @param url the url we just loaded
     * @return the js payload to inject. empty string if you don't want to inject anything.
     */
    @NonNull
    @Override
    protected String getJSPayload(@NonNull String url) {
        try {
            return loadPayloadFromRaw(R.raw.fouranime_payload);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * add javascript interfaces to the webview.
     * By default, {@link JSInterface} is added as 'App' (basic app calls)
     * Additionally, {@link TenshiJS} is added as 'Tenshi'
     *
     * @param webView the webview to add the interfaces to
     */
    @Override
    protected void addJavascriptInterfaces(@NonNull WebView webView) {
        super.addJavascriptInterfaces(webView);
        webView.addJavascriptInterface(new TenshiJS(), "Tenshi");
    }

    /**
     * get the activity adapter service to invoke the callback on
     *
     * @return the service class
     */
    @NonNull
    @Override
    protected Class<FourAnimeAdapterService> getServiceClass() {
        return FourAnimeAdapterService.class;
    }

    /**
     * javascript interface for payload- specific functions
     */
    @SuppressWarnings({"unused", "RedundantSuppression"})
    private class TenshiJS {
        /**
         * notify the application of the stream url.
         * this will ultimately close the webview and start playback in a external player
         *
         * @param streamUrl the stream url found
         */
        @JavascriptInterface
        public void onStreamUrlFound(String streamUrl) {
            if (streamUrl != null && !streamUrl.trim().isEmpty()) {
                invokeCallback(streamUrl);
                finish();
            }
        }

        /**
         * notify the application of the anime's slug
         * this is stored in persistent storage, so we can directly go to the streaming page the next time
         *
         * @param slug the slug found
         */
        @JavascriptInterface
        public void setSlug(String slug) {
            if (slug != null && !slug.trim().isEmpty())
                persistentStorage = slug;
        }
    }
}
