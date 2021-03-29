package io.github.shadow578.tenshicontent.webadapter.definition.model;

import androidx.annotation.Nullable;

/**
 * definition of a web adapter loaded from a json file
 */
@SuppressWarnings({"unused", "RedundantSuppression"})
public final class WebAdapterDefinition {

    /**
     * unique name for this adapter
     */
    public String name;

    /**
     * display name of this adapter
     */
    public String displayName;

    /**
     * regex pattern to validate the storage.
     * may be null to disable verification
     */
    @Nullable
    public String storagePattern;

    /**
     * search url for this adapter,
     * formatted: %s for escaped query string
     */
    public String searchUrl;

    /**
     * episode url of this adapter,
     * formatted: %s for persistent storage content, %d for target episode
     */
    public String episodeUrl;

    /**
     * url to the payload, injected as script.
     */
    public String payload;

    /**
     * string to use as the user agent.
     * if null ignore this
     */
    @Nullable
    public String userAgentOverride;

    /**
     * is webview dom storage reqiured to be enabled?
     * if null ignore this
     */
    @Nullable
    public Boolean domStorageEnabled;

    /**
     * allow content access in the webview.
     * if null ignore this
     */
    @Nullable
    public Boolean allowContentAccess;
}
