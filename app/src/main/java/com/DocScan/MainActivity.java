package com.DocScan;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    File app_specific_files,document_files;
    Toolbar toolbar;
    RecyclerView.Adapter adapter;
    FloatingActionButton floatingActionButton;
    static ArrayList<String> fileset=new ArrayList<>();
    datastore ds=new datastore();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ds.clear_all_data();
        floatingActionButton=findViewById(R.id.start_capturing);
        floatingActionButton.bringToFront();
        recyclerView=findViewById(R.id.files_details_holder);
        app_specific_files=new File(getExternalFilesDir(null)+"/Pictures/");
        if(!app_specific_files.exists()){
            app_specific_files.mkdirs();
        }else{
            File[] temp_files=app_specific_files.listFiles();
            if(temp_files!=null) {
                for (File listing : temp_files) {
                    fileset.add(listing.getName());
                }
            }
        }
        document_files=new File(Environment.getExternalStorageDirectory()+"/DocScan/");
        if(!document_files.exists()){
            document_files.mkdirs();
        }else {
            File[] allfiles = document_files.listFiles();
            if(allfiles!=null) {
                for (File temp_file : allfiles) {
                    fileset.add(temp_file.getName());
                }
            }
        }
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent= new Intent(MainActivity.this,image_capture.class);
                startActivity(intent);
            }
        });
        adapter=new file_detail_adapter(fileset,MainActivity.this);
        recyclerView.setAdapter(adapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        LinearLayoutManager linearLayoutManager1 = new LinearLayoutManager(this);
        linearLayoutManager1.setOrientation(RecyclerView.HORIZONTAL);
        DividerItemDecoration dividerItemDecoration=new DividerItemDecoration(recyclerView.getContext(),linearLayoutManager1.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    protected void onResume() {
        super.onResume();
        floatingActionButton.bringToFront();
        ds.clear_all_data();
        fileset.clear();
        File[] temp_files=app_specific_files.listFiles();
        if(temp_files!=null) {
            for (File listing : temp_files) {
                fileset.add(listing.getName());
            }
        }
        File[] allfiles = document_files.listFiles();
        if(allfiles!=null) {
            for (File temp_file : allfiles) {
                fileset.add(temp_file.getName());
            }
        }
        adapter.notifyDataSetChanged();
    }
}