package org.joanna.thesis.passportphotocreator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int      RC_HANDLE_CAMERA_PERM = 2;
    private static final String[] PERMISSIONS_CAMERA    =
            {Manifest.permission.CAMERA};
    private              TextView statusMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusMessage = findViewById(R.id.status_message);
        findViewById(R.id.take_photo).setOnClickListener(this);
        requestCameraPermission();
        // TODO: make it nicer
    }

    @Override
    public void onClick(final View v) {
        if (v.getId() == R.id.take_photo) {
            int rc = ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA);
            if (rc == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(this, PhotoMakerActivity.class);
                startActivity(intent);
            } else {
                requestCameraPermission();
            }
        }
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(
                this,
                PERMISSIONS_CAMERA,
                RC_HANDLE_CAMERA_PERM);
    }
}