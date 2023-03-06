package com.dns.fridaloader;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HttpURLConnectionGetRequest extends AsyncTask<String, Void, String> {
    String finalURL = "";
    String deviceArchitectureType = "x86";

    @Override
    protected String doInBackground(String... parameters) {
        try {
            URL siteURL = new URL(parameters[0]);
            switch (parameters[1].trim()) {
                case "x86_64":
                    deviceArchitectureType = "x86_64" ;
                    break;
                case "arm64-v8a":
                    deviceArchitectureType = "arm64";
                    break;
                case "armeabi":
                case "armeabi-v7a":
                    deviceArchitectureType = "arm";
                    break;
                default:
                    deviceArchitectureType = "x86";
            }
            System.out.println("XXXX Setting Architecture to " + deviceArchitectureType);
            HttpURLConnection httpConn = (HttpURLConnection) siteURL.openConnection();
            httpConn.setRequestMethod("GET");
            httpConn.connect();
            InputStream inputStream = httpConn.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line = bufferedReader.readLine();
            JSONObject jsonObj = new JSONObject(line);
            JSONArray jArray = jsonObj.getJSONArray("assets");
            for (int i = 0; i < jArray.length(); i++) {
                try {
                    JSONObject oneObject = jArray.getJSONObject(i);
                    String oneObjectsItem = oneObject.getString("browser_download_url");
                    if (oneObjectsItem.contains("server") && oneObjectsItem.contains("android-" + deviceArchitectureType + ".xz")) {
                        final Matcher matcher = Pattern.compile("download/").matcher(oneObjectsItem);
                        if (matcher.find()) {
                            System.out.println("XXXXMatching is: " + oneObjectsItem.substring(matcher.end()).trim());
                            finalURL = oneObjectsItem.substring(matcher.end()).trim();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    return "ERROR JSONObject for procedure.";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
        return finalURL;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        System.out.println("Request Status: " + result);
    }
}
