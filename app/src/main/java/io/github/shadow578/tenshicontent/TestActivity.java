package io.github.shadow578.tenshicontent;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import io.github.shadow578.tenshi.content.aidl.IContentAdapter;
import io.github.shadow578.tenshi.content.aidl.IContentAdapterCallback;
import io.github.shadow578.tenshicontent.fouranime.FourAnimeAdapter;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        // test 4anime adapter
        findViewById(R.id.fouranimeanime_test_btn).setOnClickListener(v -> testAdapter(new FourAnimeAdapter(getApplicationContext())));
    }

    private void testAdapter(IContentAdapter ca) {
        new Thread(() -> {
            try {
                // test parameters
                final int MAL_ID = 41389;
                final String EN_TITLE = "Tonikaku Kawaii";//TODO: dont use alternative_titles.en, but just title
                final String JP_TITLE = "wtf not useful here :P";
                final int EPISODE = 1;

                ca.requestStreamUri(MAL_ID, EN_TITLE, JP_TITLE, EPISODE, "", new IContentAdapterCallback() {
                    @Override
                    public void streamUriResult(String streamUri, String persistentStorage) throws RemoteException {
                        final Handler h = new Handler(getMainLooper());
                        h.post(() -> {
                            Toast.makeText(TestActivity.this, "" + streamUri, Toast.LENGTH_SHORT).show();
                            Log.i("TenshiContent", "url: " + streamUri);
                            Log.i("TenshiContent", "ps: " + persistentStorage);
                        });
                    }

                    @Override
                    public IBinder asBinder() {
                        return null;
                    }
                });

            } catch (Exception e) {
                Log.e("TenshiContent", e.toString());
                e.printStackTrace();
            }
        }).start();
    }
}