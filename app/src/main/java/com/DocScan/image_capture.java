package com.DocScan;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class image_capture extends AppCompatActivity {
    datastore data = new datastore();
    private ImageView proceedtonext;
    private ImageView flash;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private boolean isTorchOn=false;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    public static final int PICK_IMAGE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private CaptureRequest captureRequest;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_capture);
        textureView =findViewById(R.id.textureView);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        ImageView takePictureButton = findViewById(R.id.shoot_photo);
        assert takePictureButton != null;
        takePictureButton.setOnClickListener(v -> takePicture());
        ImageView pick_from_Gallery = findViewById(R.id.select_image_from_gallery);
        pick_from_Gallery.setOnClickListener(view -> {
            if (!data.has_path_set()) {
                createDirectory();
            }
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
        });
        flash = findViewById(R.id.flash);
        flash.setOnClickListener(view -> {
            if (isTorchOn) {
                flash.setImageResource(R.drawable.do_flash_on);
                isTorchOn=false;
            } else {
                flash.setImageResource(R.drawable.do_flash_off);
                isTorchOn=true;
            }
        });
        proceedtonext= findViewById(R.id.done_capturing);
        proceedtonext.setBackgroundColor(ContextCompat.getColor(this,R.color.cardviewBackgroundcolor));
        proceedtonext.setEnabled(false);
        proceedtonext.setOnClickListener(view -> {
            Intent intent =new Intent(image_capture.this,captured_image_display.class);
            startActivity(intent);
            finish();
        });
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        }
    };
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            //This is called when the camera is open
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
            cameraDevice=null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice=null;
        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void takePicture() {
        if (null == cameraDevice) {
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = Objects.requireNonNull(characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(ImageFormat.JPEG);
            }
            //height and width of the output
            int width = 640;
            int height = 480;
            Size final_imageSize = getImagesize(jpegSizes);
            width = final_imageSize.getWidth();
            height = final_imageSize.getHeight();
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            //flash
            if (isTorchOn) {
                captureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            } else {
                captureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            }

            // Orientation
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(getrotation()));
            if (!data.has_path_set()) {
                createDirectory();
            }
            Date c = Calendar.getInstance().getTime();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String file_name_capture = "IMG_" + simpleDateFormat.format(c) + ".jpeg";
            final File file = new File(data.getImage_path() + "/" + file_name_capture);
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        MediaActionSound sound = new MediaActionSound();
                        sound.play(MediaActionSound.SHUTTER_CLICK);
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                        data.setFilename(file_name_capture);
                        rotateImage(bytes, file.getAbsolutePath());
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException | IllegalStateException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    try{
                        captureRequest=captureRequestBuilder.build();
                        cameraCaptureSessions = cameraCaptureSession;
                        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                        cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(image_capture.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            },mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(image_capture.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if(null!= cameraCaptureSessions){
            cameraCaptureSessions.close();
            cameraCaptureSessions=null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(image_capture.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCamera();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @TargetApi(Build.VERSION_CODES.R)
    public int getrotation(){
        int rotation;
        if(Build.VERSION.SDK_INT<=Build.VERSION_CODES.R){
            rotation=getWindowManager().getDefaultDisplay().getRotation();
        }else{
            rotation= Objects.requireNonNull(this.getDisplay()).getRotation();
        }
        return rotation;
    }
    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
        stopBackgroundThread();
    }
    @TargetApi(Build.VERSION_CODES.Q)
    public Size above_30(){
        final WindowMetrics metrics = getWindowManager().getCurrentWindowMetrics();
        // Gets all excluding insets
        final WindowInsets windowInsets = metrics.getWindowInsets();
        Insets insets = windowInsets.getInsets(WindowInsets.Type.navigationBars());
        final DisplayCutout cutout = windowInsets.getDisplayCutout();
        if (cutout != null) {
            final Insets cutoutSafeInsets = Insets.of(cutout.getBoundingRectLeft());
            insets = Insets.max(insets, cutoutSafeInsets);
        }

        int insetsWidth = insets.right + insets.left;
        int insetsHeight = insets.top + insets.bottom;

        // Legacy size that Display#getSize reports
        return new Size(metrics.getBounds().width()-insetsWidth, metrics.getBounds().height() - insetsHeight);
    }
    public Size getImagesize(Size[] jpegSizes) {
        Point size=new Point();
        Size lsize;
        if(Build.VERSION.SDK_INT<=Build.VERSION_CODES.R){
            Display display = getWindowManager().getDefaultDisplay();
            display.getSize(size);
        }else{
            lsize=above_30();
            size.set(lsize.getHeight(),lsize.getWidth());
        }
        int screenLength = size.x;
        if (screenLength < size.y) {
            screenLength = size.y;
        }

        int index = 0;
        int finalIndex = 0;
        int finalJpegLength = Integer.MAX_VALUE;
        for (Size jpegSize : jpegSizes) {
            int jpegLength = jpegSize.getWidth();
            if (jpegLength < jpegSize.getHeight()) {
                jpegLength = jpegSize.getHeight();
            }
            if (jpegLength >= screenLength) {
                if (jpegLength < finalJpegLength) {
                    finalJpegLength = jpegLength;
                    finalIndex = index;
                }
            }
            index++;
        }

        return new Size(jpegSizes[finalIndex].getWidth(), jpegSizes[finalIndex].getHeight());
    }

    public void createDirectory() {
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String folder_name = simpleDateFormat.format(c);
        final File file = new File(getExternalFilesDir(null) + "/Pictures/" + folder_name);
        if (!file.exists()) {
            file.mkdirs();
        }
        data.setImage_Path(file.getAbsolutePath(),folder_name);
    }

    public void rotateImage(byte[] data, String filename) {
        File photo_file = new File(filename);
        if (photo_file.exists()) {
            photo_file.delete();
        }
        try {
            FileOutputStream fos = new FileOutputStream(photo_file);

            Bitmap realImage = BitmapFactory.decodeByteArray(data, 0, data.length);

            ExifInterface exif = new ExifInterface(photo_file.toString());
            if (Objects.requireNonNull(exif.getAttribute(ExifInterface.TAG_ORIENTATION)).equalsIgnoreCase("6")) {
                realImage = rotate(realImage, 90);
            } else if (Objects.requireNonNull(exif.getAttribute(ExifInterface.TAG_ORIENTATION)).equalsIgnoreCase("8")) {
                realImage = rotate(realImage, 270);
            } else if (Objects.requireNonNull(exif.getAttribute(ExifInterface.TAG_ORIENTATION)).equalsIgnoreCase("3")) {
                realImage = rotate(realImage, 180);
            } else if (Objects.requireNonNull(exif.getAttribute(ExifInterface.TAG_ORIENTATION)).equalsIgnoreCase("0")) {
                realImage = rotate(realImage, 90);
            }

            realImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            Bitmap finalRealImage = realImage;
            runOnUiThread(() -> {
                proceedtonext.setEnabled(true);
                proceedtonext.setImageBitmap(finalRealImage);
            });



            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        //       mtx.postRotate(degree);
        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent datas) {
        super.onActivityResult(requestCode, resultCode, datas);
        if (requestCode == PICK_IMAGE) {
            if(datas!=null){
                if (datas.getClipData() != null) {
                    ClipData clipData = datas.getClipData();
                    ByteArrayOutputStream byteArrayOutputStream;
                    FileOutputStream fos;
                    Bitmap bitmap = null;
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        String file_name_selected = (i + 1) + ".jpeg";
                        Uri selectedImage = clipData.getItemAt(i).getUri();//As of now use static position 0 use as per itemcount.
                        byteArrayOutputStream=new ByteArrayOutputStream();
                        try {
                            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                            fos = new FileOutputStream(data.getImage_path() + "/" + file_name_selected);
                            data.setFilename(file_name_selected);
                            fos.write(byteArrayOutputStream.toByteArray());
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    try {
                        Date c = Calendar.getInstance().getTime();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyyyy_HHmmss");
                        String file_name_selected = "IMG_" + simpleDateFormat.format(c) + ".jpeg";
                        final Uri imageUri = datas.getData();
                        final InputStream imageStream;
                        assert imageUri != null;
                        imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        selectedImage.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                        FileOutputStream fos = new FileOutputStream(data.getImage_path() + "/" + file_name_selected);
                        data.setFilename(file_name_selected);
                        fos.write(byteArrayOutputStream.toByteArray());
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                closeCamera();
                Intent intent=new Intent(image_capture.this,captured_image_display.class);
                startActivity(intent);
            }
        }
    }
}