package com.androapplite.shadowsocks.fragment;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.androapplite.shadowsocks.R;
import com.androapplite.shadowsocks.preference.DefaultSharedPrefeencesUtil;
import com.androapplite.shadowsocks.preference.SharedPreferenceKey;

import java.util.Timer;
import java.util.TimerTask;

import yyf.shadowsocks.utils.Constants;

/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectFragment extends Fragment implements View.OnClickListener{
    private OnConnectActionListener mListener;
    private TextView mMessageTextView;
    private Button mConnectButton;
    private ImageView mLoadingView;
    private Timer mCountDownTimer;


    public ConnectFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connect, container, false);
        mMessageTextView = (TextView)view.findViewById(R.id.message);
        mConnectButton = (Button)view.findViewById(R.id.connect_button);
        mConnectButton.setOnClickListener(this);
        mLoadingView = (ImageView)view.findViewById(R.id.loading);
        return view;
    }

    @Override
    public void onClick(View v) {
        if(mListener != null){
            mListener.onConnectButtonClick();
        }
    }

    public interface OnConnectActionListener{
        void onConnectButtonClick();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof OnConnectActionListener){
            mListener = (OnConnectActionListener) context;
        }else{
            throw new ClassCastException(context.getClass().getSimpleName() + " must implement " + OnConnectActionListener.class.getSimpleName());
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(activity instanceof OnConnectActionListener){
            mListener = (OnConnectActionListener) activity;
        }else{
            throw new ClassCastException(activity.getClass().getSimpleName() + " must implement " + OnConnectActionListener.class.getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoadingView.clearAnimation();
        if(mCountDownTimer != null){
            mCountDownTimer.cancel();
            mCountDownTimer.purge();
        }

    }

    public void animateConnecting(){
        startAnimation();
        mConnectButton.setText(R.string.disconnect);
        mMessageTextView.setText(R.string.connecting);
        mLoadingView.setColorFilter(getResources().getColor(R.color.animation_color));
    }

    private void startAnimation(){
        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate);
        mLoadingView.startAnimation(animation);
    }


    public void animateStopping(){
        startAnimation();
        mConnectButton.setText(R.string.connect);
        mMessageTextView.setText(R.string.stopping);
    }

    public void setConnectResult(final Constants.State state){
        switch (state){
            case INIT:
                break;
            case CONNECTING:
                animateConnecting();
                break;
            case CONNECTED:
                connectFinish();
                break;
            case STOPPING:
                animateStopping();
                break;
            case STOPPED:
                stopFinish();
                break;
            case ERROR:
                error();
                break;
        }

    }

    private void connectFinish(){
        mLoadingView.clearAnimation();
        mLoadingView.setColorFilter(getResources().getColor(R.color.connect_color));
        mCountDownTimer = new Timer();
        mCountDownTimer.schedule(new CountDownTimerTask(), 0, 1000);
    }

    private class CountDownTimerTask extends TimerTask{
        @Override
        public void run() {
            SharedPreferences sharedPreferences = DefaultSharedPrefeencesUtil.getDefaultSharedPreferences(getContext());
            final int countDown = sharedPreferences.getInt(SharedPreferenceKey.TIME_COUNT_DOWN, 0);
            mMessageTextView.post(new Runnable() {
                @Override
                public void run() {
                    mMessageTextView.setText(DateUtils.formatElapsedTime(countDown));

                }
            });
        }
    }

    private void stopFinish(){
        mLoadingView.clearAnimation();
        mLoadingView.setColorFilter(getResources().getColor(R.color.connect_color));
    }

    private void error(){
        mLoadingView.clearAnimation();
        mLoadingView.setColorFilter(getResources().getColor(R.color.error_color));

    }

    public void addProgress(int millisecond){
//        if(mProgressBar != null) {
//            mProgressBar.setProgress(mProgressBar.getProgress() + millisecond);
//        }
    }
}
