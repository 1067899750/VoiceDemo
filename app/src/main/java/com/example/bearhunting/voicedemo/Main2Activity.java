package com.example.bearhunting.voicedemo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeakerVerifier;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechListener;
import com.iflytek.cloud.VerifierListener;
import com.iflytek.cloud.VerifierResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Main2Activity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = Main2Activity.class.getSimpleName();

    private static final int PWD_TYPE_TEXT = 1;
    //自由说密码。用户通过读一段文字来进行声纹注册和验证，注册时要求声音长度为20秒左右，
    // 验证时要求声音长度为15秒左右，内容不限。
    private static final int PWD_TYPE_FREE = 2;
    private static final int PWD_TYPE_NUM = 3;
    // 当前声纹密码类型，1、2、3分别为文本、自由说和数字密码
    private int mPwdType = PWD_TYPE_TEXT;

    // 数字声纹密码
    private String mNumPwd = "";
    // 数字声纹密码段，默认有5段
    private String[] mNumPwdSegs;
    // 用于验证的数字密码
    private String mVerifyNumPwd = "";
    // 文本声纹密码
    private String mTextPwd = "";

    // 会话类型
    private int mSST = 0;
    // 注册
    private static final int SST_ENROLL = 0;
    // 验证
    private static final int SST_VERIFY = 1;

    // 是否可以录音
    private boolean isStartWork = false;

    // 用户id，唯一标识
    private String mAuthId;

    private TextView mResultText;
    private Button mBtnStartRecord;
    private EditText mAuthidEditText;
    private RadioGroup mPwdTypeGroup;
    private TextView mErrorResult;

    private Toast mToast;
    private ProgressDialog mProDialog;
    private AlertDialog mTextPwdSelectDialog;
    private SpeakerVerifier mSpeakerVerifier;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        setUI();
        // 初始化SpeakerVerifier，InitListener为初始化完成后的回调接口
        mSpeakerVerifier = SpeakerVerifier.createVerifier(this, new InitListener() {
            @Override
            public void onInit(int i) {
                if (ErrorCode.SUCCESS == i) {
                    showTip("引擎初始化成功");
                } else {
                    showTip("引擎初始化失败，错误码：" + i);
                }
            }
        });
    }


    private void setUI() {
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        mToast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);

        mResultText = (TextView) findViewById(R.id.edt_result);
        mErrorResult = (TextView) findViewById(R.id.error_result);
        mAuthidEditText = (EditText) findViewById(R.id.set_authId);
        mBtnStartRecord = (Button) findViewById(R.id.isv_reocrd);
        mBtnStartRecord.setOnClickListener(this);

        findViewById(R.id.isv_getpassword).setOnClickListener(this);
        findViewById(R.id.isv_search).setOnClickListener(this);
        findViewById(R.id.isv_delete).setOnClickListener(this);
        findViewById(R.id.isv_identity).setOnClickListener(this);

        mProDialog = new ProgressDialog(this);
        mProDialog.setCancelable(true);
        mProDialog.setTitle("请稍候");
        // cancel进度框时，取消正在进行的操作
        mProDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (null != mSpeakerVerifier) {
                    mSpeakerVerifier.cancel();
                }
            }
        });

        mPwdTypeGroup = (RadioGroup) findViewById(R.id.radioGroup);
        mPwdTypeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radioText:
                        mTextPwd = null;
                        isStartWork = false;
                        mPwdType = PWD_TYPE_TEXT;
                        break;

                    case R.id.radioFree:
                        isStartWork = false;
                        mPwdType = PWD_TYPE_FREE;
                        break;

                    case R.id.radioNumber:
                        mNumPwdSegs = null;
                        isStartWork = false;
                        mPwdType = PWD_TYPE_NUM;
                        break;
                    default:
                        break;
                }
            }
        });

    }


    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }



    @Override
    public void onClick(View v) {
        if (!checkInstance()) {
            return;
        }

        mAuthId = getAuthid();
        if (TextUtils.isEmpty(mAuthId)) {
            showTip("用户id为空，请重新输入");
            return;
        }
        // 取消先前操作
        cancelOperation();
        switch (v.getId()) {
            case R.id.isv_getpassword:
                // 获取密码之前先终止之前的注册或验证过程
                // 首次注册密码为空时，调用下载密码
                if (mTextPwd == null || mNumPwdSegs == null && mPwdType != PWD_TYPE_FREE) {
                    downloadPwd();
                }
                break;

            case R.id.isv_reocrd:
                mAuthId = getAuthid();
                if (TextUtils.isEmpty(mAuthId)) {
                    showTip("请输入authid");
                    break;
                }
                vocalEnroll();
                break;

            case R.id.isv_search:
                //查询模型
                performModelOperation("que", mModelOperationListener);
                break;

            case R.id.isv_delete:
                //删除模型
                performModelOperation("del", mModelOperationListener);
                break;

            case R.id.isv_identity:
                //验证
                // 清空参数
                mSpeakerVerifier.setParameter(SpeechConstant.PARAMS, null);
                mSpeakerVerifier.setParameter(SpeechConstant.ISV_AUDIO_PATH,
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/msc/verify.pcm");
                mSpeakerVerifier = SpeakerVerifier.getVerifier();
                // 设置业务类型为验证
                mSpeakerVerifier.setParameter(SpeechConstant.ISV_SST, "verify");
                // 对于某些麦克风非常灵敏的机器，如nexus、samsung i9300等，建议加上以下设置对录音进行消噪处理
//       mVerify.setParameter(SpeechConstant.AUDIO_SOURCE, "" + MediaRecorder.AudioSource.VOICE_RECOGNITION);

                if (mPwdType == PWD_TYPE_TEXT) {
                    // 文本密码注册需要传入密码
                    if (TextUtils.isEmpty(mTextPwd)) {
                        showTip("请获取密码后进行操作");
                        return;
                    }
                    mSpeakerVerifier.setParameter(SpeechConstant.ISV_PWD, mTextPwd);
                    mResultText.setText("请读出：" + mTextPwd);

                } else if (mPwdType == PWD_TYPE_NUM) {
                    // 数字密码注册需要传入密码
                    String verifyPwd = mSpeakerVerifier.generatePassword(8);
                    mSpeakerVerifier.setParameter(SpeechConstant.ISV_PWD, verifyPwd);
                    mResultText.setText("请读出：" + verifyPwd);

                } else if (mPwdType == PWD_TYPE_FREE) {
                    mSpeakerVerifier.setParameter(SpeechConstant.SAMPLE_RATE, "8000");
                    mResultText.setText("请随便说些用于验证");

                }
                // 设置auth_id，不能设置为空
                mSpeakerVerifier.setParameter(SpeechConstant.AUTH_ID, mAuthId);
                mSpeakerVerifier.setParameter(SpeechConstant.ISV_PWDT, "" + mPwdType);
                // 开始验证
                mSpeakerVerifier.startListening(mVerifyListener);
                break;
            default:
                break;
        }

    }

    /**
     * 执行模型操作
     *
     * @param operation 操作命令
     * @param listener  操作结果回调对象
     */
    private void performModelOperation(String operation, SpeechListener listener) {
        // 清空参数
        mSpeakerVerifier.setParameter(SpeechConstant.PARAMS, null);


        //设置密码类型(就是咱要读的)
        mSpeakerVerifier.setParameter(SpeechConstant.ISV_PWDT, "" + mPwdType);

        if (mPwdType == PWD_TYPE_TEXT) {
            // 文本密码删除需要传入密码
            if (TextUtils.isEmpty(mTextPwd)) {
                showTip("请获取密码后进行操作");
                return;
            }

            // 对于文本密码和数字密码，必须设置密码的文本内容，pwdText的取值为“芝麻开门”或者是从云端拉取的数字密码(每8位用“-”隔开，如“62389704-45937680-32758406-29530846-58206497”)。自由说略过此步

            mSpeakerVerifier.setParameter(SpeechConstant.ISV_PWD, mTextPwd);
        } else if (mPwdType == PWD_TYPE_NUM) {

        } else if (mPwdType == PWD_TYPE_FREE) {

        }
        // 设置auth_id，不能设置为空
        mSpeakerVerifier.sendRequest(operation, mAuthId, listener);
    }

    /**
     * 下载密码
     */
    private void downloadPwd() {
        mAuthId = getAuthid();
        if (TextUtils.isEmpty(mAuthId)) {
            showTip("请输入authid");
            return;
        }
        // 获取密码之前先终止之前的操作
        mSpeakerVerifier.cancel();
        // 下载密码时，按住说话触摸无效
        mBtnStartRecord.setClickable(false);

        mProDialog.setMessage("下载中...");
        mProDialog.show();
        // 清空参数
        mSpeakerVerifier.setParameter(SpeechConstant.PARAMS, null);
        // 设置会话场景
        mSpeakerVerifier.setParameter(SpeechConstant.MFV_SCENES, "ivp");
        // 当前声纹密码类型，1、2、3分别为文本、自由说和数字密码
        mSpeakerVerifier.setParameter(SpeechConstant.ISV_PWDT, "" + mPwdType);
        if (mPwdType != PWD_TYPE_FREE) {
            //本地的监听参数
            mSpeakerVerifier.getPasswordList(mPwdListenter);
        }

    }

    /**
     * 注册
     */
    private void vocalEnroll() {
        mSpeakerVerifier.setParameter(SpeechConstant.PARAMS, null);
        mSpeakerVerifier.setParameter(SpeechConstant.ASR_AUDIO_PATH,
                Environment.getExternalStorageDirectory().getAbsolutePath() + "/msc/test.pcm");
        // 对于某些麦克风非常灵敏的机器，如nexus、samsung i9300等，建议加上以下设置对录音进行消噪处理
        // mSpeakerVerifier.setParameter(SpeechConstant.AUDIO_SOURCE, "" + MediaRecorder.AudioSource.VOICE_RECOGNITION);
        if (mPwdType == PWD_TYPE_TEXT){
            if (TextUtils.isEmpty(mTextPwd)){
                showTip("请获取密码后进行操作");
                return;
            }
            StringBuffer strBuffer = new StringBuffer();
            strBuffer.append("请长按“按住说话”按钮！\n");
            strBuffer.append("请读出：" + mTextPwd + "\n");
            strBuffer.append("训练 第" + 1 + "遍，剩余4遍\n");
            mResultText.setText(strBuffer.toString());
            mSpeakerVerifier.setParameter(SpeechConstant.ISV_PWD, mTextPwd);
        } else if (mPwdType == PWD_TYPE_NUM){
            // 数字密码注册需要传入密码
            if (TextUtils.isEmpty(mNumPwd)) {
                showTip("请获取密码后进行操作");
                return;
            }
            StringBuffer strBuffer = new StringBuffer();
            strBuffer.append("请长按“按住说话”按钮！\n");
            strBuffer.append("请读出：" + mNumPwdSegs[0] + "\n");
            strBuffer.append("训练 第" + 1 + "遍，剩余4遍\n");
            mResultText.setText(strBuffer.toString());
            mSpeakerVerifier.setParameter(SpeechConstant.ISV_PWD, mNumPwd);

        } else if (mPwdType == PWD_TYPE_FREE){
            //这里插一句嘴，自由说的注册参数之次数 设置为“1” 音质的的设置“8000
            mSpeakerVerifier.setParameter(SpeechConstant.ISV_RGN, "1");
            mSpeakerVerifier.setParameter(SpeechConstant.SAMPLE_RATE, "8000");

        }

        mSpeakerVerifier.setParameter(SpeechConstant.AUTH_ID, mAuthId);
        // 设置业务类型为注册
        mSpeakerVerifier.setParameter(SpeechConstant.ISV_SST, "train");
        // 设置声纹密码类型
        mSpeakerVerifier.setParameter(SpeechConstant.ISV_PWDT, "" + mPwdType);
        // 开始注册
        mSpeakerVerifier.startListening(mRegisterListener);
    }


    //    第一步的监听参数：
    //    通过解析获得密码，注意 这里自由说不需要密码，所以这里没有它的case
    private String[] items;
    private SpeechListener mPwdListenter = new SpeechListener() {
        @Override
        public void onEvent(int i, Bundle bundle) {

        }

        @Override
        public void onBufferReceived(byte[] bytes) {
            mAuthidEditText.setEnabled(false);//密码获取成功后，authid不可再变动
            mProDialog.dismiss();
            mBtnStartRecord.setClickable(true);

            String result = new String(bytes);
            switch (mPwdType) {
                case PWD_TYPE_TEXT:
                    try {
                        JSONObject object = new JSONObject(result);
                        if (!object.has("txt_pwd")) {
                            mResultText.setText("");
                            return;
                        }

                        JSONArray pwdArray = object.optJSONArray("txt_pwd");
                        items = new String[pwdArray.length()];
                        for (int i = 0; i < pwdArray.length(); i++) {
                            items[i] = pwdArray.getString(i);
                        }
                        mTextPwdSelectDialog = new AlertDialog.Builder(Main2Activity.this)
                                .setTitle("请选择密码文本")
                                .setCancelable(false)
                                .setItems(items, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mTextPwd = items[which];
                                        mResultText.setText("您的注册密码：\n" + mTextPwd +
                                                "\n请长按“按住说话”按钮进行注册\n");
                                    }
                                }).create();
                        mTextPwdSelectDialog.show();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case PWD_TYPE_NUM:
                    StringBuffer numberString = new StringBuffer();
                    try {
                        JSONObject object = new JSONObject(result);
                        if (!object.has("num_pwd")) {
                            mResultText.setText("");
                            return;
                        }
                        JSONArray pwdArray = object.optJSONArray("num_pwd");
                        numberString.append(pwdArray.get(0));
                        for (int i = 1; i < pwdArray.length(); i++) {
                            numberString.append("-" + pwdArray.get(i));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    mNumPwd = numberString.toString();
                    mNumPwdSegs = mNumPwd.split("-");
                    mResultText.setText("您的注册密码：\n" + mNumPwd +
                            "\n请长按“按住说话”按钮进行注册\n");
                    break;
                default:
                    break;
            }

        }

        @Override
        public void onCompleted(SpeechError speechError) {
            if (null != speechError && ErrorCode.SUCCESS != speechError.getErrorCode()) {
                showTip("获取失败：" + speechError.getErrorCode());
            }
        }
    };

    //第二步的监听参数，通过调用result的参数获取结果
    private VerifierListener mRegisterListener = new VerifierListener() {
        @Override
        public void onResult(VerifierResult verifierResult) {
          if (verifierResult.ret == ErrorCode.SUCCESS){
              switch (verifierResult.err){
                  case VerifierResult.MSS_ERROR_IVP_GENERAL:
                      mErrorResult.setText("内核异常");
                      break;
                  case VerifierResult.MSS_ERROR_IVP_EXTRA_RGN_SOPPORT:
                      mErrorResult.setText("训练达到最大次数");
                      break;
                  case VerifierResult.MSS_ERROR_IVP_TRUNCATED:
                      mErrorResult.setText("出现截幅");
                      break;
                  case VerifierResult.MSS_ERROR_IVP_MUCH_NOISE:
                      mErrorResult.setText("太多噪音");
                      break;
                  case VerifierResult.MSS_ERROR_IVP_UTTER_TOO_SHORT:
                      mErrorResult.setText("录音太短");
                      break;
                  case VerifierResult.MSS_ERROR_IVP_TEXT_NOT_MATCH:
                      mErrorResult.setText("训练失败，您所读的文本不一致");
                      break;
                  case VerifierResult.MSS_ERROR_IVP_TOO_LOW:
                      mErrorResult.setText("音量太低");
                      break;
                  case VerifierResult.MSS_ERROR_IVP_NO_ENOUGH_AUDIO:
                      mErrorResult.setText("音频长达不到自由说的要求");
                      break;
                  default:
                      showTip("");
                      break;
              }

              if (verifierResult.suc == verifierResult.rgn){
                  mResultText.setText("注册成功");
                  if (PWD_TYPE_TEXT == mPwdType) {
                      mErrorResult.setText("您的文本密码声纹ID：\n" + verifierResult.vid);
                      Log.e(TAG, "onResult:文字 " + verifierResult.vid);

                  } else if (PWD_TYPE_NUM == mPwdType) {
                      mErrorResult.setText("您的数字密码声纹ID：\n" + verifierResult.vid);
                      Log.e(TAG, "onResult:数字 " + verifierResult.vid);

                  } else if (mPwdType == PWD_TYPE_FREE) {
                      mErrorResult.setText("您的数字密码声纹ID：\n" + verifierResult.vid);
                      Log.e(TAG, "onResult: 自由" + verifierResult.vid);

                  }
                  isStartWork = false;
                  if (mSpeakerVerifier != null) {
                      mSpeakerVerifier.stopListening();
                  }

              } else {
                  int nowTimes = verifierResult.suc + 1;
                  int leftTimes = verifierResult.rgn - nowTimes;

                  StringBuffer strBuffer = new StringBuffer();
                  strBuffer.append("请长按“按住说话”按钮！\n");
                  if (PWD_TYPE_TEXT == mPwdType) {
                      strBuffer.append("请读出：" + mTextPwd + "\n");
                  } else if (PWD_TYPE_NUM == mPwdType) {
                      strBuffer.append("请读出：" + mNumPwdSegs[nowTimes - 1] + "\n");
                  }
                  strBuffer.append("训练 第" + nowTimes + "遍，剩余" + leftTimes + "遍");
                  mResultText.setText(strBuffer.toString());
              }

          } else {
              mResultText.setText("注册失败，请重新开始。");

          }
        }

        @Override
        public void onVolumeChanged(int i, byte[] bytes) {
            showTip("当前正在说话，音量大小：" + i);
            Log.d(TAG, "返回音频数据：" + bytes.length);
        }

        @Override
        public void onBeginOfSpeech() {
            showTip("开始说话");
        }

        @Override
        public void onEndOfSpeech() {
            showTip("结束说话");
        }

        @Override
        public void onError(SpeechError speechError) {
            if (speechError.getErrorCode() == ErrorCode.MSP_ERROR_ALREADY_EXIST) {
                showTip("模型已存在，如需重新注册，请先删除");
            } else {
                showTip("onError Code：" + speechError.getPlainDescription(true));
            }
        }

        // 保留方法，暂不用
        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
             if (SpeechEvent.EVENT_SESSION_ID == i) {
                String sid = bundle.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
                Log.d(TAG, "session id =" + sid);
             }
        }
    };

    //第三步 监听参数
    private VerifierListener mVerifyListener = new VerifierListener() {

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("当前正在说话，音量大小：" + volume);
            Log.d(TAG, "返回音频数据：" + data.length);
        }

        @Override
        public void onResult(VerifierResult result) {
            mErrorResult.setText("");

            if (result.ret == 0) {
                // 验证通过 这里就意味着通过了！！！
                mResultText.setText("验证通过,打开****");

            } else {
                // 验证不通过
                switch (result.err) {
                    case VerifierResult.MSS_ERROR_IVP_GENERAL:
                        mErrorResult.setText("内核异常");
                        break;
                    case VerifierResult.MSS_ERROR_IVP_TRUNCATED:
                        mErrorResult.setText("出现截幅");
                        break;
                    case VerifierResult.MSS_ERROR_IVP_MUCH_NOISE:
                        mErrorResult.setText("太多噪音");
                        break;
                    case VerifierResult.MSS_ERROR_IVP_UTTER_TOO_SHORT:
                        mErrorResult.setText("录音太短");
                        break;
                    case VerifierResult.MSS_ERROR_IVP_TEXT_NOT_MATCH:
                        mErrorResult.setText("验证不通过，您所读的文本不一致");
                        break;
                    case VerifierResult.MSS_ERROR_IVP_TOO_LOW:
                        mErrorResult.setText("音量太低");
                        break;
                    case VerifierResult.MSS_ERROR_IVP_NO_ENOUGH_AUDIO:
                        mErrorResult.setText("音频长达不到自由说的要求");
                        break;
                    default:
                        mErrorResult.setText("验证不通过,相似度仅为" + result.score + "%。");
                        break;
                }
            }
        }

        // 保留方法，暂不用
        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle arg3) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //    String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //    Log.d(TAG, "session id =" + sid);
            // }
        }

        @Override
        public void onError(SpeechError error) {

            switch (error.getErrorCode()) {
                case ErrorCode.MSP_ERROR_NOT_FOUND:
                    mResultText.setText("模型不存在，请先注册");
                    break;

                default:
                    showTip("onError Code：" + error.getPlainDescription(true));
                    break;
            }
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            showTip("结束说话");
        }

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            showTip("开始说话");
        }
    };


    private SpeechListener mModelOperationListener = new SpeechListener() {

        @Override
        public void onEvent(int eventType, Bundle params) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {

            String result = new String(buffer);
            try {
                JSONObject object = new JSONObject(result);
                String cmd = object.getString("cmd");
                int ret = object.getInt("ret");

                if ("del".equals(cmd)) {
                    if (ret == ErrorCode.SUCCESS) {
                        showTip("删除成功");
                        mResultText.setText("");
                    } else if (ret == ErrorCode.MSP_ERROR_FAIL) {
                        showTip("删除失败，模型不存在");
                    }

                } else if ("que".equals(cmd)) {
                    if (ret == ErrorCode.SUCCESS) {
                        showTip("模型存在");
                    } else if (ret == ErrorCode.MSP_ERROR_FAIL) {
                        showTip("模型不存在");
                    }
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (null != error && ErrorCode.SUCCESS != error.getErrorCode()) {
                showTip("操作失败：" + error.getPlainDescription(true));
            }
        }
    };

    private boolean checkInstance() {
        if (null == mSpeakerVerifier) {
            // 创建单例失败，与 21001 错误为同样原因，
            this.showTip("创建对象失败，请确认 libmsc.so 放置正确，\n 且有调用 createUtility 进行初始化");
            return false;
        } else {
            return true;
        }
    }

    private void cancelOperation() {
        mSpeakerVerifier.cancel();
    }

    private String getAuthid() {
        String id = mAuthidEditText.getText() == null ? null : mAuthidEditText.getText().toString();
        return id;
    }


    @Override
    public void finish() {
        if (null != mTextPwdSelectDialog) {
            mTextPwdSelectDialog.dismiss();
        }
        super.finish();
    }

}



















