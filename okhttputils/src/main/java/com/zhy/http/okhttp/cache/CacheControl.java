package com.zhy.http.okhttp.cache;

/**
 * Created by David on 2017/2/13.
 */

public class CacheControl {

    /**
     * 优先使用网络，失败再使用缓存
     */
    public static final int FIRST_NET = 0;
    /**
     * 先加载缓存（缓存时间永久，除非缓存超过设置文件大小而被清理），再同步请求网络
     */
    public static final int FIRST_CACHE = 1;
    /**
     * 强制使用网络
     */
    public static final int FORCE_NETWORK = 2;
    /**
     * 强制使用缓存
     */
    public static final int FORCE_CACHE = 3;

    /**
     * 当缓存没有超过默认设置值时，则不再请求网络，使用缓存
     * 例如设置缓存默认3分钟，当前一次网络请求后，在3分钟之内不会重新请求网络，而是使用缓存
     */
    public static final int NO_EXPIRE_USE_CACHE = 4;
}
