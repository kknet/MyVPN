package com.androapplite.shadowsocks.service;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;

import com.androapplite.shadowsocks.BuildConfig;
import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.activity.MainActivity;
import com.androapplite.shadowsocks.model.ServerConfig;
import com.androapplite.shadowsocks.ShadowsocksApplication;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;
import com.bestgo.adsplugin.ads.AdAppHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.RealResponseBody;
import okio.GzipSource;
import okio.Okio;

/**
 * Created by jim on 16/11/7.
 */

public class ServerListFetcherService extends IntentService{
    private boolean hasStart;
    private static final String DOMAIN_URL = "http://c.vpnnest.com:8080/VPNServerList/fsl";
    private static final String IP_URL = "http://52.21.55.33:8080/VPNServerList/fsl";
    private static final String GITHUB_URL = "https://raw.githubusercontent.com/reachjim/speedvpn/master/fsl.json";

    private static final ArrayList<String> DOMAIN_URLS = new ArrayList<>();
    static {
        DOMAIN_URLS.add(DOMAIN_URL);
        DOMAIN_URLS.add(IP_URL);
    }

    private static final ArrayList<String> STATIC_HOST_URLS = new ArrayList<>();
    static {
        STATIC_HOST_URLS.add(GITHUB_URL);
    }


    private static final HashMap<String, String> URL_KEY_MAP = new HashMap<>();
    static {
        URL_KEY_MAP.put(IP_URL, "ip");
        URL_KEY_MAP.put(DOMAIN_URL, "domain");
        URL_KEY_MAP.put(GITHUB_URL, "github");

    }

    private static final int DELAY_MILLI = 500;
    private static final int TIMEOUT_MILLI = 3000;

    private String mServerListJsonString;
    private OkHttpClient mHttpClient;
    private String mUrl;

    public ServerListFetcherService(){
        super("ServletListFetcher");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent != null && !hasStart){
            hasStart = true;
            SharedPreferences.Editor editor = DefaultSharedPrefeencesUtil.getDefaultSharedPreferencesEditor(this);
            editor.remove(SharedPreferenceKey.SERVER_LIST).apply();
            Cache cache = new Cache(getCacheDir(), 1024 * 1024);
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(TIMEOUT_MILLI, TimeUnit.MILLISECONDS)
                    .readTimeout(TIMEOUT_MILLI, TimeUnit.MILLISECONDS)
                    .writeTimeout(TIMEOUT_MILLI, TimeUnit.MILLISECONDS)
                    .cache(cache)
                    .addInterceptor(new UnzippingInterceptor());
            if(BuildConfig.DEBUG){
                builder.addInterceptor(new LoggingInterceptor());
            }
            mHttpClient = builder.build();

            useCustomURL();
            ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            ExecutorCompletionService<Pair<String, String>> ecs = new ExecutorCompletionService<>(executorService);

            Firebase firebase = Firebase.getInstance(this);
            ArrayList<MyCallable> tasks = new ArrayList<>(DOMAIN_URLS.size());
            for (String url: DOMAIN_URLS) {
                tasks.add(new MyCallable(url, firebase, mHttpClient));
            }

            long t1 = System.currentTimeMillis();
            for (MyCallable callable: tasks) {
                ecs.submit(callable);
            }

            for (int i = 0; i < tasks.size(); i++) {
                try {
                    Future<Pair<String, String>> future = ecs.take();
                    Pair<String, String> result= future.get(3*TIMEOUT_MILLI, TimeUnit.MILLISECONDS);
                    if (result != null) {
                        mUrl = result.first;
                        mServerListJsonString = result.second;
                        break;
                    }
                } catch (Exception e) {
                    ShadowsocksApplication.handleException(e);
                }
            }
            Log.d("FetchSeverList", "动态列表总时间：" + (System.currentTimeMillis() - t1));

            //获取远程静态服务器列表
            if(mServerListJsonString == null){
                tasks = new ArrayList<>(STATIC_HOST_URLS.size());
                for (String url: STATIC_HOST_URLS) {
                    tasks.add(new MyCallable(url, firebase, mHttpClient));
                }

                t1 = System.currentTimeMillis();
                for (MyCallable callable: tasks) {
                    ecs.submit(callable);
                }

                for (int i = 0; i < tasks.size(); i++) {
                    try {
                        Future<Pair<String, String>> future = ecs.take();
                        Pair<String, String> result= future.get(3*TIMEOUT_MILLI, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            mUrl = result.first;
                            mServerListJsonString = result.second;
                            break;
                        }
                    } catch (Exception e) {
                        ShadowsocksApplication.handleException(e);
                    }
                }
                Log.d("FetchSeverList", "静态列表总时间：" + (System.currentTimeMillis() - t1));
            }

            long t2 = System.currentTimeMillis();
            String urlKey = URL_KEY_MAP.get(mUrl);
            if(urlKey == null){
                urlKey = "没有匹配的url";
            }
            //取结果
            if(mServerListJsonString != null){
                Firebase.getInstance(this).logEvent("取服务器列表成功总时间", urlKey, t2-t1);
            }else{
                Firebase.getInstance(this).logEvent("取服务器列表失败总时间", t2-t1);
            }

            //使用remote config
            if(mServerListJsonString == null){
                mServerListJsonString = ServerConfig.shuffleRemoteConfig();
                if(mServerListJsonString != null) {
                    urlKey = "remote_config";
                }
            }

            //使用本地静态服务器列表
            if(mServerListJsonString == null){
                AssetManager assetManager = getAssets();
                try {
                    InputStream inputStream = assetManager.open("fsl.json");
                    InputStreamReader isr = new InputStreamReader(inputStream);
                    BufferedReader br = new BufferedReader(isr);
                    mServerListJsonString = br.readLine();
                    if(mServerListJsonString != null){
                        mServerListJsonString = ServerConfig.shuffleStaticServerListJson(mServerListJsonString);
                        urlKey = "local_config";
                    }

                } catch (IOException e) {
                    ShadowsocksApplication.handleException(e);
                }
            }

            if(mServerListJsonString != null) {
                editor.putString(SharedPreferenceKey.SERVER_LIST, mServerListJsonString).apply();
            }else{
                urlKey = "没有任何可用的服务器列表";
            }

            String localCountry = getResources().getConfiguration().locale.getDisplayCountry();
            TelephonyManager manager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
            String simOperator = manager.getSimOperator();
            String iosCountry = manager.getSimCountryIso();
            Firebase.getInstance(this).logEvent("服务器url", urlKey, String.format("%s|%s|%s", iosCountry, simOperator, localCountry));
            if(mUrl != null){
                AdAppHelper adAppHelper = AdAppHelper.getInstance(this);
                String domain = adAppHelper.getCustomCtrlValue("serverListDomain", null);
                String ip = adAppHelper.getCustomCtrlValue("serverListIP", null);
                if(mUrl.equals(domain)){
                    Firebase.getInstance(this).logEvent("服务器列表地址", "domain", mUrl);
                }else if(mUrl.equals(ip)){
                    Firebase.getInstance(this).logEvent("服务器列表地址", "ip", mUrl);
                }
            }

            broadcastServerListFetchFinish();
            hasStart = false;
        }

    }

    private void useCustomURL(){
        AdAppHelper adAppHelper = AdAppHelper.getInstance(this);
        String url = adAppHelper.getCustomCtrlValue("serverListDomain", DOMAIN_URL);
        DOMAIN_URLS.set(0, url);
        URL_KEY_MAP.put(url, "domain");
        url = adAppHelper.getCustomCtrlValue("serverListIP", IP_URL);
        DOMAIN_URLS.set(1, url);
        URL_KEY_MAP.put(url, "ip");
    }

    private static class MyCallable implements Callable<Pair<String, String>> {
        private String mUrl;
        private Firebase mFirebase;
        private OkHttpClient mHttpClient;

        MyCallable(String url, Firebase firebase, OkHttpClient client) {
            mUrl = url;
            mFirebase = firebase;
            mHttpClient = client;
        }
        @Override
        public Pair<String, String> call() throws Exception {
            Request request = new Request.Builder()
                    .url(mUrl)
                    .addHeader("Accept-Encoding", "gzip")
                    .build();
            long t1 = System.currentTimeMillis();
            String urlKey = URL_KEY_MAP.get(mUrl);
            if(urlKey == null){
                urlKey = "没有匹配的url";
            }
            String jsonString = null;
            String errMsg = null;
            try {
                Response response = mHttpClient.newCall(request).execute();
                if(response.isSuccessful())
                {
                    jsonString = response.body().string();
                }else{
                    errMsg = response.message() + " " + response.code();
                }
            }catch (IOException e) {
                errMsg = e.getMessage();
                if(errMsg == null){
                    errMsg = e.toString();
                }
            }
            long dur = System.currentTimeMillis() - t1;
            if(jsonString != null && !jsonString.isEmpty() && ServerConfig.checkServerConfigJsonString(jsonString)) {
                mFirebase.logEvent("访问服务器列表成功", urlKey, dur);
            }else{
                mFirebase.logEvent("访问服务器列表失败", urlKey, dur);
                if(errMsg == null) errMsg = "服务器列表JSON问题";
                mFirebase.logEvent("访问服务器列表失败", urlKey, errMsg);
            }
            return new Pair<>(mUrl, jsonString);
        }
    }

    public static void fetchServerListAsync(Context context){
        Intent intent = new Intent(context, ServerListFetcherService.class);
        context.startService(intent);
    }

    private void broadcastServerListFetchFinish(){
        final Intent intent = new Intent(Action.SERVER_LIST_FETCH_FINISH);
        if(mServerListJsonString != null){
            //借用一下
            intent.putExtra(SharedPreferenceKey.SERVER_LIST, mServerListJsonString);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    static class LoggingInterceptor implements Interceptor {
        @Override public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            long t1 = System.nanoTime();
            Log.d("OkHttp", String.format("Sending request %s on %s%n%s",
                    request.url(), chain.connection(), request.headers()));

            Response response = chain.proceed(request);

            long t2 = System.nanoTime();
            Log.d("OkHttp", String.format("Received response for %s in %.1fms%n%s",
                    response.request().url(), (t2 - t1) / 1e6d, response.headers()));

            return response;
        }
    }

    static class UnzippingInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Response response = chain.proceed(chain.request());
            return unzip(response);
        }

        private Response unzip(final Response response) throws IOException {

            if (response.body() == null || !"gzip".equals(response.header("Content-Encoding"))) {
                return response;
            }

            GzipSource responseBody = new GzipSource(response.body().source());
            Headers strippedHeaders = response.headers().newBuilder()
                    .removeAll("Content-Encoding")
                    .removeAll("Content-Length")
                    .build();
            return response.newBuilder()
                    .headers(strippedHeaders)
                    .body(new RealResponseBody(strippedHeaders, Okio.buffer(responseBody)))
                    .build();
        }
    }
}