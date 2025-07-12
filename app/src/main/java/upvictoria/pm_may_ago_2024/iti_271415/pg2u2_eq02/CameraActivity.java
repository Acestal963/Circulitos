package upvictoria.pm_may_ago_2024.iti_271415.pg2u2_eq02;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final String TAG = "CameraActivity";
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private SurfaceHolder surfaceHolder;
    private ImageReader imageReader;
    private SurfaceView cameraSurfaceView;
    private ImageView processedImageView;
    private OverlayView overlayView;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private Mat lastProcessedMat;
    private boolean isSurfaceReady = false;
    private int frameCounter = 0;
    private static final int FRAME_SKIP = 1; // Process every frame
    private float viewWidth = 0;
    private float viewHeight = 0;
    private Size previewSize;
    private static final int DEFAULT_PREVIEW_WIDTH = 1280;
    private static final int DEFAULT_PREVIEW_HEIGHT = 720;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isAligned = false;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 5;
    private AtomicBoolean sheetDetected = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraSurfaceView = findViewById(R.id.cameraSurface);
        surfaceHolder = cameraSurfaceView.getHolder();
        processedImageView = findViewById(R.id.processedImage);
        overlayView = findViewById(R.id.overlay);

        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                isSurfaceReady = true;
                viewWidth = holder.getSurfaceFrame().width();
                viewHeight = holder.getSurfaceFrame().height();
                Log.d(TAG, "SurfaceView dimensions: " + viewWidth + "x" + viewHeight);
                setupGuideRectangle();
                if (cameraDevice != null) {
                    createCameraPreviewSession();
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                viewWidth = width;
                viewHeight = height;
                Log.d(TAG, "SurfaceView dimensions changed: " + viewWidth + "x" + viewHeight);
                setupGuideRectangle();
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                isSurfaceReady = false;
                Log.d(TAG, "Surface destroyed");
            }
        });

        Button btnConfig = findViewById(R.id.btnConfig);
        btnConfig.setOnClickListener(v -> {
            Intent intent = new Intent(this, ConfigActivity.class);
            startActivity(intent);
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startBackgroundThread();
            startCamera();
        }
    }

    private void setupGuideRectangle() {
        if (viewWidth == 0 || viewHeight == 0) {
            Log.w(TAG, "Cannot set guide rectangle: invalid view dimensions");
            return;
        }
        float guideWidth = viewWidth * 0.8f;
        float guideHeight = guideWidth * 1.414f; // A4 ratio
        if (guideHeight > viewHeight * 0.8f) {
            guideHeight = viewHeight * 0.8f;
            guideWidth = guideHeight / 1.414f;
        }
        float guideLeft = (viewWidth - guideWidth) / 2f;
        float guideTop = (viewHeight - guideHeight) / 2f;
        overlayView.setGuideRectangle(new android.graphics.RectF(guideLeft, guideTop, guideLeft + guideWidth, guideTop + guideHeight));
        Log.d(TAG, "Guide rectangle set: left=" + guideLeft + ", top=" + guideTop + ", width=" + guideWidth + ", height=" + guideHeight);
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        Log.d(TAG, "Background thread started");
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
                Log.d(TAG, "Background thread stopped");
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping background thread: " + e.getMessage());
            }
        }
    }

    private void startCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) {
            Log.e(TAG, "CameraManager is null");
            runOnUiThread(() -> Toast.makeText(this, "Error: No se puede acceder al servicio de cámara", Toast.LENGTH_LONG).show());
            return;
        }
        try {
            String cameraId = null;
            CameraCharacteristics characteristics = null;
            String[] cameraIdList = manager.getCameraIdList();
            Log.d(TAG, "Available camera IDs: " + Arrays.toString(cameraIdList));
            for (String id : cameraIdList) {
                try {
                    characteristics = manager.getCameraCharacteristics(id);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (facing != null && (facing == CameraCharacteristics.LENS_FACING_BACK || facing == CameraCharacteristics.LENS_FACING_FRONT)) {
                        cameraId = id;
                        Log.d(TAG, "Selected camera ID: " + cameraId + " (facing: " + (facing == CameraCharacteristics.LENS_FACING_BACK ? "back" : "front") + ")");
                        break;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Skipping invalid camera ID " + id + ": " + e.getMessage());
                }
            }
            if (cameraId == null) {
                Log.e(TAG, "No valid camera (back or front) found");
                runOnUiThread(() -> Toast.makeText(this, "No se encontró ninguna cámara válida", Toast.LENGTH_LONG).show());
                finish();
                return;
            }

            previewSize = chooseOptimalSize(characteristics, DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT);
            Log.d(TAG, "Selected preview size: " + previewSize.getWidth() + "x" + previewSize.getHeight());

            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 4);
            imageReader.setOnImageAvailableListener(reader -> processImage(reader), backgroundHandler);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Camera permission not granted");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    retryCount = 0; // Reset retry count on success
                    Log.d(TAG, "Camera opened successfully");
                    if (isSurfaceReady) {
                        createCameraPreviewSession();
                    } else {
                        Log.d(TAG, "Camera opened but surface not ready, waiting...");
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.w(TAG, "Camera disconnected");
                    cameraDevice.close();
                    cameraDevice = null;
                    runOnUiThread(() -> Toast.makeText(CameraActivity.this, "Cámara desconectada", Toast.LENGTH_SHORT).show());
                    releaseCameraAndRetry();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.e(TAG, "Camera error: " + error);
                    cameraDevice.close();
                    cameraDevice = null;
                    runOnUiThread(() -> Toast.makeText(CameraActivity.this, "Error al abrir la cámara: " + error, Toast.LENGTH_SHORT).show());
                    releaseCameraAndRetry();
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access error: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(this, "Error de acceso a la cámara: " + e.getMessage(), Toast.LENGTH_LONG).show());
            releaseCameraAndRetry();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error starting camera: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(this, "Error inesperado: " + e.getMessage(), Toast.LENGTH_LONG).show());
            releaseCameraAndRetry();
        }
    }

    private void releaseCameraAndRetry() {
        if (retryCount < MAX_RETRIES && !sheetDetected.get()) {
            retryCount++;
            // Attempt to release any lingering camera locks
            try {
                CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                if (manager != null) {
                    for (String id : manager.getCameraIdList()) {
                        manager.setTorchMode(id, false); // Disable torch to release
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to release camera: " + e.getMessage());
            }
            backgroundHandler.postDelayed(() -> {
                if (cameraDevice == null) {
                    Log.d(TAG, "Retrying camera... (Attempt " + retryCount + "/" + MAX_RETRIES + ")");
                    startCamera();
                }
            }, 2000); // 2-second delay
        } else if (!sheetDetected.get()) {
            Log.e(TAG, "Max retry attempts reached, giving up");
            runOnUiThread(() -> Toast.makeText(this, "No se pudo abrir la cámara después de " + MAX_RETRIES + " intentos", Toast.LENGTH_LONG).show());
        }
    }

    private Size chooseOptimalSize(CameraCharacteristics characteristics, int targetWidth, int targetHeight) {
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            Log.w(TAG, "StreamConfigurationMap is null, using default size");
            return new Size(targetWidth, targetHeight);
        }
        Size[] outputSizes = map.getOutputSizes(SurfaceHolder.class);
        float targetRatio = (float) targetWidth / targetHeight;
        Size optimalSize = null;
        long minDiff = Long.MAX_VALUE;

        for (Size size : outputSizes) {
            float ratio = (float) size.getWidth() / size.getHeight();
            long diff = Math.abs(size.getWidth() - targetWidth) + Math.abs(size.getHeight() - targetHeight);
            if (Math.abs(ratio - targetRatio) < 0.1 && diff < minDiff) {
                optimalSize = size;
                minDiff = diff;
            }
        }

        if (optimalSize == null) {
            Log.w(TAG, "No optimal size found, using default: " + targetWidth + "x" + targetHeight);
            return new Size(targetWidth, targetHeight);
        }
        Log.d(TAG, "Optimal size selected: " + optimalSize.getWidth() + "x" + optimalSize.getHeight());
        return optimalSize;
    }

    private void createCameraPreviewSession() {
        if (!isSurfaceReady || cameraDevice == null) {
            Log.w(TAG, "Cannot create preview session: surfaceReady=" + isSurfaceReady + ", cameraDevice=" + (cameraDevice != null));
            if (cameraDevice != null) {
                runOnUiThread(() -> Toast.makeText(this, "Esperando superficie lista...", Toast.LENGTH_SHORT).show());
            }
            return;
        }
        try {
            Surface surface = surfaceHolder.getSurface();
            if (surface == null) {
                Log.e(TAG, "Surface is null");
                runOnUiThread(() -> Toast.makeText(this, "Error: Superficie no disponible", Toast.LENGTH_SHORT).show());
                return;
            }
            List<Surface> surfaces = Arrays.asList(surface, imageReader.getSurface());

            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    captureSession = session;
                    try {
                        CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        captureRequestBuilder.addTarget(surface);
                        captureRequestBuilder.addTarget(imageReader.getSurface());
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        session.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
                        Log.d(TAG, "Camera preview session configured");
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Error configuring capture: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(CameraActivity.this, "Error al configurar la captura: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Camera preview session configuration failed");
                    runOnUiThread(() -> Toast.makeText(CameraActivity.this, "Error al configurar la sesión de captura", Toast.LENGTH_SHORT).show());
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error creating capture session: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(this, "Error al crear la sesión de captura: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void processImage(ImageReader reader) {
        if (sheetDetected.get()) return; // Stop processing if sheet is detected

        frameCounter++;
        if (frameCounter % FRAME_SKIP != 0) {
            Image image = reader.acquireLatestImage();
            if (image != null) image.close();
            return;
        }

        Image image = reader.acquireLatestImage();
        if (image == null) {
            Log.d(TAG, "No image available from ImageReader");
            return;
        }

        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            image.close();

            executor.execute(() -> {
                Bitmap bitmap = null;
                try {
                    Log.d(TAG, "Processing image: " + previewSize.getWidth() + "x" + previewSize.getHeight());
                    Mat rgbMat = convertYUVToRGB(bytes, previewSize.getWidth(), previewSize.getHeight());
                    if (rgbMat.empty()) {
                        Log.e(TAG, "RGB Mat is empty after YUV conversion");
                        rgbMat.release();
                        return;
                    }
                    bitmap = Bitmap.createBitmap(previewSize.getWidth(), previewSize.getHeight(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(rgbMat, bitmap);
                    rgbMat.release();

                    SheetDetector.SheetDetectionResult detection = SheetDetector.detectSheet(bitmap);
                    Point[] previewCorners = detection != null ? detection.corners : null;
                    String errorMessage = detection != null ? detection.errorMessage : "No se detectó la hoja";

                    Bitmap displayBitmap = bitmap.copy(bitmap.getConfig(), true); // Clonar el Bitmap
                    Bitmap finalBitmap = bitmap;
                    runOnUiThread(() -> {
                        processedImageView.setVisibility(View.GONE);
                        overlayView.setPreviewCorners(previewCorners, previewSize.getWidth(), previewSize.getHeight());
                        if (detection == null || previewCorners == null || previewCorners.length != 4) {
                            if (displayBitmap != null && !displayBitmap.isRecycled()) {
                                processedImageView.setImageBitmap(displayBitmap);
                                processedImageView.setVisibility(View.VISIBLE);
                                Toast.makeText(CameraActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                            isAligned = false;
                            if (finalBitmap != null && !finalBitmap.isRecycled()) finalBitmap.recycle(); // Reciclar original
                            return;
                        }

                        if (!isAligned) {
                            Point[] sortedCorners = sortCorners(previewCorners);
                            double width = Math.hypot(sortedCorners[1].x - sortedCorners[0].x, sortedCorners[1].y - sortedCorners[0].y);
                            double height = Math.hypot(sortedCorners[2].x - sortedCorners[1].x, sortedCorners[2].y - sortedCorners[1].y);
                            double ratio = width / height;
                            if (Math.abs(ratio - 1.414) < 0.2) {
                                isAligned = true;
                                Toast.makeText(CameraActivity.this, "Hoja alineada", Toast.LENGTH_SHORT).show();
                            }
                        }

                        if (isAligned) {
                            lastProcessedMat = UtilsPerspective.fourPointTransform(detection.image, previewCorners, viewWidth, viewHeight);
                            detection.image.release();

                            Bitmap processedBitmap = Bitmap.createBitmap(lastProcessedMat.cols(), lastProcessedMat.rows(), Bitmap.Config.ARGB_8888);
                            Utils.matToBitmap(lastProcessedMat, processedBitmap);

                            int hojaWidth = lastProcessedMat.cols();
                            int hojaHeight = lastProcessedMat.rows();
                            Log.d(TAG, "Transformed image: " + hojaWidth + "x" + hojaHeight);

                            Point[] transformedCorners = new Point[]{
                                    new Point(0, 0),
                                    new Point(hojaWidth - 1, 0),
                                    new Point(hojaWidth - 1, hojaHeight - 1),
                                    new Point(0, hojaHeight - 1)
                            };

                            Bitmap displayProcessedBitmap = processedBitmap.copy(processedBitmap.getConfig(), true); // Clonar
                            processedImageView.setImageBitmap(displayProcessedBitmap);
                            processedImageView.setVisibility(View.VISIBLE);
                            overlayView.setHojaBorde(transformedCorners);

                            try {
                                SharedPreferences prefs = getSharedPreferences("ZONES", MODE_PRIVATE);
                                String json = prefs.getString("zonas_config", "");
                                if (json.trim().isEmpty()) {
                                    overlayView.clear();
                                    return;
                                }

                                JSONArray zonaArray = new JSONArray(json);
                                if (zonaArray.length() == 0) {
                                    overlayView.clear();
                                    return;
                                }

                                overlayView.clear();
                                int zonaTotal = zonaArray.length();
                                int zonaIndex = 0;

                                if (zonaTotal < 4) {
                                    int zonaHeight = hojaHeight / zonaTotal;
                                    int zonaWidth = hojaWidth;

                                    for (int i = 0; i < zonaTotal; i++) {
                                        JSONObject zona = zonaArray.getJSONObject(i);
                                        int expected = zona.getInt("expectedCount");

                                        int x = 0;
                                        int y = i * zonaHeight;

                                        Rect rect = new Rect(x, y, zonaWidth, zonaHeight);
                                        Mat zonaMat = new Mat(lastProcessedMat, rect);

                                        ArrayList<Point3> circulos = CircleDetector.detect(zonaMat, expected);
                                        zonaMat.release();

                                        overlayView.addZona(new android.graphics.RectF(x, y, x + zonaWidth, y + zonaHeight),
                                                "Zona " + (i + 1) + ": " + circulos.size() + "/" + expected);

                                        for (Point3 c : circulos) {
                                            overlayView.addCirculo((float) c.x + x, (float) c.y + y, (float) c.z);
                                        }
                                    }
                                } else {
                                    int cols = (int) Math.ceil(Math.sqrt(zonaTotal));
                                    int rows = (int) Math.ceil((double) zonaTotal / cols);
                                    int zonaWidth = hojaWidth / cols;
                                    int zonaHeight = hojaHeight / rows;

                                    for (int row = 0; row < rows && zonaIndex < zonaTotal; row++) {
                                        for (int col = 0; col < cols && zonaIndex < zonaTotal; col++) {
                                            JSONObject zona = zonaArray.getJSONObject(zonaIndex);
                                            int expected = zona.getInt("expectedCount");

                                            int x = col * zonaWidth;
                                            int y = row * zonaHeight;

                                            Rect rect = new Rect(x, y, zonaWidth, zonaHeight);
                                            Mat zonaMat = new Mat(lastProcessedMat, rect);

                                            ArrayList<Point3> circulos = CircleDetector.detect(zonaMat, expected);
                                            zonaMat.release();

                                            overlayView.addZona(new android.graphics.RectF(x, y, x + zonaWidth, y + zonaHeight),
                                                    "Zona " + (zonaIndex + 1) + ": " + circulos.size() + "/" + expected);

                                            for (Point3 c : circulos) {
                                                overlayView.addCirculo((float) c.x + x, (float) c.y + y, (float) c.z);
                                            }

                                            zonaIndex++;
                                        }
                                    }
                                }

                                overlayView.invalidate();

                                float imageWidth = lastProcessedMat.cols();
                                float imageHeight = lastProcessedMat.rows();
                                float scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);
                                float offsetX = (viewWidth - imageWidth * scale) / 2f;
                                float offsetY = (viewHeight - imageHeight * scale) / 2f;
                                Log.d(TAG, "Scale: " + scale + ", OffsetX: " + offsetX + ", OffsetY=" + offsetY);

                                overlayView.setTransformParams(scale, offsetX, offsetY);
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing zones: " + e.getMessage());
                                runOnUiThread(() -> Toast.makeText(CameraActivity.this, "Error en procesamiento: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            }

                            sheetDetected.set(true);
                            runOnUiThread(() -> Toast.makeText(CameraActivity.this, "Hoja detectada y procesada", Toast.LENGTH_LONG).show());
                            if (captureSession != null) {
                                try {
                                    captureSession.stopRepeating();
                                    captureSession.close();
                                    captureSession = null;
                                } catch (CameraAccessException e) {
                                    Log.e(TAG, "Error closing capture session: " + e.getMessage());
                                }
                            }
                            if (cameraDevice != null) {
                                cameraDevice.close();
                                cameraDevice = null;
                            }
                        }
                        if (finalBitmap != null && !finalBitmap.isRecycled()) finalBitmap.recycle(); // Reciclar solo después de uso
                      });
                } catch (Exception e) {
                    Log.e(TAG, "Error in image processing: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error acquiring image: " + e.getMessage());
            if (image != null) image.close();
        }
    }


    private Point[] sortCorners(Point[] corners) {
        Point[] sorted = new Point[4];
        double[] sums = new double[4];
        for (int i = 0; i < 4; i++) {
            sums[i] = corners[i].x + corners[i].y;
        }
        sorted[0] = corners[sums[0] < sums[1] ? 0 : 1];
        sorted[2] = corners[sums[2] > sums[3] ? 2 : 3];
        sorted[1] = corners[sums[0] > sums[1] ? 0 : 1];
        sorted[3] = corners[sums[2] < sums[3] ? 2 : 3];
        return sorted;
    }

    private Mat convertYUVToRGB(byte[] yuvData, int width, int height) {
        Mat yuv = new Mat(height + height / 2, width, org.opencv.core.CvType.CV_8UC1);
        yuv.put(0, 0, yuvData);
        Mat rgb = new Mat();
        org.opencv.imgproc.Imgproc.cvtColor(yuv, rgb, org.opencv.imgproc.Imgproc.COLOR_YUV2RGB_NV21);
        yuv.release();
        return rgb;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBackgroundThread();
                startCamera();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (backgroundHandler != null) {
            backgroundHandler.post(() -> {
                if (captureSession != null) {
                    try {
                        captureSession.stopRepeating();
                        captureSession.close();
                        captureSession = null;
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Error closing capture session: " + e.getMessage());
                    }
                }
                if (cameraDevice != null) {
                    cameraDevice.close();
                    cameraDevice = null;
                }
            });
        }
        stopBackgroundThread();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startBackgroundThread();
            if (cameraDevice == null && !sheetDetected.get()) {
                startCamera();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (lastProcessedMat != null) {
            lastProcessedMat.release();
            lastProcessedMat = null;
        }
        executor.shutdown();
    }
}