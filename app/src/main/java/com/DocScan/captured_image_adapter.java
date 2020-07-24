package com.DocScan;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
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

        public imageViewHolder(View view) {
            super(view);
            layout = view;
            main_ImageView=view.findViewById(R.id.single_image);
            delete_imageview=view.findViewById(R.id.delete_single_image);
        }
    }

    public void add(int position, String item) {
        dataset.add(position, item);
        notifyItemInserted(position);
    }

    public void remove(int position) {
        dataset.remove(position);
        notifyItemRemoved(position);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public captured_image_adapter(ArrayList<String> passedDataset, Context context) {
        dataset = passedDataset;
        this.context=context;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public imageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.single_file_detail_shower, parent, false);
        imageViewHolder image = new imageViewHolder(v);
        return image;
    }

    @Override
    public void onBindViewHolder(@NonNull imageViewHolder holder, int position) {
        holder.delete_imageview.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                File file=new File(object.getImage_path()+"/"+object.getFilename(position));
                if(file.exists()){
                    file.delete();
                }
                notifyDataSetChanged();
            }
        });
        holder.main_ImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(context,cropper.class);
                intent.putExtra("data",position);
                context.startActivity(intent);
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return dataset.size();
    }
}
