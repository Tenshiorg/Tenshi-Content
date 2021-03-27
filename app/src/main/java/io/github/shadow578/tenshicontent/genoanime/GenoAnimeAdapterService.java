package io.github.shadow578.tenshicontent.genoanime;

import androidx.annotation.NonNull;

import io.github.shadow578.tenshi.extensionslib.content.util.ActivityAdapterService;

/**
 * ActivityAdapter service for {@link GenoAnimeAdapterActivity}
 */
public class GenoAnimeAdapterService extends ActivityAdapterService<GenoAnimeAdapterActivity> {
    /**
     * get the activity adapter activity to launch from this service
     *
     * @return the activity class
     */
    @NonNull
    @Override
    protected Class<GenoAnimeAdapterActivity> getActivityClass() {
        return GenoAnimeAdapterActivity.class;
    }
}
