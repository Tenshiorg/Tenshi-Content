package io.github.shadow578.tenshicontent.fouranime.webview;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import io.github.shadow578.tenshicontent.R;

public class FourAnimeWebViewActivity extends AppCompatActivity {

    //region Extras
    /**
     * MAL id to load, int
     */
    public static final String EXTRA_MAL_ID = "malID";

    /**
     * english anime title, string
     */
    public static final String EXTRA_ANIME_TITLE_EN = "titleEN";

    /**
     * japanese anime title, string
     */
    public static final String EXTRA_ANIME_TITLE_JP = "titleJP";

    /**
     * episode to watch, int
     */
    public static final String EXTRA_TARGET_EPISODE = "episode";

    /**
     * persistent storage, string
     */
    public static final String EXTRA_PERSISTENT_STORAGE = "persistentStorage";
    //endregion

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

    /**
     * target anime title
     */
    private String animeTitle = "";

    /**
     * the episode we want to watch
     */
    private int episode = 0;

    /**
     * persistent storage from extra.
     * loaded in {@link #onCreate(Bundle)}, saved in {@link #notifyService(String)}
     */
    private String persistentStorage = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // load intent data
        if (!loadIntentDetails()) {
            // missing intent extras, exit now (this should not happen normally)
            Toast.makeText(this, "failed to load intent details", Toast.LENGTH_SHORT).show();//TODO hardcoded string
            notifyService(null);
            return;
        }

        // show the webview
        final WebView webView = initWebview();
        setContentView(webView);

        // load the target
        webView.loadUrl(getInitialTargetUrl());
    }

    /**
     * load the required info from the intent extras
     *
     * @return was load successful? if not, show a error and exit
     */
    private boolean loadIntentDetails() {
        // ensure we have a intent
        final Intent i = getIntent();
        if (i == null)
            return false;

        // load data
        animeTitle = i.getStringExtra(EXTRA_ANIME_TITLE_EN);
        episode = i.getIntExtra(EXTRA_TARGET_EPISODE, -1);
        persistentStorage = i.getStringExtra(EXTRA_PERSISTENT_STORAGE);

        // make sure everything was loaded ok
        // persistent storage is optional, but cannot be null
        if (persistentStorage == null)
            persistentStorage = "";

        // we need to have a title and episode
        return animeTitle != null && episode > 0;
    }

    /**
     * initialize the webview, ready for js injection
     *
     * @return the initialized webview
     */
    private WebView initWebview() {
        // create the client that will inject our js payload
        final WebViewClient webViewClient = new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl("javascript:(function tenshi_js_loader(){ " + getJSLoader() + " })()");
            }
        };

        // create and setup webview
        final WebView webView = new WebView(this);
        webView.setWebViewClient(webViewClient);
        webView.getSettings().setJavaScriptEnabled(true);

        // add javascript interface
        webView.addJavascriptInterface(new JSInterface(), "Tenshi");

        return webView;
    }

    /**
     * get the initial target url from the loaded details.
     * if we have a slug in persistent storage, directly navigate to the episode page.
     * otherwise, search for the anime
     *
     * @return the target url to navigate to
     */
    private String getInitialTargetUrl() {
        if (persistentStorage.trim().isEmpty()) {
            // search for the anime
            try {
                return SEARCH_URL + URLEncoder.encode(animeTitle, "UTF-8");
            } catch (UnsupportedEncodingException ignored) {
                // really bad fallback, but shouldn't happen anyway
                return SEARCH_URL + animeTitle.replace(" ", "+");
            }
        } else {
            // directly to the episode page
            return String.format(Locale.ENGLISH, "%s%s-episode-%02d", BASE_URL, persistentStorage, episode);
        }
    }

    /**
     * notify the content adapter service and ultimately the main tenshi process of the stream url we found.
     * then close the activity.
     *
     * @param streamUrl the stream url
     */
    private void notifyService(@Nullable String streamUrl) {
        // notify service
        final Intent i = new Intent(this, FourAnimeWebViewAdapterService.class);
        i.setAction(FourAnimeWebViewAdapterService.ACTION_NOTIFY_RESULT);
        i.putExtra(FourAnimeWebViewAdapterService.EXTRA_RESULT_STREAM_URL, streamUrl);
        i.putExtra(FourAnimeWebViewAdapterService.EXTRA_RESULT_PERSISTENT_STORAGE, persistentStorage);
        startService(i);

        // we're done, bye- bye
        finish();
    }

    //region JS

    /**
     * get the js loader script.
     * the loader is is loaded by first wrapping it into a function, then invoking using {@link WebView#loadUrl(String)}
     *
     * @return the js loader
     */
    @NonNull
    private String getJSLoader() {
        return "eval(Tenshi.getPayloadJs(document.location.href));";
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
    private String getJSPayload(@NonNull String url) {
        // load payload from raw
        //TODO: do this only once!
        try (BufferedReader rawIn = new BufferedReader(new InputStreamReader(
                getResources().openRawResource(R.raw.fouranime_payload)))) {
            final StringBuilder payload = new StringBuilder();
            String ln;
            while ((ln = rawIn.readLine()) != null)
                payload.append(ln).append("\n");

            return payload.toString();
        } catch (IOException e) {
            Log.e("TenshiJS", e.toString());
            e.printStackTrace();
        }

        return "";
    }

    /**
     * methods accessible from javascript
     */
    @SuppressWarnings({"unused", "RedundantSuppression"})
    class JSInterface {
        /**
         * get the javascript payload for a given url.
         * this should ONLY be used by the loader script!!
         *
         * @param url the url to get the payload for.
         * @return the payload. may be a empty string, but never null
         */
        @JavascriptInterface
        public String getPayloadJs(String url) {
            return getJSPayload(url);
        }

        /**
         * notify the application of the stream url.
         * this will ultimately close the webview and start playback in a external player
         *
         * @param streamUrl the stream url found
         */
        @JavascriptInterface
        public void notifyStreamUrl(String streamUrl) {
            if (streamUrl != null && !streamUrl.trim().isEmpty())
                notifyService(streamUrl);
        }

        /**
         * notify the application of the anime's slug (for streaming).
         * this is stored in persistent storage, so we can directly go to the streaming page the next time
         *
         * @param slug the slug found
         */
        @JavascriptInterface
        public void notifyAnimeSlug(String slug) {
            if (slug != null && !slug.trim().isEmpty())
                persistentStorage = slug;
        }

        /**
         * show a toast from js
         *
         * @param msg the message of the toast
         */
        @JavascriptInterface
        public void toast(String msg) {
            Toast.makeText(FourAnimeWebViewActivity.this, "" + msg, Toast.LENGTH_SHORT).show();
        }

        /**
         * log a message from js
         *
         * @param msg the message to log
         */
        @JavascriptInterface
        public void log(String msg) {
            Log.d("TenshiJS", "" + msg);
        }
    }
    //endregion
}
