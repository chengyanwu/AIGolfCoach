package com.example.aigolfcoach;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class DisplayHistoryActivity extends AppCompatActivity {

    private Button button;
    private RecyclerView recyclerView;

    private ArrayList<HistoryVideo> videoArrayList;
    private HistoryVideoAdapter historyVideoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_history);

        // Set action bar title
        setTitle("history");

        // Init UI
        button = this.findViewById(R.id.button2);
        recyclerView = this.findViewById(R.id.recyclerView);

        loadVideoFromFirebase();


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DisplayHistoryActivity.this, MainActivity.class);
                startActivity(intent);

            }
        });
    }

    private void loadVideoFromFirebase(){
        // Init arraylist
        videoArrayList = new ArrayList<>();

        // database reference
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Videos");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // clear list before adding data into it
                for (DataSnapshot ds: snapshot.getChildren()){
                    // get data
                    HistoryVideo historyVideo = ds.getValue(HistoryVideo.class);
                    // add historyVideo into list
                    videoArrayList.add(historyVideo);
                }

                // set up adapter
                historyVideoAdapter = new HistoryVideoAdapter(DisplayHistoryActivity.this, videoArrayList);
                // set adapter to recyclerview
                recyclerView.setAdapter(historyVideoAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

}