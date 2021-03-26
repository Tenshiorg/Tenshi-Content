package io.github.shadow578.tenshicontent;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import io.github.shadow578.tenshi.content.aidl.IContentAdapter;
import io.github.shadow578.tenshi.content.aidl.IContentAdapterCallback;
import io.github.shadow578.tenshicontent.animixplay.AniMixPlayAdapterService;
import io.github.shadow578.tenshicontent.fouranime.FourAnimeAdapterService;
import io.github.shadow578.tenshicontent.yugenanime.YugenAnimeAdapterService;

/**
 * a basic activity for testing content adapter services in- process.
 * Bind- and calling logic is based on the one found in Tenshi
 */
public class TestActivity extends AppCompatActivity {
    //region tenshi.content Constants
    /**
     * metadata key for content adapter unique name String
     */
    public static final String META_UNIQUE_NAME = "io.github.shadow578.tenshi.content.UNIQUE_NAME";

    /**
     * metadata key for content adapter display name string
     */
    public static final String META_DISPLAY_NAME = "io.github.shadow578.tenshi.content.DISPLAY_NAME";

    /**
     * metadata key for content adapter version int
     */
    public static final String META_ADAPTER_API_VERSION = "io.github.shadow578.tenshi.content.ADAPTER_VERSION";
    //endregion

    //region Prefs Constants

    /**
     * pref key for the last selected adapter
     */
    public final String KEY_LAST_ADAPTER_INDEX = "LastAdapterIndex";

    /**
     * pref key for the last selected anime
     */
    public final String KEY_LAST_ANIME_INDEX = "LastAnimeIndex";
    //endregion

    /**
     * a list of all service classes that are testable content adapters
     */
    private final Class<?>[] testableServices = {
            FourAnimeAdapterService.class,
            AniMixPlayAdapterService.class,
            YugenAnimeAdapterService.class
    };

    /**
     * a list of anime options for testing
     */
    private final Anime[] testableAnime = {
            new Anime(41389, "Tonikaku Kawaii", 2),
            new Anime(37141, "Hataraku Saibou (TV)", 1),
            new Anime(31240, "Re:Zero kara Hajimeru Isekai Seikatsu", 3)
    };

    /**
     * the currently selected adapter class
     */
    @NonNull
    private Class<?> selectedAdapter = testableServices[0];

    /**
     * the currently selected anime
     */
    @NonNull
    private Anime selectedAnime = testableAnime[0];

    /**
     * shared preferences of the app
     */
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        // initialize preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // setup anime selection spinner
        populateAnimeSelection();
        updateSelectedAnimeDisplayViews();

        // setup spinner for adaper selection
        populateAdapterSelection();
        updateAdapterMetaViews();

        // other stuff...
        resetQueryResultViews();
        updateQueryButtons(null);
    }

    /**
     * setup the spinner for anime selection
     */
    private void populateAnimeSelection() {
        // get a list of all testable anime names
        final ArrayList<String> animeNames = new ArrayList<>();
        for (Anime a : testableAnime)
            animeNames.add(a.enTitle + " (EP " + a.episode + ")");

        // setup the spinner
        final Spinner animeSelect = findViewById(R.id.anime_select_spinner);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_generic_text, animeNames);
        animeSelect.setAdapter(adapter);
        animeSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedAnime = testableAnime[position];
                updateSelectedAnimeDisplayViews();
                resetQueryResultViews();

                // save selection in prefs
                prefs.edit().putInt(KEY_LAST_ANIME_INDEX, position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // load index from prefs
        int selIndex = prefs.getInt(KEY_LAST_ANIME_INDEX, 0);
        animeSelect.setSelection(selIndex >= testableAnime.length ? 0 : selIndex);
    }

    /**
     * update the selected anime info views
     */
    @SuppressLint("SetTextI18n")
    private void updateSelectedAnimeDisplayViews() {
        final TextView malID = findViewById(R.id.anime_mal_id);
        final TextView title = findViewById(R.id.anime_en_title);
        final TextView episode = findViewById(R.id.anime_episode);

        malID.setText("MAL ID\n" + selectedAnime.malID);
        title.setText("Title (EN)\n" + selectedAnime.enTitle);
        episode.setText("Episode\n" + selectedAnime.episode);
    }

    /**
     * setup the spinner for adapter selection
     */
    private void populateAdapterSelection() {
        // get a list of all testable adapter names
        final ArrayList<String> adapterNames = new ArrayList<>();
        for (Class<?> a : testableServices)
            adapterNames.add(a.getSimpleName());

        // setup the spinner
        final Spinner adapterSelect = findViewById(R.id.adapter_select_spinner);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_generic_text, adapterNames);
        adapterSelect.setAdapter(adapter);
        adapterSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedAdapter = testableServices[position];
                updateAdapterMetaViews();
                resetQueryResultViews();

                // save selection in prefs
                prefs.edit().putInt(KEY_LAST_ADAPTER_INDEX, position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // load index from prefs
        int selIndex = prefs.getInt(KEY_LAST_ADAPTER_INDEX, 0);
        adapterSelect.setSelection(selIndex >= testableServices.length ? 0 : selIndex);
    }

    /**
     * update the adapter metadata views
     */
    private void updateAdapterMetaViews() {
        // get name of the service
        final ComponentName svcComponent = new ComponentName(this, selectedAdapter);

        try {
            // query service with metadata
            final ServiceInfo svcInfo = getPackageManager().getServiceInfo(svcComponent, PackageManager.GET_META_DATA);

            // get metadata
            final Bundle meta = svcInfo.metaData;
            String uniqueName = meta.getString(META_UNIQUE_NAME, null);
            String displayName = meta.getString(META_DISPLAY_NAME, null);
            int apiVersion = meta.getInt(META_ADAPTER_API_VERSION, -1);

            // check if metadata is ok
            boolean metadataMissing = false;
            if (uniqueName == null || uniqueName.trim().isEmpty()) {
                metadataMissing = true;
                uniqueName = "MISSING";
            }

            if (displayName == null || displayName.trim().isEmpty()) {
                metadataMissing = true;
                displayName = "MISSING";
            }

            String apiVersionStr;
            if (apiVersion <= 0) {
                metadataMissing = true;
                apiVersionStr = "MISSING";
            } else {
                apiVersionStr = String.valueOf(apiVersion);
            }

            // update views
            setMetadataViews(uniqueName, displayName, apiVersionStr);

            // show a snackbar if metadata is missing
            if (metadataMissing)
                Snackbar.make(findViewById(R.id.test_root_view), "Some metadata is missing! consider adding it.", Snackbar.LENGTH_SHORT).show();

            // show a snackbar if the service is not exported (not accessible by Tenshi)
            if (!svcInfo.exported)
                Snackbar.make(findViewById(R.id.test_root_view), "Your Adapter is not exported. Tenshi wont be able to access it", Snackbar.LENGTH_SHORT).show();
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, "NameNotFound: " + svcComponent.flattenToString(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * reset the query result views to a default value
     */
    private void resetQueryResultViews() {
        setQueryResultViews("<streamUri>", "<persistentStorage>");
    }

    /**
     * set the values of the query result views
     *
     * @param streamUrl  the resulting stream url
     * @param perStorage the persistent storage value
     */
    private void setQueryResultViews(@NonNull String streamUrl, @NonNull String perStorage) {
        final TextView url = findViewById(R.id.query_result_url);
        final TextView ps = findViewById(R.id.query_result_perStorage);

        url.setText(streamUrl);
        ps.setText(perStorage);
    }

    /**
     * set the text of the metadata details views
     *
     * @param uniqueName  the unique name value
     * @param displayName the display name value
     * @param apiVer      the api version
     */
    private void setMetadataViews(@NonNull String uniqueName, @NonNull String displayName, @NonNull String apiVer) {
        final TextView un = findViewById(R.id.meta_unique_name);
        final TextView dn = findViewById(R.id.meta_display_name);
        final TextView api = findViewById(R.id.meta_api_ver);

        un.setText(uniqueName);
        dn.setText(displayName);
        api.setText(apiVer);
    }

    /**
     * update the buttons. if peristent storage is null or emtpy, the button is disabled
     *
     * @param persStorage persistent storage value
     */
    private void updateQueryButtons(@Nullable String persStorage) {
        // find buttons
        final Button queryNoStorage = findViewById(R.id.query_button_no_storage);
        final Button queryWithStorage = findViewById(R.id.query_button_with_storage);

        // check if storage is empty
        final boolean hasPersistentStorage = persStorage != null && !persStorage.trim().isEmpty();

        // set onclick without storage
        queryNoStorage.setOnClickListener(v -> testContentService(selectedAdapter, ""));

        // set onclick with storage if we have storage
        // else disable the button
        if (hasPersistentStorage) {
            queryWithStorage.setOnClickListener(v -> testContentService(selectedAdapter, persStorage));
            queryWithStorage.setEnabled(true);
        } else
            queryWithStorage.setEnabled(false);
    }

    /**
     * test a content adapter service, and show the result in the ui
     *
     * @param svcClass          the service to bind and test
     * @param persistentStorage persistent storage to use in the cal to requestStreamUri
     */
    private void testContentService(@NonNull Class<?> svcClass, @NonNull String persistentStorage) {
        // clear result views
        resetQueryResultViews();

        // bind the service
        final Intent svcIntent = new Intent(this, svcClass);
        bindService(svcIntent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Toast.makeText(TestActivity.this, name.getClassName() + " connected, start query", Toast.LENGTH_SHORT).show();

                // get service as IContentAdapter and test
                final IContentAdapter ca = IContentAdapter.Stub.asInterface(service);
                testContentAdapter(ca, svcIntent, persistentStorage);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Toast.makeText(TestActivity.this, name.getClassName() + " disconnected", Toast.LENGTH_SHORT).show();
            }
        }, BIND_AUTO_CREATE);
    }

    /**
     * test a content adapter and update the results in the ui
     *
     * @param ca                the adapter to test
     * @param svcIntent         the service connection intent, to close after a result is received
     * @param persistentStorage persistent storage to use in the cal to requestStreamUri
     */
    private void testContentAdapter(@NonNull IContentAdapter ca, @NonNull Intent svcIntent, @NonNull String persistentStorage) {
        // get handler on main thread
        final Handler mh = new Handler(getMainLooper());

        // run in a background thread
        new Thread(() -> {
            try {
                // request uri from the adapter
                ca.requestStreamUri(selectedAnime.malID, selectedAnime.enTitle, "", selectedAnime.episode, persistentStorage, new IContentAdapterCallback.Stub() {
                    @Override
                    public void streamUriResult(String streamUri, String persistentStorage) {
                        mh.post(() -> {
                            // show results
                            setQueryResultViews(streamUri, persistentStorage);

                            // update buttons
                            updateQueryButtons(persistentStorage);

                            // disconnect service
                            stopService(svcIntent);
                        });
                    }
                });

            } catch (Exception e) {
                Log.e("TenshiCP", "Exception in content adapter call:");
                e.printStackTrace();

                // show a toast
                mh.post(() -> Toast.makeText(TestActivity.this, e.toString() + " in CA call", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    /**
     * basic anime infos, for testing
     */
    private static class Anime {
        public final int malID;
        public final String enTitle;
        public final int episode;

        private Anime(int malID, String enTitle, int episode) {
            this.malID = malID;
            this.enTitle = enTitle;
            this.episode = episode;
        }
    }
}