package io.github.shadow578.tenshicontent.animixplay;

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

import io.github.shadow578.tenshi.extensionslib.content.util.WebViewAdapterActivity;
import io.github.shadow578.tenshicontent.R;

/**
 * ActivityAdapter activity for {@link AniMixPlayAdapterService}
 * Shows a webview and injects javascript to access the video url
 */
public class AniMixPlayAdapterActivity extends WebViewAdapterActivity<AniMixPlayAdapterService> {
    //region URL constants

    /**
     * base url for animixplay
     */
    private final String BASE_URL = "https://animixplay.to/";

    /**
     * search url for animixplay
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String SEARCH_URL = BASE_URL + "?q=";

    /**
     * formattable episode url
     * <p>
     * FMT: anime_slug, episode
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String EP_URL_FMT = BASE_URL + "/v1/%s/ep%d";
    //endregion

    /**
     * main js payload
     */
    private String jsPayload = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (!loadAdapterParams()) {
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
            jsPayload = loadPayloadFromRaw(R.raw.animixplay_payload);
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
            return String.format(Locale.ENGLISH, EP_URL_FMT, persistentStorage, episode);
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
        webView.addJavascriptInterface(new AniMixPlayJS(), "Tenshi");
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
    protected Class<AniMixPlayAdapterService> getServiceClass() {
        return AniMixPlayAdapterService.class;
    }

    /**
     * javascript interface for payload- specific functions
     */
    @SuppressWarnings({"unused", "RedundantSuppression"})
    private class AniMixPlayJS {
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
