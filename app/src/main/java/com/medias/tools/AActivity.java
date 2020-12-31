package com.medias.tools;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * ******************(^_^)***********************<br>
 * User: fee(QQ/WeiXin:1176610771)<br>
 * Date: 2020/4/17<br>
 * Time: 19:34<br>
 * <P>DESC:
 * </p>
 * ******************(^_^)***********************
 */
public class AActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Button textView = new Button(this);
        textView.setText("我是A界面");



        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AActivity.this, BActivity.class));
            }
        });
        LinearLayout llView = new LinearLayout(this);
        ViewGroup.LayoutParams vlp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        llView.addView(textView);
        setContentView(llView,vlp);
        turnTheOrientation();
    }

    private void turnTheOrientation() {
        int curReqOrientation = getRequestedOrientation();
        Log.e("AActivity", "--> turnTheOrientation() curReqOrientation = " + getcreenOriDesc(curReqOrientation));

        if (curReqOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {//!= 1
            Log.e("AActivity", "--> turnTheOrientation() 我要转为 竖屏");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private String getcreenOriDesc(int theOri) {
        switch (theOri) {
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                return "竖屏";
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                return "横屏";

            case ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED:
                return "未指定";
            case ActivityInfo.SCREEN_ORIENTATION_LOCKED:
                return "locked";

            case ActivityInfo.SCREEN_ORIENTATION_USER:
                return "user";
        }
        return "未知";
    }
}
