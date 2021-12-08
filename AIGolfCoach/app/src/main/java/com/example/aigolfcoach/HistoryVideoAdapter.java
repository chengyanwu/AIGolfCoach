package com.example.aigolfcoach;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ui.PlayerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HistoryVideoAdapter extends RecyclerView.Adapter<HistoryVideoAdapter.VideoHolder> {

    private String TAG = "HistoryVideoAdapter";
    private Context context;
    private ArrayList<HistoryVideo> videoArrayList;

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
    }

    private void setVideoUrl(HistoryVideo historyVideo, VideoHolder holder){
        // show progress
        holder.progressBar.setVisibility(View.VISIBLE);

        // get video Url
        String videoUrl = historyVideo.getVideoUrl();

        // Media controller for play, pause, seekbar, timer etc
        MediaController mediaController = new MediaController(context);
        mediaController.setAnchorView(holder.videoView);

        Uri videoUri = Uri.parse(videoUrl);
        holder.videoView.setMediaController(mediaController);
        holder.videoView.setVideoURI(videoUri);

        holder.videoView.requestFocus();
        holder.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                // video is ready to play
                mp.start();

            }
        });

        holder.videoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                // to check if buffering, rendering etc
                switch(what){
                    case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:{
                        Log.i(TAG, "RENDERING STARTS");
                        // rendering started
                        holder.progressBar.setVisibility(View.VISIBLE);
                        return true;
                    }
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:{
                        Log.i(TAG, "BUFFERING STARTS");
                        // buffering starts
                        holder.progressBar.setVisibility(View.VISIBLE);
                        return true;
                    }
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:{
                        Log.i(TAG, "BUFFERING ENDS");
                        holder.progressBar.setVisibility(View.GONE);
                        return true;
                    }
                }
                return false;
            }
        });

        holder.videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.start(); //restart video if completed
            }
        });
    }

    @Override
    public int getItemCount(){
        return videoArrayList.size();
    }



    // View Holder class, holds, units the UI View
    class VideoHolder extends RecyclerView.ViewHolder{
        // UI Views of row_video.xml
        VideoView videoView;
//        PlayerView playerView;

        TextView titletv, timetv;
        ProgressBar progressBar;

        public VideoHolder(@NonNull View itemView){
            super(itemView);

            // init UI Views of row_video.xml
            videoView = itemView.findViewById(R.id.history_view);
//            titletv = itemView.findViewById(R.id.titleTv);
            timetv = itemView.findViewById(R.id.timeTv);
            progressBar = itemView.findViewById(R.id.progressBar);
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


