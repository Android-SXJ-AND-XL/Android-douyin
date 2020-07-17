package com.example.hw8;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraDevice;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmbedCameraActivity extends AppCompatActivity {

    private static final String TAG = "mediaRecorder";
    private SurfaceView surfaceView;
    private SurfaceHolder holder;
    private Camera camera;
    private Camera.PictureCallback mPictureCallback;
    private ImageView ivPhoto;
    private Button btnPhoto;
    private Button btnRecord;
    private Button btnFlash;
    private Button btnDelay;
    private MediaRecorder mediaRecorder;
    private VideoView videoViewRecord;
    private boolean isRecording = false;
    private boolean isFlash = false;
    private File mp4path;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_embed_camera);

        initView();
    }

    private void initView() {
        surfaceView = findViewById(R.id.surfaceView);
        ivPhoto = findViewById(R.id.iv_embed_photo);
        videoViewRecord = findViewById(R.id.video_embed_record);
        btnPhoto = findViewById(R.id.btn_embed_takePhoto);
        btnRecord = findViewById(R.id.btn_embed_record);
        btnFlash = findViewById(R.id.btn_embed_flash);
        btnDelay = findViewById(R.id.btn_embed_delay);
        holder = surfaceView.getHolder();
        holder.addCallback(new CameraCallback());
        initCamera();
        mPictureCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                FileOutputStream fos = null;
                String imageName = "img_" + new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(new Date()) + ".jpg";
                String filepath = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + File.separator + imageName;
                Log.i("filepath",filepath);
                File file = new File(filepath);
                try {
                    fos = new FileOutputStream(file);
                    fos.write(data);
                    fos.flush();
                    Glide.with(EmbedCameraActivity.this).load(file).into(ivPhoto);
                    ivPhoto.setVisibility(View.VISIBLE);
                    videoViewRecord.setVisibility(View.GONE);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    camera.startPreview();
                    if(fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };

        //拍照
        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                camera.takePicture(null,null,mPictureCallback);
            }
        });

        //录像
        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                record(view);
            }
        });

        //闪光灯
        btnFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Camera.Parameters parameter = camera.getParameters();
                if(isFlash){
                    parameter.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                } else {
                    parameter.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                }
                isFlash = !isFlash;
                camera.setParameters(parameter);
            }
        });

        //延时拍摄
        btnDelay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final List<Button> buttonList = Arrays.asList(btnPhoto, btnFlash, btnRecord, btnDelay);
                disableButton(buttonList);

                new CountDownTimer(8000, 1000){
                    @Override
                    public void onTick(long millisUntilFinished) {
                        btnDelay.setText(String.valueOf(millisUntilFinished / 1000));
                    }

                    @Override
                    public void onFinish() {
                        btnDelay.setText("延时");
                        camera.takePicture(null, null, mPictureCallback);
                        enableButton(buttonList);
                    }
                }.start();

            }
        });

    }

    private void initCamera() {
        camera = Camera.open();
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPictureFormat(ImageFormat.JPEG);
        //实时聚焦
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE); //FOCUS_MODE_AUTO
        parameters.set("orientation","portrait");
        parameters.set("rotation",90);
        camera.setParameters(parameters);
        camera.setDisplayOrientation(90);
    }

    private class CameraCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            if(holder.getSurface() == null) {
                return;
            }

            camera.stopPreview();

            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    private void enableButton(final List<Button> list){
        for(Button button : list){
            button.setEnabled(true);
        }
    }

    private void disableButton(final List<Button> list){
        for(Button button : list) {
            button.setEnabled(false);
        }
    }

    public void record(View view) {
        if(isRecording) {
            btnRecord.setText("录制");
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            camera.lock();

            videoViewRecord.setVisibility(View.VISIBLE);
            ivPhoto.setVisibility(View.GONE);
            videoViewRecord.setVideoPath(mp4path.getAbsolutePath());
            videoViewRecord.start();
            isRecording = false;
        }
        else {
            if(prepareVideoRecorder()) {
                btnRecord.setText("停止");
                mediaRecorder.start();
                isRecording = true;
            }
            else {
                mediaRecorder.reset();
                mediaRecorder.release();
                Log.i(TAG,"MediaRecorder prepare failed");
            }
        }
    }

    private boolean prepareVideoRecorder() {
        mediaRecorder = new MediaRecorder();
        camera.unlock();
        mediaRecorder.setCamera(camera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        mp4path = getOutputMediaPath();
        mediaRecorder.setOutputFile(mp4path.getAbsolutePath());
        mediaRecorder.setPreviewDisplay(holder.getSurface());
        mediaRecorder.setOrientationHint(90);
        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            mediaRecorder.release();
            return false;
        }
        return true;
    }

    public File getOutputMediaPath() {
        String mediaName = "mov_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";
        File mediaFile = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES),mediaName);
        if(!mediaFile.exists()) {
            mediaFile.getParentFile().mkdirs();
        }
        return mediaFile;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(camera == null) {
            initCamera();
        }
        camera.startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        camera.stopPreview();
    }
}
