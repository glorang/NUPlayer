/*
 * Copyright 2021 Geert Lorang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package be.lorang.nuplayer.services;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.bumptech.glide.load.HttpException;

import be.lorang.nuplayer.R;
import be.lorang.nuplayer.model.Category;
import be.lorang.nuplayer.model.CategoryList;
import be.lorang.nuplayer.utils.HTTPClient;
import be.lorang.nuplayer.model.ProgramList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


/*
 * This class will download the list of categories from the VRT.NU website and add them to the
 * CategoryList array
 *
 */

public class CategoryService extends IntentService {
    private static final String TAG = "CategoryService";
    public final static String BUNDLED_LISTENER = "listener";
    public final static String ACTION_GET_CATEGORIES = "getCategories";
    public final static String ACTION_SET_CATEGORIES = "setCategories";

    private HTTPClient httpClient = new HTTPClient();
    private Bundle resultData = new Bundle();

    public CategoryService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {

        ResultReceiver receiver = workIntent.getParcelableExtra(CatalogService.BUNDLED_LISTENER);

        try {

            String action = workIntent.getExtras().getString("ACTION");
            String categoryName = workIntent.getExtras().getString("CATEGORY_NAME", "");

            switch (action) {
                case ACTION_GET_CATEGORIES:
                    getCategories();
                    break;
                case ACTION_SET_CATEGORIES:
                    setCategories(categoryName);
                    break;
            }

            receiver.send(Activity.RESULT_OK, resultData);

        } catch (Exception e) {
            String message = "Could not get/set VRT.NU categories list: " + e.getMessage();
            Log.e(TAG, message);
            e.printStackTrace();
            resultData.putString("MSG", message);
            receiver.send(Activity.RESULT_CANCELED, resultData);
        }

    }

    private void getCategories() throws IOException, JSONException {

        CategoryList categoryList = CategoryList.getInstance();

        // Return immediately if categories list already set
        if(categoryList.getCategoriesCount() > 0){
            return;
        }

        // Get categories
        JSONObject returnObject = httpClient.getCachedRequest(getCacheDir(), getString(R.string.service_categories_url), 1440);

        if(httpClient.getResponseCode() != 200) {
            throw new HttpException(httpClient.getResponseCode() + ": " + httpClient.getResponseMessage());
        }

        //[":items"].par[":items"].categories.items
        JSONArray categories = returnObject
                .getJSONObject(":items")
                .getJSONObject("par")
                .getJSONObject(":items")
                .getJSONObject("categories")
                .getJSONArray("items");

        String imageServer = getString(R.string.model_image_server);

        for(int i=0;i<categories.length();i++) {

            JSONObject categoryJSON = categories.getJSONObject(i);

            String name = categoryJSON.getString("name");
            String title = categoryJSON.getString("title");
            String thumbnail = categoryJSON.optString("imageStoreUrl").replaceFirst("^(https:)?//images.vrt.be/orig/", "");

            Log.d(TAG, "adding category " + title);

            Category category = new Category(
                    name,
                    title,
                    thumbnail,
                    imageServer
            );

            categoryList.addCategory(category);
        }

    }

    private void setCategories(String category) throws IOException, JSONException {

        if(category.length() == 0) { return; }

        // Get all Programs of certain category
        String url = String.format(getString(R.string.service_categories_program_url), category);
        JSONObject returnObject = httpClient.getCachedRequest(getCacheDir(), url, 60);

        if(httpClient.getResponseCode() != 200) {
            throw new HttpException(httpClient.getResponseCode() + ": " + httpClient.getResponseMessage());
        }

        JSONArray data = returnObject.getJSONArray("data");
        ProgramList programList = ProgramList.getInstance();

        for(int i=0;i<data.length();i++) {
            JSONObject program = data.getJSONObject(i);
            String programName = program.getString("programName");
            Log.d(TAG, "Adding category " + category + " to " + programName);
            programList.setProgramCategory(programName, category);
        }

    }
}
