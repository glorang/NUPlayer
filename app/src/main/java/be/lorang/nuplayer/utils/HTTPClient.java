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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import be.lorang.nuplayer.BuildConfig;

public class HTTPClient {

    private static String TAG = "HTTPClient";

    // Singleton instance
    private static HTTPClient instance = null;

    private HttpsURLConnection urlConnection ;
    private BufferedReader reader;
    private OutputStream writer;
    private JSONObject returnObject;
    private int responseCode;
    private String responseMessage;

    private HTTPClient() {}

    public static HTTPClient getInstance() {
        if(instance == null) {
            instance = new HTTPClient();

            // setup application wide CookieManager
            CookieHandler.setDefault(new CookieManager());
        }
        return instance;
    }

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
     *
     * @return JSONObject with result
     */
    private JSONObject doRequest(String urlString, String requestMethod, String contentType, JSONObject postData, Map<String, String> headers) throws IOException  {

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

                urlConnection.setRequestProperty("Content-Length", String.valueOf(data.length()));
                urlConnection.setDoOutput(true);

                // write POST body
                writer = urlConnection.getOutputStream();
                byte[] input = data.getBytes(StandardCharsets.UTF_8);
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

        } catch (Exception e) {
            Log.d(TAG, "Exception caught: " + e.getMessage());
            e.printStackTrace();

            // In case of an exception we set error code to 500 (Internal Server Error)
            // and the message to the one from the exception
            //responseCode = 500;
            //responseMessage = e.getMessage();

        } finally {
            urlConnection.disconnect();
        }

        return returnObject;
    }

    public JSONObject getRequest(String url) throws IOException{
        return doRequest(url, "GET", null, null, null);
    }

    public JSONObject getRequest(String url, Map<String, String> headers) throws IOException{
        return doRequest(url, "GET", null, null, headers);
    }

    public JSONObject postRequest(String url, String contentType, JSONObject postData) throws IOException {
        return doRequest(url, "POST", contentType, postData, null);
    }

    public JSONObject postRequest(String url, String contentType, JSONObject postData, Map<String, String> headers) throws IOException {
        return doRequest(url, "POST", contentType, postData, headers);
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

}
