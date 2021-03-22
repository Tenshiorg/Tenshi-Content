package io.github.shadow578.tenshicontent.fouranime;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.shadow578.tenshi.content.aidl.IContentAdapter;
import io.github.shadow578.tenshi.content.aidl.IContentAdapterCallback;

public class FourAnimeAdapter extends IContentAdapter.Stub {

    //TODO fetch from online JSON file for faster updates
    private static final String BASE_URL = "https://4anime.to/";
    private static final String SEARCH_URL = BASE_URL + "?s=";

    /**
     * application context
     */
    private final Context appContext;

    public FourAnimeAdapter(@NonNull Context ctx) {
        appContext = ctx;
    }

    @Override
    public void requestStreamUri(int malID, String enTitle, String jpTitle, int episode, String slug, IContentAdapterCallback callback) throws RemoteException {
        // find the anime slug if not in persistent storage
        boolean slugFromStorage = true;
        if (slug == null || slug.trim().length() <= 0) {
            slug = findAnimeSlug(enTitle);
            slugFromStorage = false;
        }

        // abort if no slug
        if (slug.trim().length() <= 0) {
            Log.e("TenshiC", "no slug found, abort!");
            callback.streamUriResult(null, slug);
            return;
        }

        // get episode url
        String episodeUrl = findEpisodeUrl(slug, episode);

        // if we did not find a episode url but loaded the slug from storage, retry with
        // freshly parsed slug
        if ((episodeUrl == null || episodeUrl.trim().length() <= 0) && slugFromStorage) {
            slug = findAnimeSlug(enTitle);
            episodeUrl = findEpisodeUrl(slug, episode);
        }

        // call callback
        callback.streamUriResult(episodeUrl, slug);
    }

    /**
     * use 4anime.to/?s= to search for a anime and get it's slug.
     * If more than one result is found, opens a activity
     *
     * @param enTitle the english title
     * @return the slug, or null if nothing was found
     */
    @NonNull
    private String findAnimeSlug(@NonNull String enTitle) {
        // build the search url
        final String searchUrl;
        try {
            searchUrl = SEARCH_URL + URLEncoder.encode(enTitle, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            return "";
        }

        //region Auto- parse (only works with some luck)
        try {
            // get the website
            final Document doc = Jsoup.connect(searchUrl).get();

            // get search results div
            final Elements searchResults = doc.select("div#headerDIV_2");

            // parse the results
            ArrayList<String> slugs = new ArrayList<>();
            final Pattern p = Pattern.compile("(?:4anime\\.to/anime/)(.+)");
            for (Element e : searchResults) {
                // get <a> element that links to the anime page
                final Element a = e.selectFirst("a");
                if (a == null)
                    continue;

                // try to get the href url from the element
                final String url = a.attr("abs:href");
                if (url == null || url.trim().length() <= 0)
                    continue;

                // get the slug
                final Matcher m = p.matcher(url);
                if (!m.find())
                    continue;

                final String slug = m.group(1);
                if (slug != null && !slug.trim().isEmpty())
                    slugs.add(slug);
            }

            // if we only found one slug, this is all we have to do
            if (slugs.size() == 1)
                return slugs.get(0);

            // if we have more than 1 slug, we try to guess the correct slug
            if (slugs.size() > 1) {
                final String guessedSlug = guessSlug(enTitle);
                if (slugs.contains(guessedSlug))
                    return guessedSlug;
            }
        } catch (IOException e) {
            Log.e("TenshiC", e.toString());
            e.printStackTrace();
        }
        //endregion

        //TODO: fallback to user selection stuff
        // maybe disable slug guessing then?
        return "";
    }

    @NonNull
    private String guessSlug(@NonNull String title){
        // slugs are lowercase
        title = title.toLowerCase();

        // replace spaces with dash
        title = title.replace(" ", "-");

        // remove everything that is not alphanumeric or dash
        StringBuilder slug = new StringBuilder();
        for (char c : title.toCharArray())
            if(Character.isAlphabetic(c) || Character.isDigit(c) || c == '-')
                slug.append(c);

        return slug.toString();
    }

    /**
     * find the episode url for the given anime and episode
     *
     * @param slug    the slug of the anime
     * @param episode the episode number
     * @return the episode url, or null if not found
     */
    @Nullable
    private String findEpisodeUrl(@NonNull String slug, int episode) {
        // build episode watch url
        final String watchUrl = String.format(Locale.ENGLISH, "%s%s-episode-%02d", BASE_URL, slug, episode);

        try {
            // connect to the page
            final Document doc = Jsoup.connect(watchUrl).get();

            // get playeround div
            final Element playeround = doc.getElementById("playeround");
            if (playeround == null)
                return null;

            // get source tag in video player
            final Element source = playeround.getElementsByTag("source").first();
            if (source == null)
                return null;

            // get src attribute
            String src = source.attr("src");
            if (src.trim().length() <= 0)
                return null;

            // fix url by replacing mountainoservoo002 with mountainoservo0002
            src = src.replace("mountainoservoo002", "mountainoservo0002");
            return src;
        } catch (IOException e) {
            Log.e("TenshiC", e.toString());
            e.printStackTrace();
        }
        return null;
    }
}
