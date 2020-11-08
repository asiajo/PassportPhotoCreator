package org.joanna.thesis.passportphotocreator.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public final class ImageUtils {

    private ImageUtils() {
    }

    public static void saveImage(
            final byte[] bytes,
            final Activity photoMakerActivity) {
        final Context context = photoMakerActivity.getApplicationContext();
        final String fileName = System.currentTimeMillis() + ".jpg";
        OutputStream fos = null;
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                fos = getImageOutputStreamSdkLessThanQ(fileName, context);
            } else {
                fos = getImageOutputStreamSdkFromQ(fileName, context);
            }
            fos.write(bytes);
            fos.flush();
            fos.close();
            Objects.requireNonNull(fos).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (fos != null) {
            Toast.makeText(photoMakerActivity, "Image Saved!",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(photoMakerActivity, "ERROR! Image Could not be " +
                    "Saved.", Toast.LENGTH_SHORT).show();
        }
    }

    private static OutputStream getImageOutputStreamSdkLessThanQ(
            final String fileName, final Context context)
            throws FileNotFoundException {
        File imagesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM);
        File image = new File(imagesDir, fileName);
        Uri imageUri = Uri.fromFile(image);
        Intent mediaScanIntent =
                new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        context.sendBroadcast(mediaScanIntent);
        return new FileOutputStream(image);
    }

    private static OutputStream getImageOutputStreamSdkFromQ(
            final String fileName, final Context context)
            throws FileNotFoundException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(
                MediaStore.Images.ImageColumns.DISPLAY_NAME,
                fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
        contentValues.put(
                MediaStore.Images.ImageColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DCIM);
        contentValues.put(
                MediaStore.MediaColumns.DATE_TAKEN,
                System.currentTimeMillis());
        Uri imageUri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues);
        return resolver.openOutputStream(Objects.requireNonNull(imageUri));
    }
}
