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
package be.lorang.nuplayer.utils;

/*
 * Helper class for all HTTP requests
 */

import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import be.lorang.nuplayer.BuildConfig;
import be.lorang.nuplayer.R;

import static be.lorang.nuplayer.services.CatalogService.programTypes;

public class HTTPClient {

    private static String TAG = "HTTPClient";

    private HttpsURLConnection urlConnection ;
    private BufferedReader reader;
    private OutputStream writer;
    private JSONObject returnObject;
    private int responseCode;
    private String responseMessage;

    public HTTPClient() {}

    /**
     * Perform GET/POST request
     *
     * Cookies are stored automatically in the global cookieManager
     * The HTTP response code (200, 404, etc) is stored in returnCode
     * The HTTP response message is stored in returnMessage
     *
     * All of them can be queried via the respective get methods
     *
     * @param urlString - String: URL
     * @param requestMethod - String: request method
     *                      (GET/POST)
     * @param contentType - String: content type
     *                    (application/json, application/x-www-form-urlencoded)
     * @param postData - JSONObject: data for POST request
     * @param headers - HashMap: additional headers to set
     * @param cacheDir - File: File object pointing to cache dir (getContext().getCacheDir())
     * @param ttl - Integer: Time To Live (ttl) for cached request in minutes
     *
     * @return JSONObject with result
     */
    private JSONObject doRequest(String urlString, String requestMethod, String contentType, JSONObject postData, Map<String, String> headers, File cacheDir, int ttl) throws IOException  {

        try {

            java.net.URL url = new java.net.URL(urlString);

            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Accept", "*/*");
            urlConnection.setRequestProperty("User-Agent", "NUPlayer/" + BuildConfig.VERSION_NAME);
            urlConnection.setFollowRedirects(true);

            // add request method (if set)
            if(requestMethod != null) {
                Log.d(TAG, "Setting requestMethod: " + requestMethod);
                urlConnection.setRequestMethod(requestMethod);
            }

            // add headers (if any)
            if(headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    Log.d(TAG, "Adding header: " + header.getKey() + " with value: " + header.getValue());
                    urlConnection.addRequestProperty(header.getKey(), header.getValue());
                }
            }

            // set content type header (if set)
            if(contentType != null) {
                Log.d(TAG, "Setting contentType: " + contentType);
                urlConnection.setRequestProperty("Content-Type", contentType);
            }

            if(requestMethod.equals("POST")) {

                // transform JSON postData to an URL encoded string if type is of x-www-form-urlencoded
                String data;
                if(contentType.equals("application/x-www-form-urlencoded")) {
                    StringBuilder stringBuilder = new StringBuilder();
                    String separator = "";

                    for (int i = 0; i < postData.names().length(); i++) {
                        String key = postData.names().getString(i);
                        String value = postData.getString(key);
                        stringBuilder.append(separator);
                        stringBuilder.append(URLEncoder.encode(key, "UTF-8") + "=" +
                                 URLEncoder.encode(value, "UTF-8"));
                        separator = "&";
                    }
                    data = stringBuilder.toString();
                } else {
                   data = postData.toString();
                }

                byte[] input = data.getBytes(StandardCharsets.UTF_8);

                urlConnection.setRequestProperty("Content-Length", String.valueOf(input.length));
                urlConnection.setDoOutput(true);

                // write POST body
                writer = urlConnection.getOutputStream();
                writer.write(input, 0, input.length);
                writer.flush();
                writer.close();
            }

            // set return code and message
            responseCode = urlConnection.getResponseCode();
            responseMessage = urlConnection.getResponseMessage();

            // read response
            StringBuilder sb = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            // parse response to JSON
            String json = sb.toString();

            // the VRT.NU API sometimes returns JSONObjects (e.g. Episode lists)
            // but sometimes also JSONArrays (e.g. Catalog)
            // If it's an array add it to a single JSONObject called "data"

            Object jsonTestObj = null;
            try {
                jsonTestObj = new JSONTokener(json).nextValue();
            } catch(JSONException e) {
                // ignore exception as we're testing what the Object would be
            }

            if (jsonTestObj instanceof JSONObject) {
                returnObject = new JSONObject(json);
            } else if(jsonTestObj instanceof JSONArray) {
                returnObject = new JSONObject();
                JSONArray jsonArr = new JSONArray(json);
                returnObject.put("data", jsonArr);
            }

            // Write result to cache for later use
            if(cacheDir != null && requestMethod.equals("GET") && responseCode == 200) {

                // Setup cache object as JSON with current timestamp
                JSONObject cacheObject = new JSONObject();
                cacheObject.put("timestampCacheExpires", (System.currentTimeMillis() + (ttl * 60 * 1000)));
                cacheObject.put("object", returnObject.toString());

                Log.d(TAG, "Writing to cache: " + cacheObject.toString().substring(0,100));

                String fileName = getCacheFileName(urlString);
                File file = new File(cacheDir, fileName);
                Writer output = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(file), StandardCharsets.UTF_8));
                output.write(cacheObject.toString());
                output.close();
            }

        } catch (Exception e) {
            Log.d(TAG, "Exception caught: " + e.getMessage());
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
        }

        return returnObject;
    }

    public JSONObject getRequest(String url) throws IOException{
        return doRequest(url, "GET", null, null, null, null, 0);
    }

    public JSONObject getRequest(File cacheDir, String url, int ttl) throws IOException{
        return doRequest(url, "GET", null, null, null, cacheDir, ttl);
    }

    public JSONObject getRequest(String url, Map<String, String> headers) throws IOException{
        return doRequest(url, "GET", null, null, headers, null, 0);
    }

    public JSONObject postRequest(String url, String contentType, JSONObject postData) throws IOException {
        return doRequest(url, "POST", contentType, postData, null, null, 0);
    }

    public JSONObject postRequest(String url, String contentType, JSONObject postData, Map<String, String> headers) throws IOException {
        return doRequest(url, "POST", contentType, postData, headers, null, 0);
    }

    // Return cached responses, ttl in minutes
    public JSONObject getCachedRequest(File cacheDir, String url, int ttl) throws IOException {
        String fileName = getCacheFileName(url);

        File file = new File(cacheDir, fileName);
        if(file.exists()) {

            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();

            String input = new String(data, StandardCharsets.UTF_8);
            try {
                JSONObject cacheObject = new JSONObject(input);
                Timestamp timestampCacheExpires = new Timestamp(cacheObject.getLong("timestampCacheExpires"));
                Instant cacheExpires = timestampCacheExpires.toInstant();
                JSONObject object = new JSONObject(cacheObject.getString("object"));

                // check if cache entry still valid
                if(Instant.now().isBefore(cacheExpires)) {
                    Log.d(TAG, "Returning cached object - Cache still valid until " + cacheExpires.toString() + " result for " + url);
                    responseCode = 200;
                    return object;
                }

            } catch(Exception e) {
                return getRequest(cacheDir, url, ttl);
            }
        }

        return getRequest(cacheDir, url, ttl);
    }

    public JSONObject getCachedRequest(File cacheDir, String url) throws IOException {
        return getCachedRequest(cacheDir, url, 30);
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public CookieManager getCookies() {
        return (CookieManager)CookieHandler.getDefault();
    }

    private static String getCacheFileName(String url) {
        return "url-cache-" + Utils.sha256(url) + ".json";
    }

    private static File[] getCacheFiles(File cacheDir) {
        File[] cacheFiles = cacheDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("url-cache-") && name.endsWith(".json");
            }
        });

        return cacheFiles;
    }

    // Clear all caches
    public static void clearCache(File cacheDir) {

        for(File file : getCacheFiles(cacheDir)) {
            file.delete();
        }

    }

    // Clear all expired cache files
    public static void clearExpiredCache(File cacheDir) {

        for (File file : getCacheFiles(cacheDir)) {
            if (file.exists()) {
                try {
                    FileInputStream fis = new FileInputStream(file);
                    byte[] data = new byte[(int) file.length()];
                    fis.read(data);
                    fis.close();

                    String input = new String(data, StandardCharsets.UTF_8);

                    JSONObject cacheObject = new JSONObject(input);
                    Timestamp timestampCacheExpires = new Timestamp(cacheObject.getLong("timestampCacheExpires"));
                    Instant cacheExpires = timestampCacheExpires.toInstant();

                    // check if cache entry still valid
                    if (Instant.now().isBefore(cacheExpires)) {
                        Log.d(TAG, "Removing expired cache file: " + file.getName() + " which expired on " + cacheExpires);
                        file.delete();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Clear Catalog + Favorites cache
    public static void clearCatalogCache(File cacheDir, String catalogURL, String favoritesURL) {

        // Catalog cache
        for(String programType : programTypes) {
            String url = String.format(catalogURL, programType);
            String fileName = getCacheFileName(url);
            File file = new File(cacheDir, fileName);

            if(file.exists()) {
                file.delete();
            }
        }

        // Favorites cache
        String fileName = getCacheFileName(favoritesURL);
        File file = new File(cacheDir, fileName);
        if(file.exists()) {
            file.delete();
        }

    }

    public static String getCacheStatistics(File cacheDir) {

        File[] cacheFiles = getCacheFiles(cacheDir);

        int cacheCount = cacheFiles.length;
        long totalSize = 0;

        for(File file : cacheFiles) {
            totalSize += (file.length() / 1024);
        }

        return cacheCount + " items, total size: " + totalSize + "KiB";
    }
}
