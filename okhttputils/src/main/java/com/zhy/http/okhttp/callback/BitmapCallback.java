package com.zhy.http.okhttp.callback;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by zhy on 15/12/14.
 */
public abstract class BitmapCallback extends Callback<Bitmap>
{
    @Override
    public Bitmap parseNetworkResponse(Response response , int id) throws Exception
    {
        return BitmapFactory.decodeStream(response.body().byteStream());
    }

    @Override
    public void onCacheResponse(Call call,Bitmap response, int id) {

    }

    @Override
    public void onNoCache(Call call, Exception e, int id) {

    }

}
