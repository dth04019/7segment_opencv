package com.example.opencvexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class second_method extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second_method);

        Button mBtnToResult = (Button)findViewById(R.id.to_finish);
        EditText editText = (EditText)findViewById(R.id.weight_value);
        mBtnToResult.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent to_main = new Intent(second_method.this, ResultActivity.class);
                to_main.putExtra("weight", editText.getText().toString());
                to_main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(to_main);
            }
        });
    }
}