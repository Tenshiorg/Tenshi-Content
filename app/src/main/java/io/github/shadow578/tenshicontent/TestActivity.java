package io.github.shadow578.tenshicontent;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
     * the currently selected anime
     */
    @NonNull
    private Anime selectedAnime = testableAnime[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        // setup anime selection spinner
        setupAnimeSelectionSpinner();
        updateSelectedAnimeDisplayViews();

        // create buttons for the services
        createServiceButtons();

        // other stuff...
        resetQueryResultViews();
    }

    /**
     * setup the spinner for anime selection
     */
    private void setupAnimeSelectionSpinner() {
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
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
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
     * create the buttons for all testable services
     */
    private void createServiceButtons() {
        final LinearLayout container = findViewById(R.id.adapter_buttons_container);

        // add buttons to container
        for (Class<?> svcClass : testableServices) {
            // create and add button
            final Button btn = new Button(this);
            btn.setAllCaps(false);
            btn.setText(svcClass.getSimpleName());
            container.addView(btn);

            // set onclick listener
            btn.setOnClickListener(v -> testContentService(svcClass));
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
     * test a content adapter service, and show the result in the ui
     *
     * @param svcClass the service to bind and test
     */
    private void testContentService(@NonNull Class<?> svcClass) {
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
                testContentAdapter(ca, svcIntent);
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
     * @param ca        the adapter to test
     * @param svcIntent the service connection intent, to close after a result is received
     */
    private void testContentAdapter(@NonNull IContentAdapter ca, @NonNull Intent svcIntent) {
        // get handler on main thread
        final Handler mh = new Handler(getMainLooper());

        // run in a background thread
        new Thread(() -> {
            try {
                // request uri from the adapter
                ca.requestStreamUri(selectedAnime.malID, selectedAnime.enTitle, "", selectedAnime.episode, "", new IContentAdapterCallback.Stub() {
                    @Override
                    public void streamUriResult(String streamUri, String persistentStorage) {
                        mh.post(() -> {
                            // show results
                            setQueryResultViews(streamUri, persistentStorage);

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