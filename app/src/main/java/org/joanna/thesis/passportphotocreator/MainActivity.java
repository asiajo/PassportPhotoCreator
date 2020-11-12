package org.joanna.thesis.passportphotocreator;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener {

    private static final String   TAG = MainActivity.class.getSimpleName();
    private              TextView statusMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusMessage = findViewById(R.id.status_message);
        findViewById(R.id.take_photo).setOnClickListener(this);
    }

    @Override
    public void onClick(final View v) {
        if (v.getId() == R.id.take_photo) {
            Intent intent = new Intent(this, PhotoMakerActivity.class);
            startActivity(intent);
        }
    }
}