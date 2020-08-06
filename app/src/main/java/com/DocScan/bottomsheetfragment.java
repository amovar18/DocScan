package com.DocScan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class bottomsheetfragment extends BottomSheetDialogFragment {
    TextView text_display;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.bottomsheet_for_ocr_text, container, false);
        text_display=view.findViewById(R.id.display_extracted_text);
        assert getArguments() != null;
        text_display.setText(getArguments().getString("extracted_text"));
        return view;
    }
}
