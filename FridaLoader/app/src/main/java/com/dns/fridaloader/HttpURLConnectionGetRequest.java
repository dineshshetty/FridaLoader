package com.dns.fridaloader;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HttpURLConnectionGetRequest extends AsyncTask<String, Void, String> {
    String finalURL = "";
    @Override
    protected String doInBackground(String... parameters) {
        try {

            URL siteURL = new URL(parameters[0]);
           // System.out.println("YES HttpURLConnectionGetRequest ");
            HttpURLConnection httpConn = (HttpURLConnection) siteURL.openConnection();
            httpConn.setRequestMethod("GET");
            httpConn.connect();

            InputStream inputStream = httpConn.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line = bufferedReader.readLine();
           // System.out.println("content="+line);

            JSONObject jsonObj = new JSONObject(line);
            JSONArray jArray = jsonObj.getJSONArray("assets");
          //   System.out.println("jArray length="+jArray.length());

            for (int i=0; i < jArray.length(); i++)
            {
                try {
                    JSONObject oneObject = jArray.getJSONObject(i);
                     String oneObjectsItem = oneObject.getString("browser_download_url");
                    if(oneObjectsItem.contains("server") && oneObjectsItem.contains("android-x86.xz")){
                   //     System.out.println("Download Links = "+oneObjectsItem);
                        final Matcher matcher = Pattern.compile("download/").matcher(oneObjectsItem);
                        if(matcher.find()){
                    //        System.out.println("Matching is: "+oneObjectsItem.substring(matcher.end()).trim());
                            finalURL = oneObjectsItem.substring(matcher.end()).trim();
                        }
                    }
                } catch (JSONException e) {
                    // Oops
                }
            }



        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
      //  return "SUCCESS";
        return finalURL;
    }

    @Override
    protected void onPostExecute(String result){
        super.onPostExecute(result);
        System.out.println("Request Status: " + result);
    }


}
