package com.DocScan;

import java.util.ArrayList;

public class datastore {
    private static ArrayList<String> filenames=new ArrayList<String>();
    private static String image_path= "";
    private static String image_directory_name="";
    private static boolean is_path_set=false;
    public void setImage_Path(String path,String folder){
        this.image_path=path;
        is_path_set=true;
        image_directory_name=folder;
    }
    public boolean has_path_set(){
        return is_path_set;
    }
    public void setFilename(String filename){
        filenames.add(filename);
    }
    public String getFilename(int position){
        return filenames.get(position);
    }

    public String getImage_path() {
        return image_path;
    }
    public void swapdata(){

    }
    public void clear_all_data(){
        filenames.clear();
        image_path="";
        image_directory_name="";
        is_path_set=false;
    }
    public String getFolder(){
        return  image_directory_name;
    }
    public int getSize(){
        return filenames.size();
    }
}
