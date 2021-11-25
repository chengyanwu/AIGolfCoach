package com.example.aigolfcoach;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.widget.Button;
import android.widget.MediaController;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private static int CAMERA_PERMISSION_CODE = 100;
    private static int VIDEO_RECORD_CODE = 101;
    private static final int VIDEO_PICK_GALLERY_CODE = 102;

    private Uri videoPath = null;
    private VideoView videoView;
    private FloatingActionButton pickVideoFab;
    private Button uploadVideoBtn;
    private Button showHisotryBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView = findViewById(R.id.videoView);
        pickVideoFab = findViewById(R.id.pickVideoFab);
        uploadVideoBtn = findViewById(R.id.uploadBtn);
        showHisotryBtn = findViewById(R.id.showHistoryBtn);

        if(isCameraPresentInPhone()){
            Log.i("VIDEO_RECORD_TAG", "Camera Detected");
            getCameraPermission();

        }else{
            Log.i("VIDEO_RECORD_TAG", "No Camera Detected");
        }

        pickVideoFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickVideoSource();
            }
        });

        uploadVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(videoPath == null){
                    Toast.makeText(MainActivity.this, "Select a Video First", Toast.LENGTH_SHORT).show();
                }
            }
        });

        showHisotryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showHistory();
            }
        });
    }

    private boolean isCameraPresentInPhone(){
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
            return true;
        }else{
            return false;
        }
    }

    private void getCameraPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.CAMERA},CAMERA_PERMISSION_CODE );
        }
    }

    private void recordVideo(){
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(intent, VIDEO_RECORD_CODE);
    }

    private void showHistory() {
        Intent intent = new Intent(MainActivity.this, DisplayHistoryActivity.class);
        Log.i("SHOW_HISTORY_TAG", "showing history");
        startActivity(intent);
    }

    private void pickVideoSource(){
        // options in the dialog
        String[] options = {"Camera", "Gallery"};

        //dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Video From")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(i ==1){
                            Log.i("PICK VIDEO", "Gallery CLicked");
                            pickVideoFromGallery();
                        }else{
                            Log.i("PICK VIDEO", "Camera CLicked");
                            recordVideo();
                        }
                    }
                })
                .show();
    }

    private void pickVideoFromGallery(){
//        Intent intent = new Intent();
//        intent.setType("Video/*");
//        intent.setAction(Intent.ACTION_GET_CONTENT);
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(intent, "Select Videos"), VIDEO_PICK_GALLERY_CODE);
    }

    private void setVideoToVideoView(){
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);

        //set media controller to video view
        videoView.setMediaController(mediaController);
        // set video url
        videoView.setVideoURI(videoPath);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                videoView.pause();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIDEO_RECORD_CODE) {
            if (resultCode == RESULT_OK) {

                videoPath = data.getData();
                Log.i("VIDEO_RECORD_TAG", "Video is recorded and available at path" +videoPath);
            }else if (resultCode == RESULT_CANCELED){
                Log.i("VIDEO_RECORD_TAG", "Recording video is canceled");
            }else{
                Log.i("VIDEO_RECORD_TAG", "Recording video has errors");
            }
            setVideoToVideoView();
        }
        if(requestCode == VIDEO_PICK_GALLERY_CODE){
            videoPath = data.getData();
            setVideoToVideoView();

        }
    }

}