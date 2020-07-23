package com.DocScan;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebStorage;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.DocScan.base.CropperErrorType;
import com.DocScan.base.DocumentScanActivity;
import com.DocScan.helpers.ImageUtils;
import com.DocScan.helpers.ScannerConstants;
import com.DocScan.libraries.PolygonView;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class cropper extends DocumentScanActivity {
    private FrameLayout holderImageCrop;
    private ProgressBar progressBar;
    private Bitmap cropImage,originalImage;
    private boolean isInverted;
    private  ImageView imageView;
    private TextView ic_rotate,ic_monochrome, ic_black_and_white, ic_align, ic_original;
    private  PolygonView polygonView;
    private Button crop,done,extract;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cropper);
        cropImage=ScannerConstants.selectedImageBitmap;
        originalImage=ScannerConstants.selectedImageBitmap;
        isInverted=false;
        if (ScannerConstants.selectedImageBitmap != null)
            init();
        else {
            Toast.makeText(this, ScannerConstants.imageError, Toast.LENGTH_LONG).show();
            finish();
        }
    }
    private void init(){
        imageView=findViewById(R.id.imageView);
        holderImageCrop=findViewById(R.id.holderImageCrop);
        progressBar=findViewById(R.id.progressBar);
        ic_align=findViewById(R.id.ivRebase);
        ic_rotate=findViewById(R.id.ivRotate);
        ic_monochrome=findViewById(R.id.ivInvert);
        ic_black_and_white=findViewById(R.id.ivBlackandWhite);
        ic_original=findViewById(R.id.ivOriginal);
        polygonView=findViewById(R.id.polygonView);
        crop=findViewById(R.id.btnImageCrop);
        done=findViewById(R.id.btnDone);
        extract=findViewById(R.id.btnScanText);
        crop.setOnClickListener(startCroppingImage);
        extract.setOnClickListener(extractTextFromImage);
        done.setOnClickListener(setImageFinal);
        ic_align.setOnClickListener(realign);
        ic_rotate.setOnClickListener(rotateimage);
        ic_monochrome.setOnClickListener(setMonochrome);
        ic_black_and_white.setOnClickListener(setBlackandwhite);
        ic_original.setOnClickListener(setOriginal);
        startCropping();
    }
    private View.OnClickListener setImageFinal =new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            finish();
        }
    };
    private View.OnClickListener startCroppingImage =new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showProgressBar();
            disposable.add(
                    Observable.fromCallable(() -> {
                        cropImage = getCroppedImage();
                        if (cropImage == null)
                            return false;
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                hideProgressBar();
                                if (cropImage != null) {
                                    ScannerConstants.selectedImageBitmap = cropImage;
                                    originalImage=cropImage;
                                    setResult(RESULT_OK);
                                }
                            })
            );
        }
    };
    private View.OnClickListener extractTextFromImage=new View.OnClickListener() {
        @Override
        public void onClick(View view) {

        }
    };

    private View.OnClickListener realign=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            cropImage = ScannerConstants.selectedImageBitmap.copy(ScannerConstants.selectedImageBitmap.getConfig(), true);
            isInverted = false;
            startCropping();
        }
    };
    private View.OnClickListener setMonochrome =new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showProgressBar();
            disposable.add(
                    Observable.fromCallable(() -> {
                        invertColor();
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                hideProgressBar();
                                Bitmap scaledBitmap = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                                imageView.setImageBitmap(scaledBitmap);
                            })
            );
        }
    };
    private View.OnClickListener setBlackandwhite=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showProgressBar();
            disposable.add(
                    Observable.fromCallable(() -> {
                        setGrayscale(cropImage);
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                hideProgressBar();
                                Bitmap scaledBitmap = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                                imageView.setImageBitmap(scaledBitmap);
                            })
            );
        }
    };
    private View.OnClickListener rotateimage=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showProgressBar();
            disposable.add(
                    Observable.fromCallable(() -> {
                        cropImage = rotateBitmap(cropImage, 90);
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                hideProgressBar();
                                startCropping();
                            })
            );
        }
    };
    private View.OnClickListener setOriginal =new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showProgressBar();
            cropImage=originalImage.copy(originalImage.getConfig(),true);
            ScannerConstants.selectedImageBitmap=originalImage.copy(originalImage.getConfig(),true);
            imageView.setImageBitmap(originalImage);
            hideProgressBar();
        }
    };
    @Override
    protected PolygonView getPolygonView() {
        return polygonView;
    }

    @Override
    protected ImageView getImageView() {
        return imageView;
    }

    @Override
    protected FrameLayout getHolderImageCrop() {
        return holderImageCrop;
    }

    @Override
    protected void showProgressBar() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected void hideProgressBar() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected Bitmap getBitmapImage() {
        return cropImage;
    }

    @Override
    protected void showError(CropperErrorType errorType) {

    }
    public Bitmap setGrayscale(Bitmap bitmap){
        Mat src= ImageUtils.bitmapToMat(bitmap);
        Mat grey=new Mat();
        Imgproc.adaptiveThreshold(src, grey, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 40);
        Bitmap grayscale_result=ImageUtils.matToBitmap(grey);
        return grayscale_result;
    }
    private void invertColor() {
        if (!isInverted) {
            Bitmap bmpMonochrome = Bitmap.createBitmap(cropImage.getWidth(), cropImage.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmpMonochrome);
            ColorMatrix ma = new ColorMatrix();
            ma.setSaturation(0);
            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(ma));
            canvas.drawBitmap(cropImage, 0, 0, paint);
            cropImage = bmpMonochrome.copy(bmpMonochrome.getConfig(), true);
        } else {
            cropImage = cropImage.copy(cropImage.getConfig(), true);
        }
        isInverted = !isInverted;
    }
}