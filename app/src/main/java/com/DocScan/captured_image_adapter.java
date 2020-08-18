package com.DocScan;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

public class captured_image_adapter extends RecyclerView.Adapter<captured_image_adapter.imageViewHolder> {
    private ArrayList<String> dataset;
    private Context context;
    datastore object=new datastore();
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class imageViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public ImageView delete_imageview,main_ImageView;
        public View layout;
        public CardView cardView;
        public imageViewHolder(View view) {
            super(view);
            layout = view;
            cardView=view.findViewById(R.id.single_image_holder_cardview);
            main_ImageView=view.findViewById(R.id.single_image);
            delete_imageview=view.findViewById(R.id.delete_single_image);

        }
    }
    // Provide a suitable constructor (depends on the kind of dataset)
    public captured_image_adapter(ArrayList<String> passedDataset, Context context) {
        dataset = passedDataset;
        this.context=context;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public imageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.image_display, parent, false);
        imageViewHolder image = new imageViewHolder(v);
        return image;
    }

    @Override
    public void onBindViewHolder(@NonNull imageViewHolder holder, int position) {
        Bitmap display_bitmap= BitmapFactory.decodeFile(object.getImage_path()+"/"+object.getFilename(position));
        holder.main_ImageView.setImageBitmap(display_bitmap);
        holder.delete_imageview.setOnClickListener(view -> {
            File file=new File(object.getImage_path()+"/"+object.getFilename(position));
            if(file.exists()){
                file.delete();
                dataset.remove(position);
            }
            notifyDataSetChanged();
        });
        holder.main_ImageView.setOnClickListener(view -> {
            Intent intent=new Intent(context,cropper.class);
            intent.putExtra("data",position);
            context.startActivity(intent);
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return dataset.size();
    }
}
