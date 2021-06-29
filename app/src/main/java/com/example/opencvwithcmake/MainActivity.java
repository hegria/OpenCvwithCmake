package com.example.opencvwithcmake;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.media.Image;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.Manifest.permission.CAMERA;


public class MainActivity extends AppCompatActivity
        implements  GLSurfaceView.Renderer{

    private static final String TAG = "opencv";
    private Mat matInput;
    private Mat matResult;

    private final Object frameImageInUseLock = new Object();

    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);


    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    GLSurfaceView glView; // 띄우기 위한 View

    boolean installRequested = false;
    Session session; // ??
    Camera camera; // 그냥 카메라

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 전체화면
        setContentView(R.layout.activity_main);
        glView = (GLSurfaceView) findViewById(R.id.surfaceView);
        glView.setPreserveEGLContextOnPause(true);
        glView.setEGLContextClientVersion(2);
        glView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glView.setRenderer(this);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        glView.setWillNotDraw(false);

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if(session != null){
            session.pause();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
        }

        if (session == null) {
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                // Create the session.
                session = new Session(/* context= */ this);

            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
            } catch (Exception e) {
                message = "Failed to create AR session";
            }

            if (message != null) {
                Toast.makeText(this, "TODO: handle exception " + message, Toast.LENGTH_LONG).show();
                return;
            }
        }

        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            Toast.makeText(this, "Camera not available. Try restarting the app.", Toast.LENGTH_LONG).show();
            session = null;
            return;
        }

        glView.onResume();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    public void onDestroy() {
        super.onDestroy();

    }



    //여기서부턴 퍼미션 관련 메소드
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;


    Background background;
    Cube cube;
    int width = 1, height = 1;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        background = new Background();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;

        GLES20.glViewport(0, 0, width, height);
    }

    long lastTime = SystemClock.elapsedRealtime();


    @Override
    public void onDrawFrame(GL10 gl) {

        if (session == null) return;

        synchronized (frameImageInUseLock){
        try {
            //필수
            session.setCameraTextureName(background.tex2id);
            session.setDisplayGeometry(((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation(), width, height);
            Frame frame = session.update();

            camera = frame.getCamera();
            // view matrix, projection matrix 받아오기
            float[] projMX = new float[16];
            camera.getProjectionMatrix(projMX, 0, 0.1f, 100.0f);
            float[] viewMX = new float[16];
            camera.getViewMatrix(viewMX, 0);


            // 그리기 전에 버퍼 초기화
            try (Image image = frame.acquireCameraImage()) {
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                // img to mat ->
                //image format을 신경을안썻고.


                matInput = new Mat(image.getHeight(),image.getWidth(),CvType.CV_8UC1,image.getPlanes()[0].getBuffer(),image.getPlanes()[0].getRowStride());

                Bitmap bmp = null;
                bmp = Bitmap.createBitmap(matInput.cols(), matInput.rows(), Bitmap.Config.ARGB_8888);
                Log.i("ass",Integer.toString(image.getWidth())+Integer.toString(image.getHeight())+Integer.toString(bmp.getWidth())+Integer.toString(bmp.getHeight()));
                Utils.matToBitmap(matInput,bmp);


                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,background.texID);
                /*
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                */

                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bmp,0);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0);

                long currentTime = SystemClock.elapsedRealtime();
                float dt = (float) (currentTime - lastTime) / 1000.0f;
                lastTime = currentTime;
                GLES20.glDisable(GLES20.GL_DEPTH_TEST);
                background.draw();

                GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            } catch (NotYetAvailableException e){

                //background.withoutdraw(frame);
                //??
            }



        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }



        }
    }
}