package com.example.administrator.osmapitest.indoormap;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.administrator.osmapitest.indoormap.IndoorMap;
import com.example.administrator.osmapitest.shared.Status;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Request indoor map from server
 * There are three situations for requesting indoor map:
 * 1. Outdoor to indoor
 * 2. Switch  floor
 * 3. Stay in the room at beginning
 */
public class GetIndoorMapService extends IntentService {
    private static String TAG = "GetIndoorMapService";
    private Context mContext;
    private IndoorMapCallback mIndoorMapCallback;

    public GetIndoorMapService() {
        super("GetIndoorMapService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        mIndoorMapCallback = new IndoorMapCallback();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String floor = String.valueOf(Objects.requireNonNull(intent).getStringExtra("floor"));  // floor of the indoor map
        String longitude = Objects.requireNonNull(intent).getStringExtra("longitude");
        String latitude = Objects.requireNonNull(intent).getStringExtra("latitude");

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        // 构造请求对象
        Request getIndoorMapRequest = new Request.Builder()
                // .url("http://quantum.s1.natapp.cc/SeamlessPositioning/servlet/IndoorMapHttpServlet")
                .url("http://quantum.s1.natapp.cc/servlet/IndoorMapHttpServlet")
                .addHeader("floor", floor)
                .addHeader("latitude", latitude)
                .addHeader("longitude", longitude)
                .build();
        client.newCall(getIndoorMapRequest).enqueue(mIndoorMapCallback);

    }

    private class IndoorMapCallback implements Callback {
        @Override
        public void onFailure(Call call, IOException e) {
            e.printStackTrace();
            Log.e(TAG, "接收室内地图失败" + e.getMessage(), e);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            String mapStr = response.body().string();
            Intent mapIntent;
            if (mapStr.length() == 0 && Status.isAlterIndoorMapIsNotExist()) {
                mapIntent = new Intent("no_map");
                mContext.sendBroadcast(mapIntent);
            } else {
                IndoorMap indoorMap = new IndoorMap(mapStr);
                // Log.e(TAG, indoorMap.getBaseMap().toString());
                // 将室内地图通过广播发送出去
                mapIntent = new Intent("indoor_map");
                mapIntent.putExtra("indoor_map", indoorMap);
                mContext.sendBroadcast(mapIntent);
                // Log.e(TAG, "发送室内地图");
            }
        }
    }
}



