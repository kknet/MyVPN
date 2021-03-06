package com.androapplite.shadowsocks.view;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androapplite.shadowsocks.Firebase;
import com.androapplite.shadowsocks.R;


public class VPN3AdDialog extends DialogFragment {
    private final String VPN3_URL = "https://play.google.com/store/apps/details?id=com.androapplite.vpn10";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Translucent_NoTitleBar);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK)
                    return true;
                return false;
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.vpn3_ad_dialog, container, false);

        view.findViewById(R.id.vpn3_ad_imgv).setOnClickListener(mOnClickListener);
        view.findViewById(R.id.vpn3_ad_close).setOnClickListener(mOnClickListener);
        Firebase.getInstance(getContext()).logEvent("VPN3推荐弹窗","显示","成功");
        return view;
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.vpn3_ad_imgv:
                    openGooglePlay(getContext(), VPN3_URL);
                    Firebase.getInstance(getContext()).logEvent("VPN3推荐弹窗","显示","点击跳转");
                    break;
                case R.id.vpn3_ad_close:
                    dismissAllowingStateLoss();
                    Firebase.getInstance(getContext()).logEvent("VPN3推荐弹窗","显示","点击关闭");
                    break;
            }
        }
    };

    private void openGooglePlay(Context context, String url) {
        try {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//            intent.setPackage("com.android.vending");
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOnClickListener != null)
            mOnClickListener = null;
    }
}
