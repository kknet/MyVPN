package com.androapplite.shadowsocks.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;

import com.androapplite.shadowsocks.AdHelper;
import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.ShadowsockServiceHelper;
import com.androapplite.shadowsocks.broadcast.Action;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;

import java.util.concurrent.TimeUnit;

public class SplashActivity extends BaseShadowsocksActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initBackgroundReceiver();
        initBackgroundReceiverIntentFilter();

        startNewUserGuideActivityOrConnectionActivity();
        checkAndCopyAsset();
        ShadowsockServiceHelper.startService(this);

        loadAd();
    }

    private void startNewUserGuideActivityOrConnectionActivity() {
        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                Activity activity = SplashActivity.this;
                startActivity(new Intent(activity,
                        DefaultSharedPrefeencesUtil.isNewUser(activity) ? NewUserGuideActivity.class : ConnectionActivity.class
                ));
            }
        }, TimeUnit.SECONDS.toMillis(2));
    }

    private void checkAndCopyAsset() {
        new Thread() {
            @Override
            public void run() {
                ShadowsockServiceHelper.checkAndCopyAsset(getAssets(), yyf.shadowsocks.jni.System.getABI());
            }
        }.start();
    }

    private void initBackgroundReceiver(){
        mBackgroundReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if(action.equals(Action.CONNECTION_ACTIVITY_SHOW) || action.equals(Action.NEW_USER_GUIDE_ACTIVITY_SHOW)){
                    finish();
                }
            }
        };
    }

    private void initBackgroundReceiverIntentFilter(){
        mBackgroundReceiverIntentFilter = new IntentFilter();
        mBackgroundReceiverIntentFilter.addAction(Action.CONNECTION_ACTIVITY_SHOW);
        mBackgroundReceiverIntentFilter.addAction(Action.NEW_USER_GUIDE_ACTIVITY_SHOW);
    }

    private void loadAd(){
        final AdHelper adHelper = AdHelper.getInstance(this);
        adHelper.setFacebookAdListener(new AdHelper.OnAdLoadListener() {
            @Override
            public void onAdLoaded(Object ad) {
            }

            @Override
            public void onAdClicked(Object ad) {

            }

            @Override
            public void onError(Object ad) {
                adHelper.loadAdmobAd();
            }
        });
        adHelper.loadFacebookAd();
//        adHelper.loadAdmobAd();
    }

}
