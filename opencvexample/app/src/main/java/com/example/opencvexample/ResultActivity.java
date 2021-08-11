package com.example.opencvexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        Button mFinishBtn = (Button) findViewById(R.id.to_main);

        Intent now = getIntent();
        String result_value = now.getStringExtra("weight");

        TextView weight_value_result = (TextView)findViewById(R.id.weight_value_result);
        weight_value_result.setText(result_value);


        mFinishBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent to_main = new Intent(ResultActivity.this, MainActivity.class);
                to_main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(to_main);
            }
        });
    }

}