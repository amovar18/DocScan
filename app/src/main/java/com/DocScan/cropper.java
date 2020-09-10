package com.DocScan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.DocScan.base.CropperErrorType;
import com.DocScan.base.DocumentScanActivity;
import com.DocScan.helpers.ImageUtils;
import com.DocScan.helpers.ScannerConstants;
import com.DocScan.libraries.PolygonView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class cropper extends DocumentScanActivity {
    datastore data_object=new datastore();
    private FrameLayout holderImageCrop;
    private ProgressBar progressBar;
    private Bitmap cropImage,originalImage,bmp,alteredBitmap;
    private boolean isInverted=false;
    private HorizontalScrollView horizontalScrollView;
    private LinearLayout holder,palette,image_editor;
    private  DrawableImageView imageView;
    private  PolygonView polygonView;
    private BottomSheetBehavior sheetBehavior;
    private File file;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cropper);
        Toolbar toolbar=findViewById(R.id.toolbar);
        file=new File(data_object.getImage_path()+"/"+data_object.getFilename(getIntent().getIntExtra("data",0)));
        ScannerConstants.selectedImageBitmap= BitmapFactory.decodeFile(file.toString());
        cropImage=ScannerConstants.selectedImageBitmap;
        //setting bottomsheet for scanning text from image
        LinearLayout bottom_sheet = findViewById(R.id.bottom_sheet);
        sheetBehavior = BottomSheetBehavior.from(bottom_sheet);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        sheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        sheetBehavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                    case BottomSheetBehavior.STATE_HALF_EXPANDED:
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View view, float v) {

            }
        });
        isInverted=false;
        if (ScannerConstants.selectedImageBitmap != null) {
            init();
            imageView.setImageBitmap(cropImage);
            holderImageCrop.post(new Runnable() {
                @Override
                public void run() {
                    cropImage = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                    ScannerConstants.selectedImageBitmap = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                    originalImage = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                    alteredBitmap = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                }
            });
        }else {
            Toast.makeText(this, ScannerConstants.imageError, Toast.LENGTH_LONG).show();
            finish();
        }

    }
    private void init(){
        Button crop,done;
        ImageView ic_rotate,ic_monochrome, ic_black_and_white, ic_align, ic_original,extract,image_edit_holder,brush,erase;
        ImageView black,white,purple,red,yellow,green,blue,orange;
        horizontalScrollView=findViewById(R.id.horizontal_scroll_view);
        holder=findViewById(R.id.holder_for_horizontal_scroll);
        palette=findViewById(R.id.color_palette);
        image_editor=findViewById(R.id.image_editor_holder);
        holderImageCrop=findViewById(R.id.holderImageCrop);
        imageView=findViewById(R.id.imageView);
        progressBar=findViewById(R.id.progressBar);
        //Image filter holder and image filter icon
        image_edit_holder=findViewById(R.id.image_edit);
        ic_black_and_white=findViewById(R.id.ivBlackandWhite);
        ic_original=findViewById(R.id.ivOriginal);
        ic_monochrome=findViewById(R.id.ivInvert);
        //Color brush and palette
        erase=findViewById(R.id.erase);
        brush=findViewById(R.id.paint_brush);
        black=findViewById(R.id.black);
        white=findViewById(R.id.white);
        purple=findViewById(R.id.purple);
        green=findViewById(R.id.green);
        yellow=findViewById(R.id.yellow);
        blue=findViewById(R.id.blue);
        red=findViewById(R.id.red);
        orange=findViewById(R.id.orange);
        //Other icons
        ic_align=findViewById(R.id.ivRebase);
        ic_rotate=findViewById(R.id.ivRotate);
        polygonView=findViewById(R.id.polygonView);
        crop=findViewById(R.id.btnImageCrop);
        done=findViewById(R.id.btnDone);
        extract=findViewById(R.id.btnScanText);
        //click listeners
        crop.setOnClickListener(startCroppingImage);
        extract.setOnClickListener(extractTextFromImage);
        done.setOnClickListener(setImageFinal);
        ic_align.setOnClickListener(realign);
        ic_rotate.setOnClickListener(rotateimage);
        ic_monochrome.setOnClickListener(setMonochrome);
        ic_black_and_white.setOnClickListener(setBlackandwhite);
        ic_original.setOnClickListener(setOriginal);
        black.setOnClickListener(setBlack);
        blue.setOnClickListener(setBlue);
        yellow.setOnClickListener(setYellow);
        purple.setOnClickListener(setPurple);
        green.setOnClickListener(setGreen);
        orange.setOnClickListener(setOrange);
        red.setOnClickListener(setRed);
        white.setOnClickListener(setWhite);
        image_edit_holder.setOnClickListener(select_filter);
        brush.setOnClickListener(select_color);
        erase.setOnClickListener(erasedrawing);
        startCropping();
    }
    private View.OnClickListener erasedrawing=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            cropImage=originalImage;
            imageView.startdrawing(alteredBitmap,cropImage);
        }
    };
    private View.OnClickListener setImageFinal =new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            horizontalScrollView.setVisibility(View.INVISIBLE);
            holder.setVisibility(View.INVISIBLE);
            palette.setVisibility(View.INVISIBLE);
            image_editor.setVisibility(View.INVISIBLE);
            imageView.stopdrawing();
            try {
                ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
                if(file.exists()){
                    file.delete();
                }
                alteredBitmap=imageView.getBitmap(imageView);
                alteredBitmap.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
                FileOutputStream fos=new FileOutputStream(file);
                fos.write(byteArrayOutputStream.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
            finish();
        }
    };
    private View.OnClickListener select_filter =new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            horizontalScrollView.setVisibility(View.VISIBLE);
            holder.setVisibility(View.VISIBLE);
            image_editor.setVisibility(View.VISIBLE);
            palette.setVisibility(View.GONE);
            imageView.stopdrawing();
        }
    };
    private View.OnClickListener select_color =new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            horizontalScrollView.setVisibility(View.VISIBLE);
            holder.setVisibility(View.VISIBLE);
            palette.setVisibility(View.VISIBLE);
            image_editor.setVisibility(View.GONE);

            imageView.startdrawing(alteredBitmap,cropImage);
        }
    };
    private View.OnClickListener startCroppingImage =new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            horizontalScrollView.setVisibility(View.INVISIBLE);
            holder.setVisibility(View.INVISIBLE);
            palette.setVisibility(View.INVISIBLE);
            image_editor.setVisibility(View.INVISIBLE);
            imageView.stopdrawing();

            showProgressBar();
            disposable.add(
                    Observable.fromCallable(() -> {
                        cropImage = getCroppedImage();
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                hideProgressBar();
                                if (cropImage != null) {
                                    startCropping();
                                    cropImage = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                                    imageView.setImageBitmap(cropImage);
                                    ScannerConstants.selectedImageBitmap = cropImage;
                                    originalImage=cropImage;
                                }
                            })
            );
        }
    };
    private View.OnClickListener extractTextFromImage=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            horizontalScrollView.setVisibility(View.INVISIBLE);
            holder.setVisibility(View.INVISIBLE);
            palette.setVisibility(View.INVISIBLE);
            image_editor.setVisibility(View.INVISIBLE);
            imageView.stopdrawing();

            cropImage=imageView.getBitmap(imageView);
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(cropImage);
            FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance()
                    .getOnDeviceTextRecognizer();
            textRecognizer.processImage(image)
                    .addOnSuccessListener(result -> {
                        bottomsheetfragment bottom = new bottomsheetfragment();
                        Bundle bundle=new Bundle();
                        bundle.putString("extracted_text",result.getText());
                        bottom.setArguments(bundle);
                        bottom.show(getSupportFragmentManager(), bottom.getTag());
                    })
                    .addOnFailureListener(
                            e -> Toast.makeText(cropper.this, "Could not extract text !!!", Toast.LENGTH_SHORT).show());
        }
    };

    private View.OnClickListener realign=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            horizontalScrollView.setVisibility(View.INVISIBLE);
            holder.setVisibility(View.INVISIBLE);
            palette.setVisibility(View.INVISIBLE);
            image_editor.setVisibility(View.INVISIBLE);
            imageView.stopdrawing();
            cropImage = ScannerConstants.selectedImageBitmap.copy(ScannerConstants.selectedImageBitmap.getConfig(), true);
            startCropping();
        }
    };
    private View.OnClickListener setMonochrome =new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            showProgressBar();
            disposable.add(
                    Observable.fromCallable(() -> {
                        cropImage=imageView.getBitmap(imageView);
                        invertColor();
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                hideProgressBar();
                                cropImage = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                                imageView.setImageBitmap(cropImage);
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
                        cropImage=setGrayscale(cropImage);
                        isInverted = false;
                        return false;
                    })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe((result) -> {
                                hideProgressBar();
                                startCropping();
                                cropImage = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
                                imageView.setImageBitmap(cropImage);
                            })
            );
        }
    };
    private View.OnClickListener rotateimage=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            horizontalScrollView.setVisibility(View.INVISIBLE);
            holder.setVisibility(View.INVISIBLE);
            palette.setVisibility(View.INVISIBLE);
            image_editor.setVisibility(View.INVISIBLE);
            imageView.stopdrawing();

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
    private View.OnClickListener setOriginal = view -> {
        showProgressBar();
        setoriginalImage();
        hideProgressBar();
    };

    private void setoriginalImage() {
        cropImage=originalImage.copy(originalImage.getConfig(),true);
        ScannerConstants.selectedImageBitmap=originalImage.copy(originalImage.getConfig(),true);
        //Bitmap scaledBitmap = scaledBitmap(cropImage, holderImageCrop.getWidth(), holderImageCrop.getHeight());
        imageView.setImageBitmap(cropImage);
        isInverted = false;
    }

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
        Mat intermediate=new Mat();
        Imgproc.cvtColor(src,intermediate,Imgproc.COLOR_RGB2GRAY);
        Mat grey=new Mat();
        Imgproc.adaptiveThreshold(intermediate, grey, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 40);
        return ImageUtils.matToBitmap(grey);
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
        isInverted = true;
    }

    private View.OnClickListener setBlack=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            imageView.setColor(Color.BLACK);
        }
    };
    private View.OnClickListener setBlue=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            imageView.setColor(Color.BLUE);
        }
    };
    private View.OnClickListener setWhite=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            imageView.setColor(Color.WHITE);
        }
    };
    private View.OnClickListener setYellow=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            imageView.setColor(Color.YELLOW);
        }
    };
    private View.OnClickListener setRed=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            imageView.setColor(Color.RED);
        }
    };
    private View.OnClickListener setOrange=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            imageView.setColor(Color.parseColor("#ffa200"));
        }
    };
    private View.OnClickListener setPurple=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            imageView.setColor(Color.parseColor("#800080"));
        }
    };
    private View.OnClickListener setGreen=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            imageView.setColor(Color.GREEN);
        }
    };
}