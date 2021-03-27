package io.github.shadow578.tenshicontent.yugenanime;

import androidx.annotation.NonNull;

import io.github.shadow578.tenshi.extensionslib.content.util.ActivityAdapterService;

/**
 * ActivityAdapter service for {@link YugenAnimeAdapterActivity}
 */
public class YugenAnimeAdapterService extends ActivityAdapterService<YugenAnimeAdapterActivity> {
    /**
     * get the activity adapter activity to launch from this service
     *
     * @return the activity class
     */
    @NonNull
    @Override
    protected Class<YugenAnimeAdapterActivity> getActivityClass() {
        return YugenAnimeAdapterActivity.class;
    }
}
