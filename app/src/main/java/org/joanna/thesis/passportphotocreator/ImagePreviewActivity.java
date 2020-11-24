package org.joanna.thesis.passportphotocreator;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.joanna.thesis.passportphotocreator.utils.ImageUtils;

import java.io.IOException;

public class ImagePreviewActivity extends AppCompatActivity
        implements View.OnClickListener {

    Bitmap image;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        findViewById(R.id.cancel).setOnClickListener(this);
        findViewById(R.id.save_photo).setOnClickListener(this);
        imageView = findViewById(R.id.view_photo);

        image = getIntent().getParcelableExtra("Image");
        imageView.setImageBitmap(image);
    }

    @Override
    public void onClick(final View v) {
        if (v.getId() == R.id.cancel) {
            finish();
        }
        if (v.getId() == R.id.save_photo) {
            try {
                ImageUtils.saveImage(image, ImagePreviewActivity.this);
                Toast.makeText(
                        ImagePreviewActivity.this, R.string.image_saved,
                        Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(
                        ImagePreviewActivity.this, R.string.image_not_saved,
                        Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ImageUtils.safelyRemoveBitmap(image);
    }
}