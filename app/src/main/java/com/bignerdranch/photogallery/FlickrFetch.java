package com.bignerdranch.photogallery;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetch {
    private static final String TAG = "FlickrFetch";

    private static final String API_KEY = "21e23938cdbb6639a286ec800a1d43b5";
    private static final String FETCH_RECENT_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT  = Uri.parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    public byte[] getUrlBytes(String urlSpec) throws IOException{
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if(connection.getResponseCode()!= HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage()+
                        ": with" + urlSpec);}
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while((bytesRead= in.read(buffer))>0){
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchRecentPhotos(int pageFetched){
        String url = buildUrl(FETCH_RECENT_METHOD, null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query, int pageFetched){
        String url = buildUrl(SEARCH_METHOD, query);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> downloadGalleryItems(String url){ //int is redunant

        List<GalleryItem> items = new ArrayList<>();

        try{
           /* String url = Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .build().toString();*/
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            //parseItems(items, jsonBody, url);

            Gson gson  = new Gson();
            GalleryPage.getGalleryPage();
            GalleryPage.sGalleryPage  = gson.fromJson(jsonString,GalleryPage.class);
            items   = GalleryPage.sGalleryPage.getGalleryItemList();

        } catch (IOException ioe){
            Log.e(TAG, "Failed to fetch items", ioe);
        } catch (JSONException je){
            Log.e(TAG, "Failed to parse JSON", je);
        }
        return items;
    }

    private String buildUrl (String method, String query){
     Uri.Builder uriBuilder = ENDPOINT.buildUpon()
             .appendQueryParameter("method", method);
     if (method.equals(SEARCH_METHOD)){
         uriBuilder.appendQueryParameter("text", query);
     }
     return uriBuilder.build().toString();
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody, String url)
        throws IOException, JSONException {

        Gson gson = new Gson();

       // GalleryPage.getGalleryPage();

     /*   String jsonString = getUrlString(url);
        GalleryPage.sGalleryPage = gson.fromJson(jsonString, GalleryPage.class);
        items = GalleryPage.sGalleryPage.getGalleryItemList();*/

        //code up there should be tested
        Type galleryItemType = new TypeToken<ArrayList<GalleryItem>>(){}.getType();


            JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
            JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");
            String jsonPhotosString = photoJsonArray.toString();
            List<GalleryItem>galleryItemList = gson.fromJson(jsonPhotosString, galleryItemType);
            items.addAll(galleryItemList);

           /* for (int i = 0; i<photoJsonArray.length(); i++){
                JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

                GalleryItem item = new GalleryItem();
                item.setId(photoJsonObject.getString("id"));
                item.setCaption(photoJsonObject.getString("title"));

                if(!photoJsonObject.has("url_s")){
                    continue;
                }

                item.setUrl(photoJsonObject.getString("url_s"));
                items.add(item);
            }*/
        }
}