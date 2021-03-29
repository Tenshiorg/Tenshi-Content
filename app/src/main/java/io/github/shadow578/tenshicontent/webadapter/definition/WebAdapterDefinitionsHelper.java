package io.github.shadow578.tenshicontent.webadapter.definition;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import io.github.shadow578.tenshicontent.R;
import io.github.shadow578.tenshicontent.webadapter.Constants;
import io.github.shadow578.tenshicontent.webadapter.definition.model.WebAdapterDefinition;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.internal.EverythingIsNonNull;

import static io.github.shadow578.tenshi.extensionslib.lang.LanguageUtil.isNull;
import static io.github.shadow578.tenshi.extensionslib.lang.LanguageUtil.notNull;

/**
 * helper class for loading adapter definitions
 */
public class WebAdapterDefinitionsHelper {
    /**
     * lock for adapter definitions
     */
    private static final Object DEFINITIONS_LOCK = new Object();

    /**
     * a list of all loaded adapter definitions.
     * null only if not loaded yet
     */
    @Nullable
    private static List<WebAdapterDefinition> adapterDefinitions = null;

    /**
     * get a list of web definitions, waiting until loading finished
     *
     * @return the list of web adapter definitions, or null if not loaded / loading failed / ...
     */
    @NonNull
    public static List<WebAdapterDefinition> getAdapterDefinitions() {
        try {
            synchronized (DEFINITIONS_LOCK) {
                if (isNull(adapterDefinitions))
                    DEFINITIONS_LOCK.wait();

                return adapterDefinitions;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * load the web adapter definitions from the given url if they are not already loaded
     *
     * @param ctx           context to load in
     * @param definitionUrl the url of the definition json file
     */
    public static void loadDefinitionsOnce(@NonNull Context ctx, @NonNull String definitionUrl) {
        // skip if already loaded
        if (notNull(adapterDefinitions))
            return;

        if (Constants.isDebugMode())
            loadDefinitionRaw(ctx);
        else
            loadDefinitionWeb(definitionUrl);
    }

    /**
     * laod definitions from web resource
     *
     * @param definitionUrl the definition json to load
     */
    private static void loadDefinitionWeb(@NonNull String definitionUrl) {
        // create retrofit
        final Retrofit rf = new Retrofit.Builder()
                .baseUrl("https://www.example.com/")// have to put something
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // create interface
        final WebAdapterRetrofit rfIf = rf.create(WebAdapterRetrofit.class);

        // query definitions
        rfIf.getDefinitions(definitionUrl).enqueue(new Callback<List<WebAdapterDefinition>>() {
            @Override
            @EverythingIsNonNull
            public void onResponse(Call<List<WebAdapterDefinition>> call, Response<List<WebAdapterDefinition>> response) {
                synchronized (DEFINITIONS_LOCK) {
                    // check the call was successful
                    if (response.isSuccessful()) {
                        // response is ok, set definitions from body
                        adapterDefinitions = response.body();
                    } else {
                        //failed
                        adapterDefinitions = new ArrayList<>();
                    }
                    DEFINITIONS_LOCK.notifyAll();
                }
            }

            @Override
            @EverythingIsNonNull
            public void onFailure(Call<List<WebAdapterDefinition>> call, Throwable t) {
                synchronized (DEFINITIONS_LOCK) {
                    adapterDefinitions = new ArrayList<>();
                    DEFINITIONS_LOCK.notifyAll();
                }
            }
        });
    }

    /**
     * load definitions from R.raw
     *
     * @param ctx the context to load from
     */
    private static void loadDefinitionRaw(@NonNull Context ctx) {
        synchronized (DEFINITIONS_LOCK) {
            try {
                // open reader for the resource
                try (BufferedReader rawIn = new BufferedReader(new InputStreamReader(
                        ctx.getResources().openRawResource(R.raw.debug_definition)))) {

                    // read and append line- by- line
                    final StringBuilder json = new StringBuilder();
                    String ln;
                    while ((ln = rawIn.readLine()) != null)
                        json.append(ln).append("\n");

                    // deserialize from json

                    adapterDefinitions = new Gson()
                            .fromJson(json.toString(), new TypeToken<List<WebAdapterDefinition>>() {
                            }.getType());

                }
            } catch (IOException e) {
                e.printStackTrace();
                adapterDefinitions = new ArrayList<>();
            }
            DEFINITIONS_LOCK.notifyAll();
        }
    }

    /**
     * build the unique name for a definition
     *
     * @param def the definition
     * @return the unique name
     */
    public static String buildUniqueName(@NonNull WebAdapterDefinition def) {
        return Constants.UNIQUE_NAME_PREFIX + def.name;
    }
}
