package com.practice.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.TtsMode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class FirstTryActivity extends AppCompatActivity implements EventListener {

    final String TAG = "baidutest";
    EventManager mEventManager;
    private String mJson;
    String result;
    private static final String TEMP_DIR = "/sdcard/baiduTTS";
    private static final String TEXT_FILENAME = TEMP_DIR + "/" + "bd_etts_text.dat";
    private static final String MODEL_FILENAME =
            TEMP_DIR + "/" + "bd_etts_common_speech_m15_mand_eng_high_am-mix_v3.0.0_20170505.dat";
    protected SpeechSynthesizer mSpeechSynthesizer;

    protected String appId = "11028185";
    protected String appKey = "6IoQLXu7VnMUgfeGWyf7NjBR";
    protected String secretKey = "0qyfeFZ5395alzfT31Og5rnBC7e8S47p";

    private Robot mRobot = null;
    private Handler mHandler;
    LinearLayout mLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_try);
        getSupportActionBar().hide();
        initPermission();
        mLinearLayout = (LinearLayout) findViewById(R.id.contantLL);
        mEventManager = EventManagerFactory.create(this, "asr");
        mEventManager.registerListener(this);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
        mJson = new JSONObject(params).toString();
        new Thread(new Runnable() {
            @Override
            public void run() {
                initTTs();
            }
        }).start();
        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                String s = (String) msg.obj;
                mSpeechSynthesizer.speak(s);
                TextView tv = new TextView(FirstTryActivity.this);
                tv.setGravity(Gravity.LEFT);
                tv.setText(s);
                mLinearLayout.addView(tv);
                Log.d(TAG, "handleMessage: " + s);
            }
        };
        mRobot = new Robot(mHandler);
    }

    private void initTTs() {
        fileCopy("bd_etts_text.dat", TEXT_FILENAME);
        fileCopy("bd_etts_common_speech_m15_mand_eng_high_am-mix_v3.0.0_20170505.dat"
                , MODEL_FILENAME);
        mSpeechSynthesizer = SpeechSynthesizer.getInstance();
        mSpeechSynthesizer.setContext(FirstTryActivity.this);
        mSpeechSynthesizer.setAppId(appId);
        mSpeechSynthesizer.setApiKey(appKey, secretKey);
        mSpeechSynthesizer.auth(TtsMode.MIX);
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, TEXT_FILENAME);
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, MODEL_FILENAME);
        // 设置在线发声音人： 0 普通女声（默认） 1 普通男声 2 特别男声 3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "3");
        // 设置合成的音量，0-9 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "9");
        // 设置合成的语速，0-9 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5");
        // 设置合成的语调，0-9 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5");
        // 该参数设置为TtsMode.MIX生效。即纯在线模式不生效。
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);
        mSpeechSynthesizer.setAudioStreamType(AudioManager.MODE_IN_CALL);
        mSpeechSynthesizer.initTts(TtsMode.MIX);
    }

    private void fileCopy(String file, String copy) {
        AssetManager am = getAssets();
        File copyFile = new File(copy);
        try {
            InputStream is = am.open(file);
            FileOutputStream fos = new FileOutputStream(copyFile);
            byte[] bytes = new byte[1024 * 1024];
            int length = 0;
            while ((length = is.read(bytes)) > 0) {
                fos.write(bytes, 0, length);
                fos.flush();
            }
            is.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void click(View view) {
        mEventManager.send(SpeechConstant.ASR_START, mJson, null, 0, 0);
    }

    @Override
    public void onEvent(String s, String s1, byte[] bytes, int i, int i1) {
        if (s.equals("asr.finish")) {

            mRobot.ask(result);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView tv = new TextView(FirstTryActivity.this);
                    tv.setGravity(Gravity.RIGHT);
                    tv.setText(result);
                    mLinearLayout.addView(tv);
                }
            });
        }
        try {
            if (s1 == null) return;
            JSONObject jsonObject = new JSONObject(s1);
            result = jsonObject.get("best_result").toString();

//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    bt.setText(result);
//
//                    mTextView.setText(mTextView.getText() + "/n" + result);
//                    mTextView.setGravity(Gravity.LEFT);
//                }
//            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String permissions[] = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.

            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }

    }
}
