package com.DocScan;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import io.reactivex.disposables.CompositeDisposable;

public class captured_image_display extends AppCompatActivity {
    private FloatingActionButton floatingActionButton;
    BitmapFactory.Options options=new BitmapFactory.Options();
    private datastore ds=new datastore();
    private RecyclerView.Adapter adapter;
    private ProgressBar progressBar;
    private EditText filename;
    protected CompositeDisposable disposable = new CompositeDisposable();
    private ArrayList<String> dataSet=ds.getAllData();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captured_image_display);
        filename=findViewById(R.id.filename_to_be_set);
        RecyclerView recyclerView = findViewById(R.id.image_displayer);
        adapter=new captured_image_adapter(dataSet,captured_image_display.this);
        recyclerView.setAdapter(adapter);
        ItemTouchHelper ith = new ItemTouchHelper(itemCallback);
        ith.attachToRecyclerView(recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this,2));
        Toolbar toolbar = findViewById(R.id.toolbar);
        floatingActionButton=findViewById(R.id.add_more_images);
        Button start_saving = findViewById(R.id.save_pdf_action);
        start_saving.setOnClickListener(save_pdf);
        filename.setText(ds.getFolder());
        progressBar=findViewById(R.id.progress_circular);
        progressBar.setVisibility(View.GONE);
        floatingActionButton.bringToFront();
        floatingActionButton.setOnClickListener(view -> {
            Intent intent=new Intent(captured_image_display.this,image_capture.class);
            startActivity(intent);
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
    private View.OnClickListener save_pdf= view -> {
        view.getContext();
        LayoutInflater inflater = (LayoutInflater) view.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
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
        String original = "Original size.";
        String reduced = "Compressed size (50%).";
        size_original.setText(original);
        size_reduced.setText(reduced);
        size_original.setOnClickListener(view1 -> {
                popupWindow.dismiss();
                save_to_pdf(1.0f);
        });
        size_reduced.setOnClickListener(view12 ->{
            popupWindow.dismiss();
            save_to_pdf(0.5f);
        });
    };

    @Override
    protected void onResume() {
        super.onResume();
        floatingActionButton.bringToFront();
        adapter.notifyDataSetChanged();
    }
    public void save_to_pdf(float Factor) {
        if(filename.getText().toString().isEmpty()){
            filename.setError("Enter PDF NAME!!!");
        }else if(ds.getSize()<=0){
            AlertDialog.Builder builder=new AlertDialog.Builder(getApplicationContext());
            builder.setTitle("Oops no image to save:");
            builder.setMessage("You cannot save pdf without any images:");
            builder.setNeutralButton("OK", (dialogInterface, i) -> dialogInterface.cancel());
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
            Bitmap final_bitmap;
            for(int i=0;i<ds.getSize();i++){
                Bitmap bitmap= BitmapFactory.decodeFile(new File(ds.getImage_path()+"/"+ds.getFilename(i)).toString(),options);
                final_bitmap=Bitmap.createScaledBitmap(bitmap,(int)(bitmap.getWidth()*Factor),(int)(bitmap.getWidth()*Factor),false);
                pageInfo=new PdfDocument.PageInfo.Builder(final_bitmap.getWidth(),final_bitmap.getHeight(),(i+1)).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                Paint paint = new Paint();
                paint.setColor(Color.parseColor("#ffffff"));
                canvas.drawPaint(paint);

                paint.setColor(Color.BLUE);
                canvas.drawBitmap(final_bitmap, 0, 0 , null);

                document.finishPage(page);
                final_bitmap.recycle();

            }
            try {
                document.writeTo(new FileOutputStream(Environment.getExternalStorageDirectory()+"/DocScan/"+final_filename));
                document.close();
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
}