package com.example.douyin.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.example.douyin.R;
import com.example.douyin.model.CurrentUser;
import com.example.douyin.net.IMiniDouyinService;
import com.example.douyin.net.response.PostVideoResponse;
import com.example.douyin.utils.ResourceUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class EmbedCameraActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private static final int PICK_VIDEO = 2;
    public static final int RESULT_ERROR = 404;
    public static final int RESULT_PERMISSION_DENIED = 1001;
    private final static int REQUEST_PATH = 1234;
    private static final String TAG = "mediaRecorder";
    private Uri mSelectedImage;
    private Uri mSelectedVideo;
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
    private static final String STORAGE_PATH =
            Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES) + File.separator + "mDouyin";
    private List<Button> firstDisableButton;
    private Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(IMiniDouyinService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    private IMiniDouyinService miniDouyinService = retrofit.create(IMiniDouyinService.class);

    public static void launch(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, EmbedCameraActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_embed_camera);

        final String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        if(checkPermission(permissions))
            initView();
    }

    private boolean checkPermission(String[] permissions) {
        for(String permission : permissions) {
            if(checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,permissions,REQUEST_PATH);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PATH) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initView();

            } else {
                finishCameraActivity(RESULT_PERMISSION_DENIED);
            }
        }
    }

    private void initView() {
        surfaceView = findViewById(R.id.surfaceView);
        ivPhoto = findViewById(R.id.iv_embed_photo);
        videoViewRecord = findViewById(R.id.video_embed_record);
        btnPhoto = findViewById(R.id.btn_embed_takePhoto);
        btnRecord = findViewById(R.id.btn_embed_record);
        btnFlash = findViewById(R.id.btn_embed_flash);
        btnDelay = findViewById(R.id.btn_embed_delay);
        firstDisableButton = Arrays.asList(btnRecord);
        holder = surfaceView.getHolder();
        holder.addCallback(new CameraCallback());
        initCamera();
        toPhotoState(firstDisableButton);
        mPictureCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                FileOutputStream fos = null;
                String imageName = "img_" + new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(new Date()) + ".jpg";
                String filepath = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getPath() + File.separator+ imageName;
//                String filepath = Environment.getExternalStorageDirectory() + "/" + imageName;
//                String filepath = STORAGE_PATH + File.separator + imageName;
                Log.i("filepath",filepath);
                File file = new File(filepath);
                try {
                    fos = new FileOutputStream(file);
                    fos.write(data);
                    fos.flush();
                    Glide.with(EmbedCameraActivity.this).load(file).into(ivPhoto);
                    ivPhoto.setVisibility(View.VISIBLE);
                    videoViewRecord.setVisibility(View.GONE);
                    mSelectedImage = Uri.fromFile(file);
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
                toVideoState(new ArrayList<>(Arrays.asList(btnRecord)));
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
            videoViewRecord.setVideoPath(mp4path.getPath());
            videoViewRecord.start();
            isRecording = false;
            mSelectedVideo = Uri.fromFile(mp4path);
            postVideo();
        }
        else {
            if(prepareVideoRecorder()) {
                btnRecord.setText("停止");
                mediaRecorder.start();
                isRecording = true;
            }
            else {
                Log.i(TAG,"MediaRecorder prepare failed");
                mediaRecorder.reset();
                mediaRecorder.release();
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

    private void postVideo()
    {
        if(mSelectedImage == null && mSelectedVideo == null)
            finishCameraActivity(RESULT_ERROR);

//        NetManager netManager = new NetManager();
//        netManager.setOnPostListener(new OnNetListener() {
//            @Override
//            public void exec(Response<?> res)
//            {
//                // create videos
//                PostVideoResponse response = (PostVideoResponse)res.body();
//                if(!response.isSuccess())
//                    finishCameraActivity(RESULT_ERROR);
//                else
//                    finishCameraActivity(RESULT_OK);
//            }
//        });

        MultipartBody.Part coverImagePart = getMultipartFromUri("cover_image", mSelectedImage);
        MultipartBody.Part videoPart = getMultipartFromUri("video", mSelectedVideo);

        miniDouyinService.postVideo(CurrentUser.getStudentID() /*你的id（可以是自己的手机号）*/, CurrentUser.getUsername()/*你的名字*/, coverImagePart, videoPart).enqueue(
                new Callback<PostVideoResponse>() {
                    @Override
                    public void onResponse(Call<PostVideoResponse> call, Response<PostVideoResponse> response) {
                        if (response.body() != null) {
                            Toast.makeText(EmbedCameraActivity.this, response.body().toString(), Toast.LENGTH_SHORT)
                                    .show();
                            finishCameraActivity(RESULT_OK);
                        }
                    }

                    @Override
                    public void onFailure(Call<PostVideoResponse> call, Throwable throwable) {
                        Toast.makeText(EmbedCameraActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        finishCameraActivity(RESULT_ERROR);
                    }
                });
//        netManager.execPostFeed(
//                CurrentUser.getStudentID(), CurrentUser.getUsername(),
//                coverImagePart, videoPart
//        );

    }
    private MultipartBody.Part getMultipartFromUri(String name, Uri uri) {
        File f = new File(ResourceUtils.getRealPath(this, uri));
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), f);
        return MultipartBody.Part.createFormData(name, f.getName(), requestFile);
    }


    private void finishCameraActivity(int resultCode)
    {
        setResult(resultCode);
        this.finish();
    }

    public File getOutputMediaPath() {
        String mediaName = "mov_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".mp4";
        File mediaFile = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES),mediaName);
//        File mediaFile = new File(STORAGE_PATH,mediaName);
        if(!mediaFile.exists()) {
            mediaFile.getParentFile().mkdirs();
        }
        return mediaFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult() called with: requestCode = ["
                + requestCode
                + "], resultCode = ["
                + resultCode
                + "], data = ["
                + data
                + "]");

        if (resultCode == RESULT_OK && null != data) {
            if (requestCode == PICK_IMAGE) {
                mSelectedImage = data.getData();
                Log.d(TAG, "selectedImage = " + mSelectedImage);
                toVideoState(firstDisableButton);

            } else if (requestCode == PICK_VIDEO) {
                mSelectedVideo = data.getData();
                Log.d(TAG, "mSelectedVideo = " + mSelectedVideo);
                postVideo();
            }
        }
    }

    private void toPhotoState(List<Button> buttons)
    {
        disableButton(buttons);
        Toast.makeText(this,"请先上传照片",Toast.LENGTH_SHORT).show();
    }

    private void toVideoState(List<Button> buttons)
    {
        enableButton(buttons);
        disableButton(new ArrayList<>(Arrays.asList(btnDelay,btnPhoto)));
        Toast.makeText(this,"请上传视频",Toast.LENGTH_SHORT).show();
    }

    private void notifySystemAlbum(Uri uri)
    {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(uri);
        sendBroadcast(intent);
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
