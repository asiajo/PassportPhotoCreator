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
import android.util.Log;
import android.widget.Toast;

import org.joanna.thesis.passportphotocreator.R;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.detectors.face.FaceGraphic;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import static org.opencv.core.Core.BORDER_REPLICATE;

public final class ImageUtils {

    private static final String TAG = ImageUtils.class.getSimpleName();

    private static final float FINAL_IMAGE_WIDTH_FACTOR = 35f;
    private static final float FINAL_IMAGE_HEIGHT_FACTOR = 45f;
    public static final  float FINAL_IMAGE_H_TO_W_RATIO =
            FINAL_IMAGE_HEIGHT_FACTOR / FINAL_IMAGE_WIDTH_FACTOR;
    public static final  float FINAL_IMAGE_W_TO_H_RATIO =
            FINAL_IMAGE_WIDTH_FACTOR / FINAL_IMAGE_HEIGHT_FACTOR;
    /**
     * Size in pixels of the resulting image. 827 corresponds to 3,5 cm wide
     * image with the quality of 600 ppi.
     */
    private static final int   FINAL_IMAGE_WIDTH_PX = 827;
    private static final int   FINAL_IMAGE_HEIGHT_PX =
            (int) (FINAL_IMAGE_WIDTH_PX * FINAL_IMAGE_H_TO_W_RATIO);


    private ImageUtils() {
    }

    public static void saveImage(
            final byte[] bytes,
            final Activity photoMakerActivity,
            final GraphicOverlay<Graphic> mGraphicOverlay) throws IOException {

        Mat image = getMatFromBytes(bytes);
        image = cropMatToFaceBoundingBox(
                image, mGraphicOverlay);
        if (image == null) {
            Toast.makeText(photoMakerActivity, R.string.cannot_make_a_picture,
                           Toast.LENGTH_LONG).show();
            return;
        }
        image = resizeMat(
                image, FINAL_IMAGE_WIDTH_PX, FINAL_IMAGE_HEIGHT_PX);
        Bitmap imageCropped = getBitmapFromMat(image);
        image.release();
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

    public static Mat getMatFromBytes(final byte[] bytes) {
        Mat image = Imgcodecs.imdecode(
                new MatOfByte(bytes),
                Imgcodecs.IMREAD_COLOR);
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2RGBA);
        return image;
    }

    public static Mat getMatFromYuvBytes(
            final byte[] bytes, final int width, final int height) {
        int increasedHeight = height + (height / 2);
        Mat image = new Mat(increasedHeight, width, CvType.CV_8UC1);
        image.put(0, 0, bytes);
        Imgproc.cvtColor(image, image, Imgproc.COLOR_YUV2RGBA_NV21, 4);
        return rotateMat(image);
    }

    public static byte[] getBytesFromBitmap(final Bitmap src)
            throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        src.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        stream.flush();
        stream.close();
        return byteArray;
    }

    public static Bitmap getBitmapFromMat(final Mat src) {
        Bitmap bitmap = Bitmap.createBitmap(src.width(), src.height(),
                                            Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, bitmap);
        return bitmap;
    }

    public static Mat rotateMat(final Mat src) {
        Mat rotated = new Mat();
        Core.transpose(src, rotated);
        // TODO: double check if on all the phones the same rotation required
        Core.flip(rotated, rotated, 1);
        return rotated;
    }

    public static Mat resizeMat(final Mat src, final int width) {
        return resizeMat(src, width,
                         (int) (width * FINAL_IMAGE_H_TO_W_RATIO));
    }

    public static Mat resizeMat(
            final Mat src, final int width, final int height) {

        float croppedMatRatio = (float) src.height() / src.width();
        float requestedRatio = (float) height / width;
        float epsilon = 0.01f;
        if (Math.abs(croppedMatRatio - requestedRatio) > epsilon) {
            Log.w(TAG, String.format(
                    "Requested crop ratio: %d/%d=%.3f is different than the " +
                            "ratio of original image: %d/%d=%.3f. Image will " +
                            "get squeezed!",
                    height, width, requestedRatio,
                    src.height(), src.width(), croppedMatRatio));
        }
        Mat resized = new Mat();
        Size sz = new Size(width, height);
        Imgproc.resize(src, resized, sz);

        return resized;
    }

    public static Mat cropMatToFaceBoundingBox(
            final Mat src,
            final GraphicOverlay<Graphic> mGraphicOverlay) {
        FaceGraphic faceGraphic = null;
        for (Graphic item : mGraphicOverlay.getmGraphics()) {
            if (item instanceof FaceGraphic) {
                faceGraphic = (FaceGraphic) item;
            }
        }
        if (faceGraphic == null || faceGraphic.getFaceBoundingBox() == null) {
            return null;
        }

        double matWidth = src.size().width;
        double matHeight = src.size().height;

        int cutWidth = (int) (faceGraphic.getBbProportionWidth() * matWidth);
        int cutHeight = (int) (cutWidth * FINAL_IMAGE_H_TO_W_RATIO);
        int cutLeft = (int) (faceGraphic.getBbProportionLeft() * matWidth);
        int cutTop = (int) (faceGraphic.getBbProportionTop() * matHeight);
        int cutRight = cutLeft + cutWidth;
        int cutBottom = cutTop + cutHeight;
        if (!verifyBoundingBox(cutLeft, cutTop, cutRight, cutBottom,
                               src.size())) {
            return null;
        }
        Mat cropped = src.submat(cutTop, cutBottom, cutLeft, cutRight);
        return cropped;
    }

    public static Mat padMatToSquare(final Mat src, final int borderSize) {
        Mat dst = new Mat();
        int top = (borderSize - src.height()) / 2;
        int left = (borderSize - src.width()) / 2;
        int bottom = borderSize - top - src.height();
        int right = borderSize - left - src.width();
        Core.copyMakeBorder(src, dst, top, bottom, left, right,
                            BORDER_REPLICATE);
        return dst;
    }

    public static Mat unpadMatFromSquare(final Mat src, final int imgWidth) {
        int left = (src.width() - imgWidth) / 2;
        int right = left + imgWidth;
        Mat cropped = src.submat(0, src.height(), left, right);
        return cropped;
    }

    private static boolean verifyBoundingBox(
            final int cutLeft, final int cutTop, final int cutRight,
            final int cutBottom, final Size size) {
        return cutLeft >= 0
                && cutTop >= 0
                && cutRight > cutLeft
                && cutRight <= size.width
                && cutBottom <= size.height;
    }

    public static void safelyRemoveBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
        System.gc();
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
        // TODO: send info to gallery
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
