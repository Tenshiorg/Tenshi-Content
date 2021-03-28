package io.github.shadow578.tenshicontent.webadapter;

import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import io.github.shadow578.tenshi.extensionslib.content.util.WebViewAdapterActivity;
import io.github.shadow578.tenshicontent.R;
import io.github.shadow578.tenshicontent.webadapter.definition.WebAdapterDefinitionsHelper;
import io.github.shadow578.tenshicontent.webadapter.definition.model.WebAdapterDefinition;

import static io.github.shadow578.tenshi.extensionslib.lang.LanguageUtil.elvis;
import static io.github.shadow578.tenshi.extensionslib.lang.LanguageUtil.fmt;
import static io.github.shadow578.tenshi.extensionslib.lang.LanguageUtil.isNull;
import static io.github.shadow578.tenshi.extensionslib.lang.LanguageUtil.notNull;
import static io.github.shadow578.tenshi.extensionslib.lang.LanguageUtil.nullOrWhitespace;

public class WebAdapterActivity extends WebViewAdapterActivity<WebAdapterService> {

    /**
     * the definition for this adapter instance
     */
    private WebAdapterDefinition definition;

    /**
     * the storage pattern, so we only have to compile it once
     */
    @Nullable
    private Pattern storagePattern = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        // load params
        if (!loadAdapterParams()) {
            Toast.makeText(this, "failed to load params!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // init adapter definition
        if (!initDefinition()) {
            Toast.makeText(this, "failed to init definition!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        validatePersistentStorage();
        super.onCreate(savedInstanceState);
    }

    /**
     * initialize the adapter definition
     *
     * @return was init ok?
     */
    private boolean initDefinition() {
        // find correct definition
        for (WebAdapterDefinition def : WebAdapterDefinitionsHelper.getAdapterDefinitions())
            if (uniqueName.equals(WebAdapterDefinitionsHelper.buildUniqueName(def))) {
                definition = def;
                return true;
            }

        // cannot found
        return false;
    }

    /**
     * validates the persistent storage using {@link WebAdapterDefinition#storagePattern}.
     * if the pattern does not match, the storage is reset
     */
    private void validatePersistentStorage() {
        if (!nullOrWhitespace(definition.storagePattern)) {
            // compile pattern if needed
            if (isNull(storagePattern))
                storagePattern = Pattern.compile(definition.storagePattern);

            if (!storagePattern.matcher(persistentStorage).matches())
                persistentStorage = "";
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
        if (nullOrWhitespace(persistentStorage)) {
            // search for the anime
            String query;
            try {
                query = URLEncoder.encode(enTitle, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                query = enTitle.replace(" ", "+");
            }

            return fmt(definition.searchUrl, query);
        } else {
            // directly open episode page
            return fmt(definition.episodeUrl, persistentStorage, episode);
        }
    }

    /**
     * add javascript interfaces to the webview.
     * By default, {@link JSInterface} is added as 'App' (basic app calls),
     * also, {@link WebAdapterJs} is added as 'Tenshi'
     *
     * @param webView the webview to add the interfaces to
     */
    @Override
    protected void addJavascriptInterfaces(@NonNull WebView webView) {
        super.addJavascriptInterfaces(webView);
        webView.addJavascriptInterface(new WebAdapterJs(), "Tenshi");
    }

    /**
     * configure the webview's settings. according to the definition
     *
     * @param webView  the webview to configure
     * @param settings the webview's settings (same as webView.getSettings())
     */
    @Override
    protected void configureWebView(@NonNull WebView webView, @NonNull WebSettings settings) {
        super.configureWebView(webView, settings);

        // configure as per definition
        if (!nullOrWhitespace(definition.userAgentOverride))
            settings.setUserAgentString(definition.userAgentOverride);

        if (notNull(definition.allowContentAccess))
            settings.setAllowContentAccess(definition.allowContentAccess);

        if (notNull(definition.domStorageEnabled))
            settings.setDomStorageEnabled(definition.domStorageEnabled);
    }

    /**
     * get the js loader script.
     * the loader is is loaded by first wrapping it into a function, then invoking using {@link WebView#loadUrl(String)}
     * this loader loads the javascript payload from a web resource and calls {@link Constants#PAYLOAD_INIT_FUNCTION_CALL} in it
     *
     * @return the js loader
     */
    @NonNull
    @Override
    protected String getJSLoader() {
        if (!Constants.isDebugMode()) {
            // loader for production builds (webloader)
            return "fetch('" + definition.payload + "').then(function(response) { response.text().then(function(text) { var script = document.createElement('script'); script.innerText = text; document.body.appendChild(script); " + Constants.PAYLOAD_INIT_FUNCTION_CALL + "; }) });";
        } else {
            // loader that loads from raw using getJsPayload()
            return "var script = document.createElement('script'); script.innerText = JSPayloadIf.getPayloadJs(document.location.href); document.body.appendChild(script); " + Constants.PAYLOAD_INIT_FUNCTION_CALL + ";";
        }
    }

    /**
     * get the javascript payload. only used in debugging mode ({@link Constants#isDebugMode()})
     * this is loaded and called by the loader returned by {@link #getJSLoader()}.
     *
     * @param url the url we just loaded
     * @return the js payload to inject. empty string if you don't want to inject anything.
     */
    @NonNull
    @Override
    protected String getJSPayload(@NonNull String url) {
        try {
            return loadPayloadFromRaw(R.raw.debug_payload);
        } catch (IOException e) {
            Log.e("TenshiCP", "debug payload load failed: " + e.toString());
            e.printStackTrace();
            return "";
        }
    }

    /**
     * get the activity adapter service to invoke the callback on
     *
     * @return the service class
     */
    @NonNull
    @Override
    protected Class<WebAdapterService> getServiceClass() {
        return WebAdapterService.class;
    }

    /**
     * javascript interface for web adapter payloads
     */
    @SuppressWarnings({"unused", "RedundantSuppression"})
    public class WebAdapterJs {

        /**
         * @return the unique name we're currently working under
         */
        @JavascriptInterface
        public String getUniqueName() {
            return uniqueName;
        }

        /**
         * @return the english anime title
         */
        @JavascriptInterface
        public String getAnimeTitle() {
            return enTitle;
        }

        /**
         * @return the japanese anime title
         */
        @JavascriptInterface
        public String getAnimeTitleJp() {
            return jpTitle;
        }

        /**
         * @return the anime's mal id
         */
        @JavascriptInterface
        public int getMalId() {
            return malID;
        }

        /**
         * @return the current value of the persistent storage
         */
        @JavascriptInterface
        public String getPersistentStorage() {
            return persistentStorage;
        }

        /**
         * set the value of the persistent storage.
         * Is only saved after finish is called
         *
         * @param ps the new persistent storage value
         */
        @JavascriptInterface
        public void setPersistentStorage(String ps) {
            persistentStorage = elvis(ps, "");
        }

        /**
         * close the adapter activity and return the stream url to tenshi
         *
         * @param streamUrl the stream url. this may be null
         */
        @JavascriptInterface
        public void finish(String streamUrl) {
            invokeCallback(streamUrl);
            WebAdapterActivity.this.finish();
        }
    }
}
