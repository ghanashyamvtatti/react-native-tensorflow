package com.rntensorflow;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.URLUtil;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.modules.network.OkHttpClientProvider;

import okhttp3.Request;
import okhttp3.Response;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceManager {

    private ReactContext reactContext;

    public ResourceManager(ReactContext reactContext) {
        this.reactContext = reactContext;
    }

    public String loadResourceAsString(String resource) {
        return new String(loadResource(resource));
    }

    public byte[] loadResource(String resource) {
        boolean isLocalFile = isResourceFile(resource);
        if (!isLocalFile && URLUtil.isValidUrl(resource)) {
            return loadFromUrl(resource);
        } else {
            return loadFromLocal(resource, isLocalFile);
        }
    }


    private boolean isResourceFile(String resource) {
        if (resource.startsWith("file://") || resource.startsWith("content:/")) {
            return true;
        }
        return false;
    }


    public String getRealPathFromURI(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = reactContext.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


    private byte[] loadFromLocal(String resource, Boolean isFile) {
        try {
            InputStream inputStream;
            if (isFile) {
                if (resource.startsWith("content:/")) {
                    resource = getRealPathFromURI(Uri.parse(resource));
                }
                inputStream = new FileInputStream(resource.replace("file://", ""));
            } else {
                int identifier = reactContext.getResources().getIdentifier(resource, "drawable", reactContext.getPackageName());
                inputStream = reactContext.getResources().openRawResource(identifier);
            }
            return inputStreamToByteArray(inputStream);
        } catch (IOException | Resources.NotFoundException e) {
            try {
                InputStream inputStream = reactContext.getAssets().open(resource);
                return inputStreamToByteArray(inputStream);
            } catch (IOException e1) {
                try {
                    InputStream inputStream = new FileInputStream(resource);
                    return inputStreamToByteArray(inputStream);
                } catch (IOException e2) {
                    throw new IllegalArgumentException("Could not load resource");
                }
            }
        }
    }

    private byte[] inputStreamToByteArray(InputStream inputStream) throws IOException {
        byte[] b = new byte[inputStream.available()];
        inputStream.read(b);
        return b;
    }

    private byte[] loadFromUrl(String url) {
        try {
            Request request = new Request.Builder().url(url).get().build();
            Response response = OkHttpClientProvider.createClient().newCall(request).execute();
            return response.body().bytes();
        } catch (IOException e) {
            throw new IllegalStateException("Could not fetch data from url " + url);
        }
    }
}
