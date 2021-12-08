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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HistoryVideoAdapter extends RecyclerView.Adapter<HistoryVideoAdapter.VideoHolder> {

    private String TAG = "HistoryVideoAdapter";
    private Context context;
    private ArrayList<HistoryVideo> videoArrayList;
    private Player.Listener listener;

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

        try {
            DefaultTrackSelector trackSelector = new DefaultTrackSelector(context);
            holder.player = new SimpleExoPlayer.Builder(context)
                            .setTrackSelector(trackSelector)
                            .build();
            trackSelector.setParameters(
                    trackSelector
                            .buildUponParameters()
                            .setAllowVideoMixedMimeTypeAdaptiveness(true));

            //  parsing uri from string
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
                            holder.progressBar.setVisibility(View.VISIBLE);
                        }
                        case Player.STATE_READY:{
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


    @Override
    public int getItemCount(){
        return videoArrayList.size();
    }



    // View Holder class, holds, units the UI View
    class VideoHolder extends RecyclerView.ViewHolder{
        SimpleExoPlayer player;
        // UI Views of row_video.xml
//        VideoView videoView;
        PlayerView playerView;

        TextView titletv, timetv;
        ProgressBar progressBar;

        public VideoHolder(@NonNull View itemView){
            super(itemView);

            // init UI Views of row_video.xml
            playerView = itemView.findViewById(R.id.history_view);
            player = new SimpleExoPlayer.Builder(HistoryVideoAdapter.this.context).build();
            playerView.setPlayer(player);
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



