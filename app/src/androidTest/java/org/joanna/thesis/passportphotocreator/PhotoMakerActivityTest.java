package org.joanna.thesis.passportphotocreator;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.vision.face.FaceDetector;

import org.joanna.thesis.passportphotocreator.processing.background.enhancement.BackgroundEnhancement;
import org.joanna.thesis.passportphotocreator.processing.light.enhancement.ShadowRemoverPix2Pix;
import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.joanna.thesis.passportphotocreator.processing.light.ShadowUtils.isEvenlyLightened;
import static org.joanna.thesis.passportphotocreator.utils.ImageUtils.safelyRemoveBitmap;
import static org.joanna.thesis.passportphotocreator.utils.TestUtils.getBitmapFromFile;
import static org.joanna.thesis.passportphotocreator.utils.TestUtils.getFaceMatFromFileIfCorrectSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PhotoMakerActivityTest {

    private Activity mActivity;
    private BackgroundEnhancement backgroundEnhancer = null;
    private ShadowRemoverPix2Pix shadowEnhancer      = null;
    private Context context                          = null;
    private FaceDetector mDetectorPhotoT             = null;

    @Before
    public void initActivity() throws IOException {
        Instrumentation instrumentation =
                InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor =
                new Instrumentation.ActivityMonitor(
                        PhotoMakerActivity.class.getName(), null, false);
        instrumentation.addMonitor(monitor);
        Intent intent = new Intent(
                instrumentation.getTargetContext(),
                PhotoMakerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        instrumentation.startActivitySync(intent);
        mActivity = instrumentation.waitForMonitor(monitor);
        assertNotNull(mActivity);
        backgroundEnhancer = new BackgroundEnhancement(mActivity);
        shadowEnhancer = new ShadowRemoverPix2Pix(mActivity);
        context = InstrumentationRegistry.getInstrumentation().getContext();
        mDetectorPhotoT = new com.google.android.gms.vision
                .face.FaceDetector.Builder(
                mActivity.getApplication().getApplicationContext())
                .setProminentFaceOnly(true)
                .setMode(com.google.android.gms.vision
                        .face.FaceDetector.ACCURATE_MODE)
                .build();
    }

    @Test
    public void testGetFaceWithFace() throws IOException {
        Mat img = getFaceMatFromFileIfCorrectSize("face.jpg", context,
                mDetectorPhotoT);
        assertNotNull(img);
    }

    @Test
    public void testGetFaceWithoutFace() throws IOException {
        Mat img = getFaceMatFromFileIfCorrectSize("flowers.jpg", context,
                mDetectorPhotoT);
        assertNull(img);
    }

    // TODO: make it parametrized
    @Test
    public void testShadowIsCorrectlyEnhanced() throws IOException {
        List<String> files = Arrays.asList(
                "shadow1.jpg",
                "shadow2.jpg");

        if (shadowEnhancer == null) {
            shadowEnhancer = new ShadowRemoverPix2Pix(mActivity);
        }

        for (String file : files) {
            Bitmap bitmap = getBitmapFromFile(file, context);
            Mat picture = new Mat();
            Utils.bitmapToMat(bitmap, picture);
            final Mat resized = ImageUtils.resizeMatToFinalSize(picture);
            assertFalse(shadowEnhancer.verify(resized));
            Mat enhanced = shadowEnhancer.enhance(resized);
            assertTrue(shadowEnhancer.verify(enhanced));
            safelyRemoveBitmap(bitmap);
        }
    }

    // TODO: make it parametrized
    @Test
    public void testIsNotEvenlyLightened() throws IOException {
        List<String> files = Arrays.asList(
                "shadow1.jpg",
                "shadow2.jpg");

        for (String file : files) {
            Bitmap bitmap = getBitmapFromFile(file, context);
            Mat picture = new Mat();
            Utils.bitmapToMat(bitmap, picture);
            assertFalse(isEvenlyLightened(picture));
            safelyRemoveBitmap(bitmap);
        }
    }

    // TODO: make it parametrized
    @Test
    public void testIsEvenlyLightened() throws IOException {
        List<String> files = Arrays.asList(
                "light1.png",
                "light2.png",
                "light3.png");

        for (String file : files) {
            Bitmap bitmap = getBitmapFromFile(file, context);
            Mat picture = new Mat();
            Utils.bitmapToMat(bitmap, picture);
            assertTrue(isEvenlyLightened(picture));
            safelyRemoveBitmap(bitmap);
        }
    }

    // TODO: make it parametrized
    @Test
    public void testBackgroundIsCorrectlyEnhanced() throws IOException {
        List<String> files = Arrays.asList(
                "background1.jpg",
                "background2.jpg");

        for (String file : files) {
            Bitmap bitmap = getBitmapFromFile(file, context);
            Mat picture = new Mat();
            Utils.bitmapToMat(bitmap, picture);
            final Mat resized = ImageUtils.resizeMatToFinalSize(picture);
            assertFalse(backgroundEnhancer.verify(resized));
            Mat enhanced = backgroundEnhancer.enhance(resized);
            assertTrue(backgroundEnhancer.verify(enhanced));
            safelyRemoveBitmap(bitmap);
        }
    }

    @After
    public void tearDown() {
        if (backgroundEnhancer != null) {
            backgroundEnhancer.close();
        }
        if (shadowEnhancer != null) {
            shadowEnhancer.close();
        }
    }
}


