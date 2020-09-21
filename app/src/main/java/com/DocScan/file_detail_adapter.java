package com.DocScan;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class file_detail_adapter extends RecyclerView.Adapter<file_detail_adapter.fileViewHolder> {
    private ArrayList<String> dataset;
    private Context context;
    datastore object=new datastore();
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class fileViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView filename_textview;
        public ImageView filethumbnail_imageview,menu_imageview;
        public View layout;

        public fileViewHolder(View view) {
            super(view);
            layout = view;
            filename_textview = view.findViewById(R.id.filename);
            filethumbnail_imageview = view.findViewById(R.id.thumbnail);
            menu_imageview=view.findViewById(R.id.menu_for_file);
        }
    }

    public void add(int position, String item) {
        dataset.add(position, item);
        notifyItemInserted(position);
    }
    // Provide a suitable constructor (depends on the kind of dataset)
    public file_detail_adapter(ArrayList<String> passedDataset, Context context) {
        dataset = passedDataset;
        this.context=context;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public fileViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.single_file_detail_shower, parent, false);
        return new fileViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull fileViewHolder holder, int position) {
        if (dataset.get(position).contains(".")){
            try {
                ParcelFileDescriptor parcelFileDescriptor = ParcelFileDescriptor.open(new File(Environment.getExternalStorageDirectory() + "/DocScan/" + dataset.get(position)), ParcelFileDescriptor.MODE_READ_ONLY);
                PdfRenderer renderer = new PdfRenderer(parcelFileDescriptor);
                Bitmap bitmap;
                PdfRenderer.Page page = renderer.openPage(0);
                bitmap=Bitmap.createBitmap(page.getWidth(),page.getHeight(), Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
                bitmap=bitmap.copy(Bitmap.Config.RGB_565,false);
                holder.filethumbnail_imageview.setImageBitmap(bitmap);
                holder.filename_textview.setText(dataset.get(position));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            File file=new File(context.getExternalFilesDir(null)+"/Pictures/"+dataset.get(position));
            File[] temp_files=file.listFiles();
            assert temp_files != null;
            Bitmap bitmap= BitmapFactory.decodeFile(temp_files[0].getAbsolutePath());
            holder.filethumbnail_imageview.setImageBitmap(bitmap);
            holder.filename_textview.setText(dataset.get(position));
        }
        holder.menu_imageview.setOnClickListener(view -> {
            if(dataset.get(position).contains(".")){
                PopupMenu popupMenu=new PopupMenu(context,holder.menu_imageview);
                popupMenu.getMenuInflater().inflate(R.menu.icon_menu_pdf_file,popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    switch(menuItem.getItemId()){
                        case R.id.pdf_delete:
                            File file=new File(Environment.getExternalStorageDirectory()+"/DocScan/"+dataset.get(position));
                            if(file.exists()){
                                file.delete();
                                dataset.remove(position);
                            }
                            notifyDataSetChanged();
                            break;
                        case R.id.pdf_edit:
                            edit(position,dataset.get(position));
                            break;
                        case R.id.pdf_open:
                            Intent intent=new Intent(Intent.ACTION_VIEW);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            Uri path= FileProvider.getUriForFile(context,BuildConfig.APPLICATION_ID+".provider",new File(Environment.getExternalStorageDirectory()+"/DocScan/"+dataset.get(position)));
                            intent.setDataAndType(path, "application/pdf");
                            context.startActivity(intent);
                            break;
                        case R.id.pdf_share:
                            Intent shareintent=new Intent(Intent.ACTION_SEND);
                            shareintent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            Uri sharepath= FileProvider.getUriForFile(context,BuildConfig.APPLICATION_ID+".provider",new File(Environment.getExternalStorageDirectory()+"/DocScan/"+dataset.get(position)));
                            shareintent.putExtra(Intent.EXTRA_STREAM,sharepath);
                            shareintent.setDataAndType(sharepath, "application/pdf");
                            Intent chooser=Intent.createChooser(shareintent,"Select Mode of Sharing:");
                            context.startActivity(chooser);
                            break;
                    }
                    return false;
                });
                popupMenu.show();
            }else{
                PopupMenu popupMenu=new PopupMenu(context,holder.menu_imageview);
                popupMenu.getMenuInflater().inflate(R.menu.icon_menu_image_directory,popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    switch(menuItem.getItemId()){
                        case R.id.image_directory_delete:
                            File file=new File(context.getExternalFilesDir(null)+"/Pictures/"+dataset.get(position));
                            File[] temp_files=file.listFiles();
                            assert temp_files != null;
                            for(File delete_file : temp_files){
                                delete_file.delete();
                            }
                            if(file.exists()){
                                file.delete();
                                dataset.remove(position);
                            }
                            notifyDataSetChanged();
                            break;
                        case R.id.image_directory_edit:
                            File image_directory_file=new File(context.getExternalFilesDir(null)+"/Pictures/"+dataset.get(position));
                            File[] list_files=image_directory_file.listFiles();
                            assert list_files != null;
                            for (File names:list_files){
                                object.setFilename(names.getName());
                            }
                            object.setImage_Path(image_directory_file.getAbsolutePath(),dataset.get(position));
                            Intent intent=new Intent(context,captured_image_display.class);
                            context.startActivity(intent);
                            break;
                    }
                    return false;
                });
                popupMenu.show();
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return dataset.size();
    }
    public void edit(int position,String folder_name) {
        try {
            File file=new File(Environment.getExternalStorageDirectory() + "/DocScan/" + dataset.get(position));
            folder_name=folder_name.substring(0,(folder_name.indexOf(".")));
            File image_directory = new File(context.getExternalFilesDir(null) + "/Pictures/" + folder_name);
            if (!image_directory.exists()) {
                image_directory.mkdirs();
            }
            object.setImage_Path(image_directory.getAbsolutePath(),folder_name);
            ParcelFileDescriptor parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(parcelFileDescriptor);
            Bitmap bitmap;
            ByteArrayOutputStream byteArrayOutputStream;
            final int pageCount = renderer.getPageCount();
            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page page = renderer.openPage(i);
                bitmap=Bitmap.createBitmap(page.getWidth(),page.getHeight(), Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                byteArrayOutputStream= new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG,100,byteArrayOutputStream);
                FileOutputStream fos =new FileOutputStream(object.getImage_path()+"/"+(i+1)+".jpeg");
                object.setFilename((i+1)+".jpeg");
                fos.write(byteArrayOutputStream.toByteArray());
                fos.close();
                // close the page
                page.close();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        Intent intent=new Intent(context,captured_image_display.class);
        context.startActivity(intent);
    }
}
