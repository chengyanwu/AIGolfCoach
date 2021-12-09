package com.example.aigolfcoach;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aigolfcoach.gles.GlPlayerRenderer;
import com.example.aigolfcoach.gles.GlPlayerView;
import com.example.aigolfcoach.preference.PreferenceUtils;
import com.example.aigolfcoach.preview.VisionProcessorBase;
import com.example.aigolfcoach.preview.posedetector.PoseDetectorProcessor;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HistoryVideoAdapter extends RecyclerView.Adapter<HistoryVideoAdapter.VideoHolder> implements GlPlayerRenderer.FrameListener{

    private String TAG = "HistoryVideoAdapter";
    private Context context;
    private ArrayList<HistoryVideo> videoArrayList;

    private GlPlayerView glPlayerView;
    private GraphicOverlay graphicOverlay;

    VisionProcessorBase imageProcessor;
    private boolean graphicOn = true;

    int frameWidth, frameHeight;
    boolean processing;
    boolean pending;
    private FrameLayout contentFrame;
    Bitmap lastFrame;

    String selectedFunction = "Spine Tracking";

    public HistoryVideoAdapter(Context context, ArrayList<HistoryVideo> videoArrayList){
        this.context = context;
        this.videoArrayList = videoArrayList;
    }

    @NonNull
    @Override
    public VideoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        // inflate layout row_video.xml
        View view = LayoutInflater.from(context).inflate(R.layout.row_video, parent, false);
        return new VideoHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoHolder holder, int position) {
        Log.i(TAG, "onBindViewHolder started");
        HistoryVideo historyVideo = videoArrayList.get(position);

        String id = historyVideo.getId();
//        String title = historyVideo.getTitle();
        String timestamp = historyVideo.getTimestamp();
        String videoUrl = historyVideo.getVideoUrl();

        // format timestamp
        Calendar calender = Calendar.getInstance();
        calender.setTimeInMillis(Long.parseLong(timestamp));
        String formattedDateTime = DateFormat.format("dd/MM/yyyy k:mm a", calender).toString();

        //set data
        holder.timetv.setText(formattedDateTime);
        setVideoUrl(historyVideo, holder);

        // handle delete video click
        holder.deleteFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // show alert dialog, confirm to delete
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Delete").setMessage("Delete Video from History?");
                builder.setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // confirm delete

                        deleteVideo(historyVideo);
                        videoArrayList.remove(position);
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // cancel delete
                        dialogInterface.dismiss();
                    }
                })
                .show();

            }
        });

        createImageProcessor();
        addVideoAnalysis(historyVideo, holder);
        if (lastFrame != null) processFrame(lastFrame);
        Log.i(TAG, "onBindViewHolder ended");

    }

    private void setVideoUrl(HistoryVideo historyVideo, VideoHolder holder){
        // show progress
        holder.progressBar.setVisibility(View.VISIBLE);

        // get video Url
        String videoUrl = historyVideo.getVideoUrl();

        try {
            DefaultTrackSelector trackSelector = new DefaultTrackSelector(context);
            holder.player = new SimpleExoPlayer.Builder(context)
                            .setTrackSelector(trackSelector)
                            .build();
            trackSelector.setParameters(
                    trackSelector
                            .buildUponParameters()
                            .setAllowVideoMixedMimeTypeAdaptiveness(true));

            // parsing uri from string
            Uri videoUri = Uri.parse(videoUrl);

            // set up playerview
            holder.playerView.setPlayer(holder.player);

            // set up player
            MediaItem mediaItem = MediaItem.fromUri(videoUri);
            holder.player.setMediaItem(mediaItem);
            holder.player.prepare();
            holder.player.setPlayWhenReady(true);

            // add listender to handle events
            holder.player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    switch(state){
                        case Player.STATE_BUFFERING:{
                            Log.i(TAG, "PLAYER_STATE: STATE_BUFFERING");
                            holder.progressBar.setVisibility(View.VISIBLE);
                        }
                        case Player.STATE_READY:{
                            Log.i(TAG, "PLAYER_STATE: STATE_READY");
                            holder.progressBar.setVisibility(View.GONE);
                        }
                    }
                }
            });

        } catch (Exception e) {
            // below line is used for
            // handling our errors.
            Log.e("TAG", "Error : " + e.toString());
        }


    }

    private void deleteVideo(HistoryVideo historyVideo){
        String videoUrl = historyVideo.getVideoUrl();
        String videoTimestamp = historyVideo.timestamp;

        // delete from firebase storage
        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(videoUrl);
        storageReference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(@NonNull Void unused) {
                        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Videos");
                        databaseReference.child(videoTimestamp)
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(@NonNull Void unused) {
                                        Toast.makeText(context, "Video Deleted", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "Failed to Delete Video", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addVideoAnalysis(HistoryVideo historyVideo, VideoHolder holder) {
        contentFrame = holder.playerView.findViewById(R.id.exo_content_frame);
        glPlayerView = new GlPlayerView(this.context);
        glPlayerView.setSimpleExoPlayer(holder.player);
        glPlayerView.setFrameListener(this);
        View videoFrameView = glPlayerView;

        if(videoFrameView != null) contentFrame.addView(videoFrameView);

        graphicOverlay = new GraphicOverlay(this.context, null);
        contentFrame.addView(graphicOverlay);
    }

    @Override
    public void onFrame(Bitmap bitmap) {
        processFrame(bitmap);
    }

    protected void createImageProcessor(){
        PoseDetectorOptionsBase poseDetectorOptions =
                PreferenceUtils.getPoseDetectorOptionsForLivePreview(this.context);
        boolean shouldShowInFrameLikelihood = false;
        boolean visualizeZ = false;
        boolean rescaleZ = PreferenceUtils.shouldPoseDetectionRescaleZForVisualization(this.context);
        boolean runClassification = PreferenceUtils.shouldPoseDetectionRunClassification(this.context);
        imageProcessor =
                new PoseDetectorProcessor(
                        this.context,
                        poseDetectorOptions,
                        selectedFunction,
                        shouldShowInFrameLikelihood,
                        visualizeZ,
                        rescaleZ,
                        runClassification,
                        /* isStreamMode = */ true);
    }

    protected void processFrame(Bitmap frame){
        Log.i(TAG, "processing frame");
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
                        if(pending) processFrame(lastFrame);
                    }
                });
                imageProcessor.processBitmap(frame, graphicOverlay);
            }
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


    @Override
    public int getItemCount(){
        return videoArrayList.size();
    }

    // View Holder class, holds, inits the UI View
    class VideoHolder extends RecyclerView.ViewHolder{
        SimpleExoPlayer player;
        // UI Views of row_video.xml
//        VideoView videoView;
        PlayerView playerView;

        TextView titletv, timetv;
        ProgressBar progressBar;
        FloatingActionButton deleteFab;

        public VideoHolder(@NonNull View itemView){
            super(itemView);

            // init UI Views of row_video.xml
            playerView = itemView.findViewById(R.id.history_view);
            player = new SimpleExoPlayer.Builder(HistoryVideoAdapter.this.context).build();
//            playerView.setPlayer(player);
//            titletv = itemView.findViewById(R.id.titleTv);
            timetv = itemView.findViewById(R.id.timeTv);
            progressBar = itemView.findViewById(R.id.progressBar);

            deleteFab = itemView.findViewById(R.id.deleteFab);
        }

    }
}

class HistoryVideo{
    String id, timestamp, videoUrl;
//    String title;
    public HistoryVideo(){
        // Firebase requires empty constructor
    }

    public HistoryVideo(String id, String timestamp, String videoUrl){
        this.id = id;
        this.timestamp = timestamp;
        this.videoUrl = videoUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

//    public String getTitle() {
//        return title;
//    }
//
//    public void setTitle(String title) {
//        this.title = title;
//    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}



