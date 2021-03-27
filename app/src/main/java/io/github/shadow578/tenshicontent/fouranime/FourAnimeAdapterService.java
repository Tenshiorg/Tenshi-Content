package io.github.shadow578.tenshicontent.fouranime;

import androidx.annotation.NonNull;

import io.github.shadow578.tenshi.extensionslib.content.util.ActivityAdapterService;

/**
 * ActivityAdapter service for {@link FourAnimeAdapterActivity}
 */
public class FourAnimeAdapterService extends ActivityAdapterService<FourAnimeAdapterActivity> {

    /**
     * get the activity adapter activity to launch from this service
     *
     * @return the activity class
     */
    @NonNull
    @Override
    protected Class<FourAnimeAdapterActivity> getActivityClass() {
        return FourAnimeAdapterActivity.class;
    }
}
