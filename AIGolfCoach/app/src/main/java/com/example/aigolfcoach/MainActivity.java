package com.example.aigolfcoach;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static int CAMERA_PERMISSION_CODE = 100;
    private static int VIDEO_RECORD_CODE = 101;
    private static final int VIDEO_PICK_GALLERY_CODE = 102;

    private Uri videoPath = null;

    private VideoView videoView;
    private FloatingActionButton pickVideoFab;
    private Button uploadVideoBtn;
    private Button showHisotryBtn;
    private ProgressDialog progressDial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView = findViewById(R.id.videoView);
        pickVideoFab = findViewById(R.id.pickVideoFab);
        uploadVideoBtn = findViewById(R.id.uploadBtn);
        showHisotryBtn = findViewById(R.id.showHistoryBtn);

        // setup progress dialog
        progressDial = new ProgressDialog(this);
        progressDial.setTitle("Please Wait");
        progressDial.setMessage("Uploading Video");
        progressDial.setCanceledOnTouchOutside(false);

        // check permission
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
                }else{
                    uploadVideoToFirebase();
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

    private void uploadVideoToFirebase(){
        // show progress
        progressDial.show();

        // timestamp
        String timestamp = "" + System.currentTimeMillis();

        // file path and name in firebase storage
        String filePathAndName = "Videos/" + "Video_" + timestamp;

        //storage reference
        StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
        // upload video, you can upload any type of file using this method
        storageReference.putFile(videoPath)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // video uploaded, get url of uploaded video
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while(!uriTask.isSuccessful());
                        Uri downloadUri = uriTask.getResult();
                        while (!uriTask.isSuccessful()){
                            // uri of uploaded video is received


                            //now we can add video detail to our firebass database
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("id", ""+timestamp);
                            hashMap.put("timestamp", "" + timestamp);
                            hashMap.put("videoUrl", "" + downloadUri);

                            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Videos");
                            reference.child(timestamp)
                                    .setValue(hashMap)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            // video details added to databse
                                            progressDial.dismiss();
                                            Toast.makeText(MainActivity.this, "Video uploaded...", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // failed adding details to db
                                            progressDial.dismiss();
                                            Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // failed uploading to storage
                        progressDial.dismiss();
                        Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        progressDial.dismiss();


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
            Log.i("VIDEO_PICK_GALLERY","Video is picked from path" +videoPath );
            setVideoToVideoView();

        }
    }

}