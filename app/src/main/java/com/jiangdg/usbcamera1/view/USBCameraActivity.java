package com.jiangdg.usbcamera1.view;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jiangdg.usbcamera1.R;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.jiangdg.usbcamera1.UVCCameraHelper;
import com.jiangdg.usbcamera1.application.MyApplication;
import com.jiangdg.usbcamera1.utils.FileUtils;
import com.jiangdg.usbcamera1.utils.MethodsUtils;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.encoder.RecordParams;
import com.serenegiant.usb.widget.CameraViewInterface;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * UVCCamera use demo
 * <p>
 * Created by jiangdongguo on 2017/9/30.
 */

public class USBCameraActivity extends AppCompatActivity implements CameraDialog.CameraDialogParent, CameraViewInterface.Callback {
    private static final String TAG = "Debug";
    @BindView(R.id.camera_view)
    public View mTextureView;
    @BindView(R.id.toolbar)
    public Toolbar mToolbar;
    @BindView(R.id.seekBar_sensitivity)
    public SeekBar mSeekSensitivity;
    @BindView(R.id.seekbar_saturation)
    public SeekBar mSeekSaturation;
    @BindView(R.id.seekbar_brightness)
    public SeekBar mSeekBrightness;
    @BindView(R.id.seekbar_contrast)
    public SeekBar mSeekContrast;
//    @BindView(R.id.switch_rec_voice)
//    public Switch mSwitchVoice;

    @BindView(R.id.spinner)
    public Spinner spinner;


    private UVCCameraHelper mCameraHelper;
    private CameraViewInterface mUVCCameraView;
    private AlertDialog mDialog;

    private boolean isRequest;
    private boolean isPreview;

    private int interval;
    private int count = 1; //定时拍照计数

    private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {

        @Override
        public void onAttachDev(UsbDevice device) {
            // request open permission
            if (!isRequest) {
                isRequest = true;
                if (mCameraHelper != null) {
                    mCameraHelper.requestPermission(0);
                }
            }
        }

        @Override
        public void onDettachDev(UsbDevice device) {
            // close camera
            if (isRequest) {
                isRequest = false;
                mCameraHelper.closeCamera();
                showShortMsg(device.getDeviceName() + " is out");
            }
        }

        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {
            if (!isConnected) {
                showShortMsg("fail to connect,please check resolution params");
                isPreview = false;
            } else {
                isPreview = true;
                showShortMsg("connecting");
                // initialize seekbar
                // need to wait UVCCamera initialize over
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Looper.prepare();
                        if(mCameraHelper != null && mCameraHelper.isCameraOpened()) {
                            mSeekBrightness.setProgress(mCameraHelper.getModelValue(UVCCameraHelper.MODE_BRIGHTNESS));
                            mSeekContrast.setProgress(mCameraHelper.getModelValue(UVCCameraHelper.MODE_CONTRAST));
                        }
                        Looper.loop();
                    }
                }).start();
            }
        }

        @Override
        public void onDisConnectDev(UsbDevice device) {
            showShortMsg("disconnecting");
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usbcamera);
        ButterKnife.bind(this);
        initView();

        // step.1 initialize UVCCameraHelper
        mUVCCameraView = (CameraViewInterface) mTextureView;
        mUVCCameraView.setCallback(this);
        mCameraHelper = UVCCameraHelper.getInstance();
        mCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_MJPEG);
       // mCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_YUYV);
        mCameraHelper.initUSBMonitor(this, mUVCCameraView, listener);

        mCameraHelper.setOnPreviewFrameListener(new AbstractUVCCameraHandler.OnPreViewResultListener() {
            @Override
            public void onPreviewResult(byte[] nv21Yuv) {
                Log.d(TAG, "onPreviewResult: "+nv21Yuv.length);
            }
        });

        getLED_V(spinner);
        TextView textView2 = findViewById(R.id.textView2);
        TextView editTextView = (EditText)findViewById(R.id.editTextNumber);
        //输入改变interval的间隔
        setInterval(editTextView); //TODO 修改
        //实现定时拍照功能；
        Handler handler=new Handler();
        Runnable runnable=new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                showShortMsg("Take a picture!");
                textView2.setText(String.valueOf(count++));
                takeTimedPics();// 睡眠之后执行方法，如果要修改更新UI，需要配合Handler的使用
                handler.postDelayed(this, interval *1000);// 2秒后执行
            }
        };
        //设置定时拍照
        ToggleButton tbt = findViewById(R.id.toggleButton2);
        tbt.setOnClickListener((View)->{
            if(tbt.isChecked()){
                String str1 = editTextView.getText().toString();
                try{
                    interval = Integer.parseInt(str1);
                }catch(NumberFormatException e){
                    tbt.setChecked(false);
                    showShortMsg("输入正确数字格式！");
                }
                if(interval > 0 && interval <= 100){
                    handler.post(runnable);
                }
                else {
                    showShortMsg("请输入1 —— 100的数字！");
                    tbt.setChecked(false);
                    showShortMsg("Please enter the interval time！");
                }
            }
            else{
                handler.removeCallbacks(runnable);
            }
        });
    }

    private void getLED_V(Spinner spinner) {
        //拿到数据
        final String[] arrays = getResources ().getStringArray (R.array.spingarr);
        //获取系统自带的适配器
        ArrayAdapter<String> mSpinnerAdapter = new ArrayAdapter<String> (USBCameraActivity.this,android.R.layout.simple_spinner_dropdown_item,arrays);
        spinner.setAdapter (mSpinnerAdapter);
        //添加点击事件
        spinner.setOnItemLongClickListener (new AdapterView.OnItemLongClickListener () {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText (USBCameraActivity.this, "哈喽" + arrays[position], Toast.LENGTH_SHORT).show ();
                return false;
            }
        });
    }

//TODO wanshan
    private void setInterval(TextView editTextView){
        //TODO Complete the code!
        editTextView.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                String strs = editTextView.getText().toString();
//                //正则限制只输入数字字母汉字
//                String str = MethodsUtils.stringFilter2(strs, "[^A-Z0-9a-z\u4E00-\u9FA5]");
//                if (!strs.equals(str)) {
//                    editTextView.setText(str);
//                    editTextView.setEms(str.length());
//                }
            }
            @Override
            public void afterTextChanged(Editable s) {
//                String str = (String) editTextView.getText();
//                interval = Integer.parseInt(str);
            }
        });



    }
    //拍照
    public void takeTimedPics(){
        if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
            showShortMsg("sorry,camera open failed");
        }

        SimpleDateFormat timesdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //SimpleDateFormat filesdf = new SimpleDateFormat("yyyy-MM-dd HHmmss"); //文件名不能有：
        String FileTime =timesdf.format(new Date()).toString();//获取系统时间
        String filename = FileTime.replace(":", "");


        /*String picPath = UVCCameraHelper.ROOT_PATH + MyApplication.DIRECTORY_NAME +"/images/"
                + System.currentTimeMillis() + UVCCameraHelper.SUFFIX_JPEG;*/
        String picPath = UVCCameraHelper.ROOT_PATH + MyApplication.DIRECTORY_NAME +"/images/"
                + filename + UVCCameraHelper.SUFFIX_JPEG;

        mCameraHelper.capturePicture(picPath, new AbstractUVCCameraHandler.OnCaptureListener() {
            @Override
            public void onCaptureResult(String path) {
                if(TextUtils.isEmpty(path)) {
                    return;
                }
                new Handler(getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(USBCameraActivity.this, "save path:"+path, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

//调整摄像头参数；饱和度，亮度，对比度，感光度
    private void initView() {
        setSupportActionBar(mToolbar);

        //TODO 需要加感光度的调节
        TextView textView7 = findViewById(R.id.textView7);
        mSeekSensitivity.setMax(100);
        mSeekSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //TODO 调节控制芯片
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.v("停止滑动时的值：", String.valueOf(seekBar.getProgress()));
                textView7.setText(String.valueOf(seekBar.getProgress()));
            }
        });
        // add saturation by fuzm
        TextView textView3 = findViewById(R.id.textView3);
        mSeekSaturation.setMax(100);
        mSeekSaturation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mCameraHelper != null && mCameraHelper.isCameraOpened()) {
                    mCameraHelper.setModelValue(UVCCameraHelper.MODE_SATURATION,progress);
                    Log.v("拖动过程中的值：", String.valueOf(progress) + ", " + String.valueOf(fromUser));
                    //textView3.setText(seekBar.get);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.v("开始滑动时的值：", String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.v("停止滑动时的值：", String.valueOf(seekBar.getProgress()));
                textView3.setText(String.valueOf(seekBar.getProgress()));
            }
        });
        //调节亮度
        TextView textView6 = findViewById(R.id.textView6);
        mSeekBrightness.setMax(100);
        mSeekBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mCameraHelper != null && mCameraHelper.isCameraOpened()) {
                    mCameraHelper.setModelValue(UVCCameraHelper.MODE_BRIGHTNESS,progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.v("停止滑动时的值：", String.valueOf(seekBar.getProgress()));
                textView6.setText(String.valueOf(seekBar.getProgress()));
            }
        });
        //调节对比度
        TextView textView4 = findViewById(R.id.textView4);
        mSeekContrast.setMax(100);
        mSeekContrast.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mCameraHelper != null && mCameraHelper.isCameraOpened()) {
                    mCameraHelper.setModelValue(UVCCameraHelper.MODE_CONTRAST,progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.v("停止滑动时的值：", String.valueOf(seekBar.getProgress()));
                textView4.setText(String.valueOf(seekBar.getProgress()));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // step.2 register USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.registerUSB();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // step.3 unregister USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.unregisterUSB();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_toobar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_takepic:
                if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
                    showShortMsg("sorry,camera open failed");
                    return super.onOptionsItemSelected(item);
                }
                String picPath = UVCCameraHelper.ROOT_PATH + MyApplication.DIRECTORY_NAME +"/images/"
                        + System.currentTimeMillis() + UVCCameraHelper.SUFFIX_JPEG;

                mCameraHelper.capturePicture(picPath, new AbstractUVCCameraHandler.OnCaptureListener() {
                    @Override
                    public void onCaptureResult(String path) {
                        if(TextUtils.isEmpty(path)) {
                            return;
                        }
                        new Handler(getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(USBCameraActivity.this, "save path:"+path, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

                break;
            case R.id.menu_recording:
                if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
                    showShortMsg("sorry,camera open failed");
                    return super.onOptionsItemSelected(item);
                }
                if (!mCameraHelper.isPushing()) {
                    String videoPath = UVCCameraHelper.ROOT_PATH + MyApplication.DIRECTORY_NAME +"/videos/" + System.currentTimeMillis()
                            + UVCCameraHelper.SUFFIX_MP4;

//                    FileUtils.createfile(FileUtils.ROOT_PATH + "test666.h264");
                    // if you want to record,please create RecordParams like this
                    RecordParams params = new RecordParams();
                    params.setRecordPath(videoPath);
                    params.setRecordDuration(0);                        // auto divide saved,default 0 means not divided
//                    params.setVoiceClose(mSwitchVoice.isChecked());    // is close voice

                    params.setSupportOverlay(true); // overlay only support armeabi-v7a & arm64-v8a
                    mCameraHelper.startPusher(params, new AbstractUVCCameraHandler.OnEncodeResultListener() {
                        @Override
                        public void onEncodeResult(byte[] data, int offset, int length, long timestamp, int type) {
                            // type = 1,h264 video stream
                            if (type == 1) {
                                FileUtils.putFileStream(data, offset, length);
                            }
                            // type = 0,aac audio stream
                            if(type == 0) {

                            }
                        }

                        @Override
                        public void onRecordResult(String videoPath) {
                            if(TextUtils.isEmpty(videoPath)) {
                                return;
                            }
                            new Handler(getMainLooper()).post(() -> Toast.makeText(USBCameraActivity.this, "save videoPath:"+videoPath, Toast.LENGTH_SHORT).show());
                        }
                    });
                    // if you only want to push stream,please call like this
                    // mCameraHelper.startPusher(listener);
                    showShortMsg("start record...");
//                    mSwitchVoice.setEnabled(false);
                } else {
                    FileUtils.releaseFile();
                    mCameraHelper.stopPusher();
                    showShortMsg("stop record...");
//                    mSwitchVoice.setEnabled(true);
                }
                break;
            case R.id.menu_resolution:
                if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
                    showShortMsg("sorry,camera open failed");
                    return super.onOptionsItemSelected(item);
                }
                showResolutionListDialog();
                break;
            case R.id.menu_focus:
                if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
                    showShortMsg("sorry,camera open failed");
                    return super.onOptionsItemSelected(item);
                }
                mCameraHelper.startCameraFoucs();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showResolutionListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(USBCameraActivity.this);
        View rootView = LayoutInflater.from(USBCameraActivity.this).inflate(R.layout.layout_dialog_list, null);
        ListView listView = (ListView) rootView.findViewById(R.id.listview_dialog);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(USBCameraActivity.this, android.R.layout.simple_list_item_1, getResolutionList());
        if (adapter != null) {
            listView.setAdapter(adapter);
        }
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (mCameraHelper == null || !mCameraHelper.isCameraOpened())
                    return;
                final String resolution = (String) adapterView.getItemAtPosition(position);
                String[] tmp = resolution.split("x");
                if (tmp != null && tmp.length >= 2) {
                    int widht = Integer.valueOf(tmp[0]);
                    int height = Integer.valueOf(tmp[1]);
                    mCameraHelper.updateResolution(widht, height);
                }
                mDialog.dismiss();
            }
        });

        builder.setView(rootView);
        mDialog = builder.create();
        mDialog.show();
    }

    // example: {640x480,320x240,etc}
    private List<String> getResolutionList() {
        List<Size> list = mCameraHelper.getSupportedPreviewSizes();
        List<String> resolutions = null;
        if (list != null && list.size() != 0) {
            resolutions = new ArrayList<>();
            for (Size size : list) {
                if (size != null) {
                    resolutions.add(size.width + "x" + size.height);
                }
            }
        }
        return resolutions;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileUtils.releaseFile();
        // step.4 release uvc camera resources
        if (mCameraHelper != null) {
            mCameraHelper.release();
        }
    }

    private void showShortMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mCameraHelper.getUSBMonitor();
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            showShortMsg("取消操作");
        }
    }

    public boolean isCameraOpened() {
        return mCameraHelper.isCameraOpened();
    }

    @Override
    public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
        if (!isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.startPreview(mUVCCameraView);
            isPreview = true;
        }
    }

    @Override
    public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

    }

    @Override
    public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
        if (isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.stopPreview();
            isPreview = false;
        }
    }
}
