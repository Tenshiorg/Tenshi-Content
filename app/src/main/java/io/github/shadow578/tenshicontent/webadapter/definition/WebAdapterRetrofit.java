package io.github.shadow578.tenshicontent.webadapter.definition;

import java.util.List;

import io.github.shadow578.tenshicontent.webadapter.definition.model.WebAdapterDefinition;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * retrofit interface for web adapters
 */
public interface WebAdapterRetrofit {

    /**
     * load web adapter definitions from a json file
     * @param jsonUrl the json file to load
     * @return the adapter definitions
     */
    @GET
    Call<List<WebAdapterDefinition>> getDefinitions(@Url String jsonUrl);
}
