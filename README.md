CacheOkttpUtils
===============
***CacheOkttpUtils 基于鸿洋OkHttpUtils，增加多种http缓存。***
- - -
- 功能说明：

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


- - -
- 使用说明：
**集成：**

第一步：

root gradle:

	allprojects {
    	     repositories {
        	maven { url 'https://jitpack.io' }
        	jcenter()
    	    }
	}

第二步：

	compile 'com.github.huangyanbin:CacheOkttpUtils:7820d5bd18'

**1.首先初始化建议在Application onCreate();**


    public void initOkHttpClient(boolean isLogDisable, String logName,CacheInterceptor cacheInterceptor){

        File cacheFile = new File(mApplication.getExternalCacheDir(),"httpCache");
        Cache cache = new Cache(cacheFile,1024*1024*50);
         OkHttpClient.Builder builder= new OkHttpClient.Builder();
                if(isLogDisable){
                    builder.addInterceptor(new LoggerInterceptor(logName));
                }
        OkHttpClient okHttpClient =builder.addNetworkInterceptor(cacheInterceptor)
                .connectTimeout(8000, TimeUnit.MILLISECONDS)
                .writeTimeout(8000,TimeUnit.MILLISECONDS)
                .readTimeout(8000,TimeUnit.MILLISECONDS)
                .addInterceptor(cacheInterceptor)
                .cache(cache)
                .build();
        OkHttpUtils.initClient(okHttpClient);

