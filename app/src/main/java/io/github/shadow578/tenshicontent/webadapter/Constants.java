package io.github.shadow578.tenshicontent.webadapter;

import io.github.shadow578.tenshicontent.BuildConfig;

/**
 * WebAdapter constants
 */
public final class Constants {

    /**
     * in debug mode, definition json and script payload are loaded from R.raw.
     * This is ignored when not a debug build
     */
    public static final boolean DEBUG_MODE = true;

    /**
     * url to load web definitions from
     */
    @SuppressWarnings("FieldCanBeLocal")
    public static final String DEFINITIONS_URL = "https://raw.githubusercontent.com/Tenshiorg/Tenshi-Content/kohai/webadapters/adapter-definitions.json";

    /**
     * prefix for web adapter unique names
     */
    @SuppressWarnings("FieldCanBeLocal")
    public static final String UNIQUE_NAME_PREFIX = "io.github.shadow578.tenshicontent.webadapter.";

    /**
     * payload function call, executed after inject
     */
    public static final String PAYLOAD_INIT_FUNCTION_CALL = "__tenshi_payload_init()";

    /**
     * should we load definition and payload from R.raw?
     * @return enable debug mode?
     */
    public static boolean isDebugMode(){
        return BuildConfig.DEBUG && DEBUG_MODE;
    }
}
