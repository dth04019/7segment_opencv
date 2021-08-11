package com.example.opencvexample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        Button mBtnCamera = (Button)findViewById(R.id.camera);

        mBtnCamera.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent camera_recognize  = new Intent(MainActivity.this, camera_recognize.class);

                startActivity(camera_recognize);
            }
        });
    }

}