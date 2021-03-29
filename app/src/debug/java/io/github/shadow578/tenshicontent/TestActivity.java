package io.github.shadow578.tenshicontent;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import java.util.ArrayList;
import java.util.List;

import io.github.shadow578.tenshi.extensionslib.content.ContentAdapterManager;
import io.github.shadow578.tenshi.extensionslib.content.ContentAdapterWrapper;

import static io.github.shadow578.tenshi.extensionslib.lang.LanguageUtil.str;

/**
 * a basic activity for testing content adapter services in- process.
 * Bind- and calling logic is based on the one found in Tenshi
 */
public class TestActivity extends AppCompatActivity {
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
     * a list of anime options for testing
     */
    private final Anime[] testableAnime = {
            new Anime(41389, "Tonikaku Kawaii", 2),
            new Anime(37141, "Hataraku Saibou (TV)", 1),
            new Anime(31240, "Re:Zero kara Hajimeru Isekai Seikatsu", 3)
    };

    /**
     * content adapter manager
     */
    private ContentAdapterManager contentAdapterManager;

    /**
     * the currently selected adapter unique name
     */
    @NonNull
    private String selectedAdapter = "";

    /**
     * the currently selected anime
     */
    @NonNull
    private Anime selectedAnime = testableAnime[0];

    /**
     * shared preferences of the app
     */
    private SharedPreferences prefs;

    /**
     * current persistent storage value
     */
    private String persistentStorageValue = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        // initialize preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // setup anime selection spinner
        populateAnimeSelection();
        updateSelectedAnimeDisplayViews();

        // init content adapter manager
        contentAdapterManager = new ContentAdapterManager(this, new ContentAdapterManager.IPersistentStorageProvider() {
            @NonNull
            @Override
            public String getPersistentStorage(@NonNull String uniqueName, int animeId) {
                return persistentStorageValue;
            }

            @Override
            public void setPersistentStorage(@NonNull String uniqueName, int animeId, @NonNull String persistentStorage) {
                persistentStorageValue = persistentStorage;
                updateQueryButtons(persistentStorage);
            }
        });
        contentAdapterManager.discoverAndInit(false);
        contentAdapterManager.addOnDiscoveryEndCallback(p -> {
            // abort if no adapters found
            if(p.getAdapterCount() <= 0)
            {
                Toast.makeText(this, "No adapters loaded!", Toast.LENGTH_SHORT).show();
                return;
            }

            // setup spinner for adapter selection
            populateAdapterSelection();
            updateAdapterMetaViews();

            // other stuff...
            resetQueryResultViews();
            updateQueryButtons(null);
        });
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
        final List<ContentAdapterWrapper> cas = contentAdapterManager.getAdapters();
        final ArrayList<String> adapterNames = new ArrayList<>();
        for (ContentAdapterWrapper a : cas) {
            adapterNames.add(a.getDisplayName());
        }

        // setup the spinner
        final Spinner adapterSelect = findViewById(R.id.adapter_select_spinner);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_generic_text, adapterNames);
        adapterSelect.setAdapter(adapter);
        adapterSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedAdapter = cas.get(position).getUniqueName();
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
        adapterSelect.setSelection(selIndex >= cas.size() ? 0 : selIndex);
    }

    /**
     * update the adapter metadata views
     */
    private void updateAdapterMetaViews() {
        // get adapter
        final ContentAdapterWrapper ca = contentAdapterManager.getAdapterOrDefault(selectedAdapter);

        // update views
        setMetadataViews(ca.getUniqueName(), ca.getDisplayName(), str(ca.getApiVersion()));
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
        queryNoStorage.setOnClickListener(v -> {
            persistentStorageValue = "";
            testCurrentContentAdapter();
        });

        // set onclick with storage if we have storage
        // else disable the button
        if (hasPersistentStorage) {
            queryWithStorage.setOnClickListener(v -> testCurrentContentAdapter());
            queryWithStorage.setEnabled(true);
        } else
            queryWithStorage.setEnabled(false);
    }

    /**
     * test the current content adapter
     */
    private void testCurrentContentAdapter() {
        // clear result views
        resetQueryResultViews();

        // get wrapper and bind
        final ContentAdapterWrapper ca = contentAdapterManager.getAdapter(selectedAdapter);
        ca.bind(this);
        ca.requestStreamUri(selectedAnime.malID, selectedAnime.enTitle, "", selectedAnime.episode, url -> {
            // show results
            setQueryResultViews(url, persistentStorageValue);

            // unbind
            ca.unbind(this);
        });
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