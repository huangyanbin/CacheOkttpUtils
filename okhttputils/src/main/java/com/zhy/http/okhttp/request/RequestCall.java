package com.zhy.http.okhttp.request;

import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.cache.CacheControl;
import com.zhy.http.okhttp.callback.Callback;

import java.io.IOException;
import java.net.CacheRequest;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by zhy on 15/12/15.
 * 对OkHttpRequest的封装，对外提供更多的接口：cancel(),readTimeOut()...
 */
public class RequestCall {
    private OkHttpRequest okHttpRequest;
    private Request request;
    private Request cacheRequest;
    private Call call;
    private Call cacheCall;
    private long readTimeOut;
    private long writeTimeOut;
    private long connTimeOut;
    private OkHttpClient clone;
    private int cacheControl;
    private int cacheExpireSeconds = 60;

    public RequestCall(OkHttpRequest request) {
        this.okHttpRequest = request;
    }

    public RequestCall readTimeOut(long readTimeOut) {
        this.readTimeOut = readTimeOut;
        return this;
    }

    public RequestCall cacheControl(int cacheControl) {
        this.cacheControl = cacheControl;
        return this;
    }

    public RequestCall writeTimeOut(long writeTimeOut) {
        this.writeTimeOut = writeTimeOut;
        return this;
    }

    public RequestCall connTimeOut(long connTimeOut) {
        this.connTimeOut = connTimeOut;
        return this;
    }

    public int getCacheExpireSeconds() {
        return cacheExpireSeconds;
    }

    public RequestCall setCacheExpireSeconds(int cacheExpireSeconds) {
        this.cacheExpireSeconds = cacheExpireSeconds;
        return this;
    }

    public Call buildCall(Callback callback) {
        request = generateRequest(callback);

        if (readTimeOut > 0 || writeTimeOut > 0 || connTimeOut > 0) {
            readTimeOut = readTimeOut > 0 ? readTimeOut : OkHttpUtils.DEFAULT_MILLISECONDS;
            writeTimeOut = writeTimeOut > 0 ? writeTimeOut : OkHttpUtils.DEFAULT_MILLISECONDS;
            connTimeOut = connTimeOut > 0 ? connTimeOut : OkHttpUtils.DEFAULT_MILLISECONDS;

            clone = OkHttpUtils.getInstance().getOkHttpClient().newBuilder()
                    .readTimeout(readTimeOut, TimeUnit.MILLISECONDS)
                    .writeTimeout(writeTimeOut, TimeUnit.MILLISECONDS)
                    .connectTimeout(connTimeOut, TimeUnit.MILLISECONDS)
                    .build();

            call = clone.newCall(request);
        } else {
            call = OkHttpUtils.getInstance().getOkHttpClient().newCall(request);
        }
        return call;
    }

    public Call buildCacheCall(Callback callback) {
        cacheRequest = generateCacheRequest(callback);
        if(cacheRequest !=null) {
            cacheCall = OkHttpUtils.getInstance().getOkHttpClient().newCall(cacheRequest);
        }
        return cacheCall;
    }


    private Request generateRequest(Callback callback) {
        return okHttpRequest.generateRequest(callback);
    }

    private Request generateCacheRequest(Callback callback) {
        return okHttpRequest.generateCacheRequest(callback);
    }

    public void execute(Callback callback) {
        buildCall(callback);
        if(cacheControl != CacheControl.FORCE_NETWORK){
            buildCacheCall(callback);
        }
        if (callback != null) {
            callback.onBefore(request, getOkHttpRequest().getId());
        }

        OkHttpUtils.getInstance().execute(this, callback);
    }

    public Call getCall() {
        return call;
    }

    public Request getRequest() {
        return request;
    }

    public Request getCacheRequest(){
        return cacheRequest;
    }

    public OkHttpRequest getOkHttpRequest() {
        return okHttpRequest;
    }

    public Response execute() throws IOException {
        if(cacheControl != CacheControl.FORCE_NETWORK){
            buildCacheCall(null);
        }
        buildCall(null);
        return call.execute();
    }

    public void cancel() {

        if(cacheCall !=null){
            cacheCall.cancel();
        }
        if (call != null) {
            call.cancel();
        }
    }

    public Call getCacheCall() {
        return cacheCall;
    }


    public int getCacheControl() {
        return cacheControl;
    }
}
