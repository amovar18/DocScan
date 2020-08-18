package com.DocScan;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private File app_specific_files,document_files;
    private Toolbar toolbar;
    private RecyclerView.Adapter adapter;
    private FloatingActionButton floatingActionButton;
    static ArrayList<String> fileset=new ArrayList<>();
    private datastore ds=new datastore();
    private AppUpdateManager mAppUpdateManager;
    private static final int RC_APP_UPDATE = 11;
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
                    File file=new File(getExternalFilesDir(null)+"/Pictures/"+listing.getName());
                    File[] temp_file=file.listFiles();
                    assert temp_file != null;
                    if(temp_file.length<=0){
                        file.delete();
                    }else {
                        fileset.add(listing.getName());
                    }
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
        floatingActionButton.setOnClickListener(view -> {
            Intent intent= new Intent(MainActivity.this,image_capture.class);
            startActivity(intent);
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
    protected void onStart() {
        super.onStart();
        mAppUpdateManager = AppUpdateManagerFactory.create(this);

        mAppUpdateManager.registerListener(installStateUpdatedListener);

        mAppUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE /*AppUpdateType.IMMEDIATE*/)){

                try {
                    mAppUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo, AppUpdateType.FLEXIBLE /*AppUpdateType.IMMEDIATE*/, MainActivity.this, RC_APP_UPDATE);

                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }

            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED){
                //CHECK THIS if AppUpdateType.FLEXIBLE, otherwise you can skip
                popupSnackbarForCompleteUpdate();
            }
        });
    }
    InstallStateUpdatedListener installStateUpdatedListener = new
            InstallStateUpdatedListener() {
                @Override
                public void onStateUpdate(InstallState state) {
                    if (state.installStatus() == InstallStatus.DOWNLOADED){
                        //CHECK THIS if AppUpdateType.FLEXIBLE, otherwise you can skip
                        popupSnackbarForCompleteUpdate();
                    } else if (state.installStatus() == InstallStatus.INSTALLED){
                        if (mAppUpdateManager != null){
                            mAppUpdateManager.unregisterListener(installStateUpdatedListener);
                        }

                    }
                }
            };
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_APP_UPDATE) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "Application Download Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void popupSnackbarForCompleteUpdate() {

        Snackbar snackbar =
                Snackbar.make(
                        findViewById(R.id.coordinator),
                        "New app is ready!",
                        Snackbar.LENGTH_INDEFINITE);

        snackbar.setAction("Install", view -> {
            if (mAppUpdateManager != null){
                mAppUpdateManager.completeUpdate();
            }
        });


        snackbar.setActionTextColor(ContextCompat.getColor(getApplicationContext(),R.color.colorAccentPrimary));
        snackbar.show();
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
                File file=new File(getExternalFilesDir(null)+"/Pictures/"+listing.getName());
                File[] temp_file=file.listFiles();
                assert temp_file != null;
                if(temp_file.length<=0){
                    file.delete();
                }else {
                    fileset.add(listing.getName());
                }
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

    @Override
    protected void onStop() {
        super.onStop();
        if (mAppUpdateManager != null) {
            mAppUpdateManager.unregisterListener(installStateUpdatedListener);
        }
    }
}