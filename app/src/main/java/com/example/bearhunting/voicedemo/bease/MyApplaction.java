package com.example.bearhunting.voicedemo.bease;

import android.app.Application;

import com.example.bearhunting.voicedemo.R;
import com.iflytek.cloud.Setting;
import com.iflytek.cloud.SpeechUtility;

/**
 * Created by bearhunting on 2018/02/01.
 */

public class MyApplaction extends Application {


    @Override
    public void onCreate() {
        SpeechUtility.createUtility(this, "appid=" + getString(R.string.app_id));

        // 以下语句用于设置日志开关（默认开启），设置成false时关闭语音云SDK日志打印
        Setting.setShowLog(false);
        super.onCreate();
    }




}






