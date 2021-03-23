package io.github.shadow578.tenshicontent;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import io.github.shadow578.tenshi.content.aidl.IContentAdapter;
import io.github.shadow578.tenshi.content.aidl.IContentAdapterCallback;
import io.github.shadow578.tenshicontent.fouranime.webview.FourAnimeWebViewAdapterService;

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        // test 4anime adapter
        findViewById(R.id.fouranimeanime_test_btn).setOnClickListener(v -> testService(FourAnimeWebViewAdapterService.class));
    }

    private void testService(@SuppressWarnings("SameParameterValue") Class<?> svcClass)
    {
        final Intent svcI = new Intent(this, svcClass);
        bindService(svcI, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Toast.makeText(TestActivity.this, "service connected", Toast.LENGTH_SHORT).show();
                testAdapter(IContentAdapter.Stub.asInterface(service));
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Toast.makeText(TestActivity.this, "service disconnected", Toast.LENGTH_SHORT).show();
            }
        }, BIND_AUTO_CREATE);
    }

    private void testAdapter(IContentAdapter ca) {
        new Thread(() -> {
            try {
                // test parameters
                final int MAL_ID = 41389;
                final String EN_TITLE = "Tonikaku Kawaii";
                final String JP_TITLE = "";
                final int EPISODE = 1;

                ca.requestStreamUri(MAL_ID, EN_TITLE, JP_TITLE, EPISODE, "tonikaku-kawaii", new IContentAdapterCallback() {
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