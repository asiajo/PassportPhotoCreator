package org.joanna.thesis.passportphotocreator.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.detectors.face.FaceGraphic;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
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
            final Activity photoMakerActivity,
            final GraphicOverlay<Graphic> mGraphicOverlay) throws IOException {

        Mat croppedMat = getCroppedMat(bytes, mGraphicOverlay);
        if (croppedMat == null) {
            return;
        }
        Bitmap imageCropped = getBitmapFromMat(croppedMat);
        byte[] byteArray = getBytesFromBitmap(imageCropped);
        safelyRemoveBitmap(imageCropped);

        final Context context = photoMakerActivity.getApplicationContext();
        final String fileName = System.currentTimeMillis() + ".png";

        OutputStream fos;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            fos = getImageOutputStreamSdkLessThanQ(fileName, context);
        } else {
            fos = getImageOutputStreamSdkFromQ(fileName, context);
        }
        fos.write(byteArray);
        fos.flush();
        fos.close();
    }

    private static byte[] getBytesFromBitmap(final Bitmap imageCropped)
            throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        imageCropped.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        stream.flush();
        stream.close();
        return byteArray;
    }


    private static void safelyRemoveBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
        System.gc();
    }

    private static Bitmap getBitmapFromMat(final Mat image) {
        Bitmap map = Bitmap.createBitmap(image.width(), image.height(),
                Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, map);
        return map;
    }

    private static Mat getCroppedMat(
            final byte[] bytes,
            final GraphicOverlay<Graphic> mGraphicOverlay) {

        FaceGraphic faceGraphic = null;
        for (Graphic item : mGraphicOverlay.getmGraphics()) {
            if (item instanceof FaceGraphic) {
                faceGraphic = (FaceGraphic) item;
            }
        }
        if (faceGraphic == null) {
            return null;
        }

        Mat mat = Imgcodecs.imdecode(
                new MatOfByte(bytes),
                Imgcodecs.IMREAD_COLOR);

        double matWidth = mat.size().width;
        double matHeight = mat.size().height;
        int cutWidth = (int) (faceGraphic.getBbProportionWidth() * matWidth);
        int cutHeight = cutWidth * 45 / 35;
        int cutLeft = (int) (faceGraphic.getBbProportionLeft() * matWidth);
        int cutTop = (int) (faceGraphic.getBbProportionTop() * matHeight);
        int cutRight = cutLeft + cutWidth;
        int cutBottom = cutTop + cutHeight;

        Mat cropped = mat.submat(cutTop, cutBottom, cutLeft, cutRight);
        Mat rgba = new Mat();
        Imgproc.cvtColor(cropped, rgba, Imgproc.COLOR_BGR2RGBA);
        return rgba;
    }

    private static OutputStream getImageOutputStreamSdkLessThanQ(
            final String fileName, final Context context)
            throws FileNotFoundException {
        File imagesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM);
        File image = new File(imagesDir, fileName);
        broadcastImageInfoSdkLessThanQ(context, image);
        return new FileOutputStream(image);
    }

    private static void broadcastImageInfoSdkLessThanQ(
            final Context context, final File image) {
        Uri imageUri = Uri.fromFile(image);
        Intent mediaScanIntent =
                new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        context.sendBroadcast(mediaScanIntent);
    }

    private static OutputStream getImageOutputStreamSdkFromQ(
            final String fileName, final Context context)
            throws FileNotFoundException {
        // TODO: double check this
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, fileName);
        contentValues.put(
                MediaStore.Images.ImageColumns.DISPLAY_NAME,
                fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        contentValues.put(
                MediaStore.Images.ImageColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DCIM);
        contentValues.put(
                MediaStore.MediaColumns.DATE_TAKEN,
                System.currentTimeMillis());
        contentValues.put(
                MediaStore.Images.Media.DATE_ADDED,
                System.currentTimeMillis());
        contentValues.put(
                MediaStore.MediaColumns.DATE_MODIFIED,
                System.currentTimeMillis() / 1000l);
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, true);
        Uri imageUri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues);
        return resolver.openOutputStream(Objects.requireNonNull(imageUri));
    }
}
