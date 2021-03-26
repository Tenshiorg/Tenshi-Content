package io.github.shadow578.tenshicontent.yugenanime;

import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
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
 * ActivityAdapter activity for {@link YugenAnimeAdapterService}.
 * Shows a webview to the user for episode selection from yugenani.me
 */
public class YugenAnimeAdapterActivity extends WebViewAdapterActivity<YugenAnimeAdapterService> {

    //region URL Constants
    /**
     * yougenanime base url
     */
    private final String BASE_URL = "https://yugenani.me/";

    /**
     * yougenanime search url
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String SEARCH_URL = BASE_URL + "search/?q=";

    /**
     * yougenanime episode watch url
     *
     * FMT Params: id/slug, episode
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String EPISODE_URL_FMT = BASE_URL + "watch/%s/%d/";
    //endregion

    /**
     * main js payload
     */
    private String jsPayload = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if(!loadAdapterParams()){
            Toast.makeText(this, "failed to load params!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initPayload();
        super.onCreate(savedInstanceState);
    }

    /**
     * initialize the payload (loaded from raw)
     */
    private void initPayload() {
        try {
            jsPayload = loadPayloadFromRaw(R.raw.yugenanime_payload);
        } catch (IOException ignored) {
            jsPayload = "";
        }
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
            return String.format(Locale.ENGLISH, EPISODE_URL_FMT, persistentStorage, episode);
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
       return jsPayload;
    }

    @Override
    protected void addJavascriptInterfaces(@NonNull WebView webView) {
        super.addJavascriptInterfaces(webView);
        webView.addJavascriptInterface(new YugenAnimeJS(), "Tenshi");
    }

    @Override
    protected void configureWebView(@NonNull WebView webView, @NonNull WebSettings settings) {
        super.configureWebView(webView, settings);
        settings.setDomStorageEnabled(true);
    }

    /**
     * get the activity adapter service to invoke the callback on
     *
     * @return the service class
     */
    @NonNull
    @Override
    protected Class<YugenAnimeAdapterService> getServiceClass() {
        return YugenAnimeAdapterService.class;
    }

    /**
     * javascript interface for payload- specific functions
     */
    @SuppressWarnings({"unused", "RedundantSuppression"})
    private class YugenAnimeJS {
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
         * update persistent storage so we can directly go to the streaming page the next time
         *
         * @param ps the slug found
         */
        @JavascriptInterface
        public void setPersistentStorage(String ps) {
            if (ps != null && !ps.trim().isEmpty())
                persistentStorage = ps;
        }
    }
}
