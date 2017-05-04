package com.zhy.http.okhttp;

import android.util.Log;

import com.zhy.http.okhttp.builder.GetBuilder;
import com.zhy.http.okhttp.builder.HeadBuilder;
import com.zhy.http.okhttp.builder.OtherRequestBuilder;
import com.zhy.http.okhttp.builder.PostFileBuilder;
import com.zhy.http.okhttp.builder.PostFormBuilder;
import com.zhy.http.okhttp.builder.PostStringBuilder;
import com.zhy.http.okhttp.cache.CacheControl;
import com.zhy.http.okhttp.callback.Callback;
import com.zhy.http.okhttp.request.RequestCall;
import com.zhy.http.okhttp.utils.Platform;

import java.io.IOException;
import java.util.concurrent.Executor;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * Created by zhy on 15/8/17.
 */
public class OkHttpUtils
{
    public static final long DEFAULT_MILLISECONDS = 10_000L;
    private volatile static OkHttpUtils mInstance;
    private OkHttpClient mOkHttpClient;
    private Platform mPlatform;

    public OkHttpUtils(OkHttpClient okHttpClient)
    {
        if (okHttpClient == null)
        {
            mOkHttpClient = new OkHttpClient();
        } else
        {
            mOkHttpClient = okHttpClient;
        }

        mPlatform = Platform.get();
    }


    public static OkHttpUtils initClient(OkHttpClient okHttpClient)
    {
        if (mInstance == null)
        {
            synchronized (OkHttpUtils.class)
            {
                if (mInstance == null)
                {
                    mInstance = new OkHttpUtils(okHttpClient);
                }
            }
        }
        return mInstance;
    }

    public static OkHttpUtils getInstance()
    {
        return initClient(null);
    }


    public Executor getDelivery()
    {
        return mPlatform.defaultCallbackExecutor();
    }

    public OkHttpClient getOkHttpClient()
    {
        return mOkHttpClient;
    }

    public static GetBuilder get()
    {
        return new GetBuilder();
    }

    public static PostStringBuilder postString()
    {
        return new PostStringBuilder();
    }

    public static PostFileBuilder postFile()
    {
        return new PostFileBuilder();
    }

    public static PostFormBuilder post()
    {
        return new PostFormBuilder();
    }

    public static OtherRequestBuilder put()
    {
        return new OtherRequestBuilder(METHOD.PUT);
    }

    public static HeadBuilder head()
    {
        return new HeadBuilder();
    }

    public static OtherRequestBuilder delete()
    {
        return new OtherRequestBuilder(METHOD.DELETE);
    }

    public static OtherRequestBuilder patch()
    {
        return new OtherRequestBuilder(METHOD.PATCH);
    }

    public void execute(final RequestCall requestCall, Callback callback) {
        Call call;
        switch (requestCall.getCacheControl()){
            case CacheControl.FIRST_CACHE:
            case CacheControl.NO_EXPIRE_USE_CACHE:
                executeLoadCache(requestCall,callback);
                return;
            case CacheControl.FORCE_CACHE:
                call = requestCall.getCacheCall();
                break;
            default:
                call = requestCall.getCall();
                break;
        }
        if(call != null) {
            executeFirst(requestCall, call, callback);
        }
    }


    public void executeFirst(final RequestCall requestCall,Call call, Callback callback){
        if (callback == null)
            callback = Callback.CALLBACK_DEFAULT;
        final Callback finalCallback = callback;
        final int id = requestCall.getOkHttpRequest().getId();
        call.enqueue(new okhttp3.Callback()
        {
            @Override
            public void onFailure(Call call, final IOException e)
            {
                sendFailResultCallback(requestCall,call, e, finalCallback, id);
            }

            @Override
            public void onResponse(final Call call, final Response response)
            {
                try
                {
                    if (call.isCanceled())
                    {
                        sendFailResultCallback(requestCall,call, new IOException("Canceled!"), finalCallback, id);
                        return;
                    }

                    if (!finalCallback.validateReponse(response, id))
                    {
                        sendFailResultCallback(requestCall,call, new IOException("request failed , reponse's code is : " + response.code()), finalCallback, id);
                        return;
                    }

                    Object o = finalCallback.parseNetworkResponse(response, id);
                    sendSuccessResultCallback(o, finalCallback, id);
                } catch (Exception e)
                {
                    sendFailResultCallback(requestCall,call, e, finalCallback, id);
                } finally
                {
                    if (response.body() != null)
                        response.body().close();
                }

            }
        });
    }


    /**
     * 执行先加载缓存
     * @param requestCall
     * @param callback
     */
    private void executeLoadCache(final RequestCall requestCall, Callback callback)
    {
        if(requestCall ==null){
            return;
        }
        if (callback == null)
            callback = Callback.CALLBACK_DEFAULT;
        final Callback finalCallback = callback;
        final int id = requestCall.getOkHttpRequest().getId();
        final Call cacheCall =requestCall.getCacheCall();
        if(cacheCall != null) {
            requestCall.getCacheCall().enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, final IOException e) {
                    sendCacheFailResultCallback(requestCall,call, e, finalCallback, id);
                }

                @Override
                public void onResponse(final Call call, final Response response) {
                    try {
                        if (call.isCanceled()) {
                            sendCacheFailResultCallback(requestCall,call, new IOException("Canceled!"), finalCallback, id);
                            return;
                        }
                        if (!finalCallback.validateReponse(response, id)) {
                            sendCacheFailResultCallback(requestCall,call, new IOException("request failed , reponse's code is : " + response.code()), finalCallback, id);
                            return;
                        }
                        Object o = finalCallback.parseNetworkResponse(response, id);
                        sendCacheSuccessResultCallback(cacheCall,o, finalCallback, id);
                         if(requestCall.getCacheControl() == CacheControl.FIRST_CACHE){
                             executeFirst(requestCall,requestCall.getCall(),finalCallback);
                        }else if(requestCall.getCacheControl() == CacheControl.NO_EXPIRE_USE_CACHE) {
                            int responseSeconds = (int) (( System.currentTimeMillis() - response.receivedResponseAtMillis())/1000);
                            if(responseSeconds > requestCall.getCacheExpireSeconds()){
                                executeFirst(requestCall,requestCall.getCall(),finalCallback);
                            }
                        }
                    } catch (Exception e) {
                        sendCacheFailResultCallback(requestCall,call, e, finalCallback, id);
                    } finally {
                        if (response.body() != null)
                            response.body().close();
                    }

                }
            });
        }
    }

    public void sendFailResultCallback(final RequestCall requestCall,final Call call, final Exception e, final Callback callback, final int id) {
        if (callback == null) return;

        mPlatform.execute(new Runnable()
        {
            @Override
            public void run() {
                callback.onError(call, e, id);
                callback.onAfter(id);
                if(requestCall != null && requestCall.getCacheControl() == CacheControl.FIRST_NET) {
                    executeLoadCache(requestCall, callback);
                }
            }
        });
    }



    public void sendSuccessResultCallback(final Object object, final Callback callback, final int id)
    {
        if (callback == null) return;
        mPlatform.execute(new Runnable()
        {
            @Override
            public void run()
            {
                callback.onResponse(object, id);
                callback.onAfter(id);
            }
        });
    }

    /**
     * 缓存的请求返回失败处理
     * @param call
     * @param e
     * @param callback
     * @param id
     */

    public void sendCacheFailResultCallback(final RequestCall requestCall,final Call call, final Exception e, final Callback callback, final int id)
    {
        if (callback == null) return;

        mPlatform.execute(new Runnable()
        {
            @Override
            public void run()
            {
               callback.onNoCache(call,e,id);
            }
        });
        if(requestCall != null && (requestCall.getCacheControl() == CacheControl.NO_EXPIRE_USE_CACHE
                || requestCall.getCacheControl() == CacheControl.FIRST_CACHE)) {
            executeFirst(requestCall,requestCall.getCall(),callback);
        }
    }

    /**
     * 缓存的请求返回成功处理
     * @param object
     * @param callback
     * @param id
     */
    public void sendCacheSuccessResultCallback(final Call call,final Object object, final Callback callback, final int id)
    {
        if (callback == null) return;

        mPlatform.execute(new Runnable()
        {
            @Override
            public void run()
            {
               callback.onCacheResponse(call,object,id);
            }
        });
    }
    public void cancelTag(Object tag)
    {
        for (Call call : mOkHttpClient.dispatcher().queuedCalls())
        {
            if (tag.equals(call.request().tag()))
            {
                call.cancel();
            }
        }
        for (Call call : mOkHttpClient.dispatcher().runningCalls())
        {
            if (tag.equals(call.request().tag()))
            {
                call.cancel();
            }
        }
    }

    public static class METHOD
    {
        public static final String HEAD = "HEAD";
        public static final String DELETE = "DELETE";
        public static final String PUT = "PUT";
        public static final String PATCH = "PATCH";
    }
}

