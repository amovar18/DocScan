package com.DocScan;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;

import io.reactivex.disposables.CompositeDisposable;

public class captured_image_display extends AppCompatActivity {
    private FloatingActionButton floatingActionButton;
    BitmapFactory.Options options=new BitmapFactory.Options();
    private datastore ds=new datastore();
    private RecyclerView recyclerView;
    private Toolbar toolbar;
    private double[] file_sizes= new double[2];
    private RecyclerView.Adapter adapter;
    private ProgressBar progressBar;
    private Button start_saving;
    private EditText filename;
    protected CompositeDisposable disposable = new CompositeDisposable();
    private ArrayList<String> dataSet=ds.getAllData();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captured_image_display);
        filename=findViewById(R.id.filename_to_be_set);
        recyclerView=findViewById(R.id.image_displayer);
        adapter=new captured_image_adapter(dataSet,captured_image_display.this);
        recyclerView.setAdapter(adapter);
        ItemTouchHelper ith = new ItemTouchHelper(itemCallback);
        ith.attachToRecyclerView(recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this,2));
        toolbar=findViewById(R.id.toolbar);
        floatingActionButton=findViewById(R.id.add_more_images);
        start_saving=findViewById(R.id.save_pdf_action);
        start_saving.setOnClickListener(save_pdf);
        filename.setText(ds.getFolder());
        progressBar=findViewById(R.id.progress_circular);
        progressBar.setVisibility(View.GONE);
        floatingActionButton.bringToFront();
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(captured_image_display.this,image_capture.class);
                startActivity(intent);
            }
        });
    }

    ItemTouchHelper.Callback itemCallback = new ItemTouchHelper.Callback() {
        public boolean onMove(@NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            // get the viewHolder's and target's positions in your adapter data, swap them
            Collections.swap(dataSet, viewHolder.getAdapterPosition(), target.getAdapterPosition());
            // and notify the adapter that its dataset has changed
            adapter.notifyDataSetChanged();
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        }

        //defines the enabled move directions in each state (idle, swiping, dragging).
        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            return makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                    ItemTouchHelper.DOWN | ItemTouchHelper.UP | ItemTouchHelper.START | ItemTouchHelper.END);
        }
    };
    private View.OnClickListener save_pdf=new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            double[] size=get_size_file();
            LayoutInflater inflater = (LayoutInflater) view.getContext().getSystemService(view.getContext().LAYOUT_INFLATER_SERVICE);
            View popupView = inflater.inflate(R.layout.select_pdf_size, null);

            //Specify the length and width through constants
            int width = LinearLayout.LayoutParams.MATCH_PARENT;
            int height = LinearLayout.LayoutParams.MATCH_PARENT;

            //Make Inactive Items Outside Of PopupWindow
            boolean focusable = true;

            //Create a window with our parameters
            final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

            //Set the location of the window on the screen
            popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

            //Initialize the elements of our window, install the handler

            TextView size_original = popupView.findViewById(R.id.original_size);
            TextView size_reduced = popupView.findViewById(R.id.reduced_size);
            String original = "Original size ("+size[0]+" MB).";
            String reduced = "Compressed size ("+size[1]+" MB).";
            size_original.setText(original);
            size_reduced.setText(reduced);
            size_original.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    save_to_pdf(100);
                }
            });
            size_reduced.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    save_to_pdf(50);
                }
            });
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        floatingActionButton.bringToFront();
        adapter.notifyDataSetChanged();
    }
    public void save_to_pdf(int compressionRatio) {
        if(filename.getText().toString().isEmpty()){
            filename.setError("Enter PDF NAME!!!");
        }else if(ds.getSize()<=0){
            AlertDialog.Builder builder=new AlertDialog.Builder(getApplicationContext());
            builder.setTitle("Oops no image to save:");
            builder.setMessage("You cannot save pdf without any images:");
            builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });
            builder.show();
        }else{
            options.inMutable=true;
            progressBar.setVisibility(View.VISIBLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            String to_be_processed_filename=filename.getText().toString();
            String final_filename=to_be_processed_filename.replaceAll("[\\s\\W+]","_");
            final_filename=final_filename+".pdf";
            PdfDocument.PageInfo pageInfo;
            PdfDocument document = new PdfDocument();
            ByteArrayOutputStream byteArrayOutputStream;
            Bitmap final_bitmap;
            for(int i=0;i<ds.getSize();i++){
                byteArrayOutputStream=new ByteArrayOutputStream();
                Bitmap bitmap= BitmapFactory.decodeFile(new File(ds.getImage_path()+"/"+ds.getFilename(i)).toString(),options);
                bitmap.compress(Bitmap.CompressFormat.JPEG,compressionRatio,byteArrayOutputStream);
                pageInfo=new PdfDocument.PageInfo.Builder(bitmap.getWidth(),bitmap.getHeight(),(i+1)).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                Paint paint = new Paint();
                paint.setColor(Color.parseColor("#ffffff"));
                canvas.drawPaint(paint);

                paint.setColor(Color.BLUE);
                final_bitmap=BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(),0,byteArrayOutputStream.toByteArray().length);
                canvas.drawBitmap(final_bitmap, 0, 0 , null);

                document.finishPage(page);
                final_bitmap.recycle();

            }
            try {
                document.writeTo(new FileOutputStream(Environment.getExternalStorageDirectory()+"/DocScan/"+final_filename));
            }catch (IOException e){
                e.printStackTrace();
            }
            File file=new File(ds.getImage_path()+"/");
            File[] temp_files=file.listFiles();
            assert temp_files != null;
            for(File delete_file : temp_files){
                delete_file.delete();
            }
            if(file.exists()){
                file.delete();
            }
            Intent intent=new Intent(captured_image_display.this,MainActivity.class);
            progressBar.setVisibility(View.GONE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            ds.clear_all_data();
            startActivity(intent);
            disposable.dispose();
            finish();
        }
    }
    public double[] get_size_file(){
        options.inMutable=true;
        double original_pdf_size=0.0,reduced_pdf_size=0.0;
        int reduced_size=0,original_size=0;
        for(int i=0;i<ds.getSize();i++) {
            Bitmap bitmap = BitmapFactory.decodeFile(new File(ds.getImage_path() + "/" + ds.getFilename(i)).toString(),options);
            bitmap.reconfigure(bitmap.getWidth(),bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            original_size=original_size+bitmap.getAllocationByteCount();
            bitmap.reconfigure(bitmap.getWidth(),bitmap.getHeight(), Bitmap.Config.RGB_565);
            reduced_size=reduced_size+bitmap.getAllocationByteCount();
            bitmap.recycle();
        }
        original_pdf_size=original_size/(1024*1024);
        reduced_pdf_size=reduced_size/(1024*1024*2);
        double[] sizes=new double[2];
        sizes[0]=original_pdf_size;
        sizes[1]=reduced_pdf_size;
        return sizes;
    }
}