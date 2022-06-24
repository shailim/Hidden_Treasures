package com.example.hidden_treasures;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;

public class CreateFragment extends Fragment {

    private static final String TAG = "CreateFragment";

    private File photoFile;
    private EditText etTitle;
    private EditText etDescription;
    private Button btnSubmitMarker;
    private ImageView ivPreview;

    ActivityResultLauncher<Uri> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (result == true) {
                        Log.i(TAG, "took picture");
                        //decoding image
                        Bitmap takenImage = BitmapFactory.decodeFile(photoFile.getAbsolutePath());

                        //setting the image to the image preview in layout
                        ivPreview.setImageBitmap(takenImage);
                    } else {
                        Log.e(TAG, "picture wasn't taken");
                    }
                }
            });

    public CreateFragment() {
        // Required empty public constructor
    }

    public static CreateFragment newInstance() {
        CreateFragment fragment = new CreateFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_create, container, false);

        // get references to all the views in layout
        ivPreview = (ImageView) view.findViewById(R.id.ivPreview);
        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        btnSubmitMarker = view.findViewById(R.id.btnSubmitMarker);

        // getting a file reference
        photoFile = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_image_" + System.currentTimeMillis() + ".png");

        // wrapping File object into a content provider
        Uri fileProvider = FileProvider.getUriForFile(getContext(), "com.example.hidden_treasures.fileprovider", photoFile);

        // launch intent to open camera
        resultLauncher.launch(fileProvider);

        // return the view
        return view;
    }
}