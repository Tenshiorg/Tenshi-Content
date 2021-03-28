package io.github.shadow578.tenshicontent.webadapter;

import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import io.github.shadow578.tenshi.extensionslib.content.util.ActivityAdapterService;
import io.github.shadow578.tenshicontent.webadapter.definition.WebAdapterDefinitionsHelper;
import io.github.shadow578.tenshicontent.webadapter.definition.model.WebAdapterDefinition;

public class WebAdapterService extends ActivityAdapterService<WebAdapterActivity> {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // start loading definitions
        WebAdapterDefinitionsHelper.loadDefinitionsOnce(getApplicationContext(), Constants.DEFINITIONS_URL);

        // do normal bind stuff
        return super.onBind(intent);
    }

    /**
     * get the unique names included in this adapter
     *
     * @return the unique names in this adapter
     */
    @NonNull
    @Override
    protected String[] getUniqueNames() {
        final ArrayList<String> uniqueNames = new ArrayList<>();
        for (WebAdapterDefinition def : WebAdapterDefinitionsHelper.getAdapterDefinitions())
            uniqueNames.add(WebAdapterDefinitionsHelper.buildUniqueName(def));

        return uniqueNames.toArray(new String[0]);
    }

    /**
     * get the display name for a given unique name in this adapter
     *
     * @param uniqueName the unique name to get the display name of
     * @return the display name
     */
    @NonNull
    @Override
    protected String getDisplayName(@NonNull String uniqueName) {
        // find correct definition
        for (WebAdapterDefinition def : WebAdapterDefinitionsHelper.getAdapterDefinitions())
            if (uniqueName.equals(WebAdapterDefinitionsHelper.buildUniqueName(def))) {
                return def.displayName;
            }

        // fallback to just using the unique name
        return uniqueName;
    }

    /**
     * get the activity adapter activity to launch from this service
     *
     * @return the activity class
     */
    @NonNull
    @Override
    protected Class<WebAdapterActivity> getActivityClass() {
        return WebAdapterActivity.class;
    }
}
