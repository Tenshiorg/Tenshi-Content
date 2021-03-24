package io.github.shadow578.tenshicontent.animixplay;

import androidx.annotation.NonNull;

import io.github.shadow578.tenshicontent.util.activityadapter.ActivityAdapterService;

/**
 * ActivityAdapter service for {@link AniMixPlayAdapterActivity}
 */
public class AniMixPlayAdapterService extends ActivityAdapterService<AniMixPlayAdapterActivity> {
    /**
     * get the activity adapter activity to launch from this service
     *
     * @return the activity class
     */
    @NonNull
    @Override
    protected Class<AniMixPlayAdapterActivity> getActivityClass() {
        return AniMixPlayAdapterActivity.class;
    }
}
