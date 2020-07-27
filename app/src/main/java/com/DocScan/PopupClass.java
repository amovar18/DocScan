package com.DocScan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.io.File;

public class PopupClass {
    private  datastore ds=new datastore();

    int value_to_be_returned=0;
    public int showPopupWindow(final View view, double[] size) {

        //Create a View object yourself through inflater
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
                value_to_be_returned=1;
            }
        });
        size_reduced.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                value_to_be_returned=2;
            }
        });
        return value_to_be_returned;
        //Handler for clicking on the inactive zone of the window
    }
    public double[] get_size_file(){
        double original_pdf_size=0.0,reduced_pdf_size=0.0;
        int init_size=0,original_size=0;
        for(int i=0;i<ds.getSize();i++) {
            Bitmap bitmap = BitmapFactory.decodeFile(new File(ds.getImage_path() + "/" + ds.getFilename(i)).toString());
            bitmap.reconfigure(bitmap.getWidth(),bitmap.getHeight(), Bitmap.Config.RGB_565);
            original_size=original_size+bitmap.getAllocationByteCount();
            bitmap.reconfigure(bitmap.getWidth(),bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            init_size=init_size+bitmap.getAllocationByteCount();
            bitmap.recycle();
        }
        original_pdf_size=original_size/(1024*1024);
        reduced_pdf_size=reduced_pdf_size/(1024*1024);
        double[] sizes=new double[2];
        sizes[0]=original_pdf_size;
        sizes[1]=reduced_pdf_size;
        return sizes;
    }

}
