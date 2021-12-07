package com.example.aigolfcoach;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

public class DisplayHistoryActivity extends AppCompatActivity {

    private Button button;
    private RecyclerView videosRv;

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
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DisplayHistoryActivity.this, MainActivity.class);
                startActivity(intent);

            }
        });
    }

}