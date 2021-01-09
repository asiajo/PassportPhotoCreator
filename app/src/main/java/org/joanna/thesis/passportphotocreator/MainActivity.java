package org.joanna.thesis.passportphotocreator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener {

    public static final  String   FILE_NAME             = "fileName";
    private static final String   TAG                   =
            MainActivity.class.getSimpleName();
    private static final int      PHOTO_REQUEST_CODE    = 0;
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
    }

    @Override
    public void onClick(final View v) {
        if (v.getId() == R.id.take_photo) {
            int rc = ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA);
            if (rc == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(this, PhotoMakerActivity.class);
                startActivityForResult(intent, PHOTO_REQUEST_CODE);
            } else {
                requestCameraPermission();
            }
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PHOTO_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                String picturePath =
                        Environment.DIRECTORY_DCIM + File.separator +
                                data.getStringExtra(FILE_NAME);

                statusMessage.setText(String.format(
                        getResources().getString(R.string.successfully_saved),
                        picturePath));
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