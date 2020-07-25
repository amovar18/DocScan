package com.DocScan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

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
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class captured_image_display extends AppCompatActivity {
    FloatingActionButton floatingActionButton;
    datastore ds=new datastore();
    RecyclerView recyclerView;
    Toolbar toolbar;
    RecyclerView.Adapter adapter;
    ProgressBar progressBar;
    Button start_saving;
    EditText filename;
    ArrayList<String> dataSet=ds.getAllData();
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
                progressBar.setVisibility(View.VISIBLE);
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                String to_be_processed_filename=filename.getText().toString();
                String final_filename=to_be_processed_filename.replaceAll("[\\s\\W+]","_");
                final_filename=final_filename+".pdf";
                PdfDocument.PageInfo pageInfo;
                PdfDocument document = new PdfDocument();
                for(int i=0;i<ds.getSize();i++){
                    Bitmap bitmap= BitmapFactory.decodeFile(new File(ds.getImage_path()+"/"+ds.getFilename(i)).toString());
                    pageInfo=new PdfDocument.PageInfo.Builder(bitmap.getWidth(),bitmap.getHeight(),(i+1)).create();
                    PdfDocument.Page page = document.startPage(pageInfo);

                    Canvas canvas = page.getCanvas();

                    Paint paint = new Paint();
                    paint.setColor(Color.parseColor("#ffffff"));
                    canvas.drawPaint(paint);

                    paint.setColor(Color.BLUE);
                    canvas.drawBitmap(bitmap, 0, 0 , null);

                    document.finishPage(page);
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
                finish();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        floatingActionButton.bringToFront();
        adapter.notifyDataSetChanged();
    }
}