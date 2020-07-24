package com.DocScan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Environment;
import android.widget.Toolbar;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    Toolbar toolbar;
    RecyclerView.Adapter adapter;
    ArrayList<String> fileset=new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar=findViewById(R.id.toolbar);
        recyclerView=findViewById(R.id.files_details_holder);
        File app_specific_files=new File(getExternalFilesDir(null)+"/Pictures");
        if(!app_specific_files.exists()){
            app_specific_files.mkdirs();
        }else{
            File[] temp_files=app_specific_files.listFiles();
            for(File listing:temp_files){
                fileset.add(listing.getName());
            }
        }
        File document_files=new File(Environment.getExternalStorageDirectory()+"/DocScan");
        if(!document_files.exists()){
            document_files.mkdirs();
        }else {
            File[] allfiles = document_files.listFiles();
            for (File temp_file : allfiles) {
                fileset.add(temp_file.getName());
            }
        }
        adapter=new file_detail_adapter(fileset,getApplicationContext());
        recyclerView.setAdapter(adapter);
    }
}