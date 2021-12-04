package com.example.aigolfcoach;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.aigolfcoach.gles.GlPlayerRenderer;
import com.example.aigolfcoach.gles.GlPlayerView;
import com.example.aigolfcoach.preference.PreferenceUtils;
import com.example.aigolfcoach.preview.VisionProcessorBase;
import com.example.aigolfcoach.preview.posedetector.PoseDetectorProcessor;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GlPlayerRenderer.FrameListener{
    private static final String TAG = MainActivity.class.getSimpleName();

    private static int CAMERA_PERMISSION_CODE = 100;
    private static int VIDEO_RECORD_CODE = 101;
    private static final int VIDEO_PICK_GALLERY_CODE = 102;
    private static final int REQUEST_CHOOSE_VIDEO = 1003;

    private static final String SPINE_TRACKING = "Spine Tracking";
    private static final String HEAD_TRACKING = "Head Tracking";

    private Uri videoPath = null;

    private VideoView videoView;
    private FloatingActionButton pickVideoFab;
    private Button uploadVideoBtn;
    private Button showHisotryBtn;
    private ProgressDialog progressDial;

    private SimpleExoPlayer player;
    private PlayerView playerView;
    private GlPlayerView glPlayerView;
    private GraphicOverlay graphicOverlay;

    private String selectedFunction = SPINE_TRACKING;
    private VisionProcessorBase imageProcessor;

    private int frameWidth, frameHeight;
    private boolean processing;
    private boolean pending;
    private Bitmap lastFrame;

    @Override
    public void onFrame(Bitmap bitmap) {
        processFrame(bitmap);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        player = new SimpleExoPlayer.Builder(this).build();
        playerView = findViewById(R.id.player_view);
        playerView.setPlayer(player);

        FrameLayout contentFrame = playerView.findViewById(R.id.exo_content_frame);
        glPlayerView = new GlPlayerView(this);
        glPlayerView.setSimpleExoPlayer(player);
        glPlayerView.setFrameListener(this);
        View videoFrameView = glPlayerView;

        if(videoFrameView != null) contentFrame.addView(videoFrameView);

        graphicOverlay = new GraphicOverlay(this, null);
        contentFrame.addView(graphicOverlay);

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

        populateFunctionSelector();
    }

    @Override
    protected void onResume() {
        super.onResume();
        createFunctionProcessor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        player.pause();
        stopImageProcessor();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.stop();
        player.release();
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
                        if (uriTask.isSuccessful()){
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

    private void setupPlayer(Uri uri){
        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.stop();
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    protected void processFrame(Bitmap frame){
        lastFrame = frame;
        if(imageProcessor != null){
            pending = processing;
            if(!processing){
                processing = true;
                if(frameWidth != frame.getWidth() || frameHeight != frame.getHeight()){
                    frameWidth = frame.getWidth();
                    frameHeight = frame.getHeight();
                    graphicOverlay.setImageSourceInfo(frameWidth, frameHeight, false);
                }
                imageProcessor.setOnProcessingCompleteListener(new VisionProcessorBase.OnProcessingCompleteListener() {
                    @Override
                    public void onProcessingComplete() {
                        processing = false;
                        onProcessComplete(frame);
                        if(pending) processFrame(lastFrame);
                    }
                });
                imageProcessor.processBitmap(frame, graphicOverlay);
            }
        }
    }

    protected void onProcessComplete(Bitmap frame){ }

    private void populateFunctionSelector(){
        Spinner functionSpinner = findViewById(R.id.function_selector);
        List<String> options = new ArrayList<>();
        options.add(SPINE_TRACKING);
        options.add(HEAD_TRACKING);

        // Creating adapter for featureSpinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_style, options);
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // attaching data adapter to spinner
        functionSpinner.setAdapter(dataAdapter);
        functionSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                        selectedFunction = parentView.getItemAtPosition(pos).toString();
                        createFunctionProcessor();
                        if(lastFrame != null) processFrame(lastFrame);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {}
                });
    }

    private void createFunctionProcessor(){

        PoseDetectorOptionsBase poseDetectorOptions =
                PreferenceUtils.getPoseDetectorOptionsForLivePreview(this);
        boolean shouldShowInFrameLikelihood = false;
        boolean visualizeZ = false;
        boolean rescaleZ = PreferenceUtils.shouldPoseDetectionRescaleZForVisualization(this);
        boolean runClassification = PreferenceUtils.shouldPoseDetectionRunClassification(this);
        imageProcessor =
                new PoseDetectorProcessor(
                        this,
                        poseDetectorOptions,
                        selectedFunction,
                        shouldShowInFrameLikelihood,
                        visualizeZ,
                        rescaleZ,
                        runClassification,
                        /* isStreamMode = */ true);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIDEO_RECORD_CODE) {
            if (resultCode == RESULT_OK) {
                videoPath = data.getData();
                setupPlayer(videoPath);
                Log.i("VIDEO_RECORD_TAG", "Video is recorded and available at path" +videoPath);
            }else if (resultCode == RESULT_CANCELED){
                Log.i("VIDEO_RECORD_TAG", "Recording video is canceled");
            }else{
                Log.i("VIDEO_RECORD_TAG", "Recording video has errors");
            }

        }
        if(requestCode == VIDEO_PICK_GALLERY_CODE){
            videoPath = data.getData();
            setupPlayer(videoPath);
            Log.i("VIDEO_PICK_GALLERY","Video is picked from path" +videoPath );


        }
    }

    private void stopImageProcessor(){
        if(imageProcessor != null){
            imageProcessor.stop();
            imageProcessor = null;
            processing = false;
            pending = false;
        }
    }

}