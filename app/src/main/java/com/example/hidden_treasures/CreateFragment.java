package com.example.hidden_treasures;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;

public class CreateFragment extends Fragment {

    private static final String TAG = "CreateFragment";

    private File photoFile;
    private EditText etTitle;
    private EditText etDescription;
    private Button btnSubmitMarker;
    private ImageView ivPreview;
    private VideoView vvPreview;
    private Button btnTakePicture;
    private Button btnTakeVideo;

    private ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (result == true) {
                        Log.i(TAG, "took picture");
                        //decoding image
                        Bitmap takenImage = BitmapFactory.decodeFile(photoFile.getAbsolutePath());

                        // make image preview visible
                        ivPreview.setVisibility(View.VISIBLE);

                        //setting the image to the image preview in layout
                        ivPreview.setImageBitmap(takenImage);
                    } else {
                        Log.e(TAG, "picture wasn't taken");
                    }
                }
            });

    private final ActivityResultLauncher<Intent> videoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri contentURI = result.getData().getData();
                vvPreview.setVisibility(View.VISIBLE);
                vvPreview.setVideoURI(contentURI);
                vvPreview.start();
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
        return inflater.inflate(R.layout.fragment_create, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // get references to all the views in layout
        ivPreview = (ImageView) view.findViewById(R.id.ivPreview);
        vvPreview = (VideoView) view.findViewById(R.id.vvPreview);
        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        btnSubmitMarker = view.findViewById(R.id.btnSubmitMarker);
        btnTakePicture = view.findViewById(R.id.btnTakePicture);
        btnTakeVideo = view.findViewById(R.id.btnTakeVideo);

        // set the onClick listeners for picture and video button
        setButtonListeners();
    }


    /* Sets the onClickListeners for any buttons */
    private void setButtonListeners() {
        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // make sure a previous photo or video taken is removed from the view
                setMediaPreviewInvisible();

                // getting a file reference
                photoFile = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_image_" + System.currentTimeMillis() + ".png");

                // wrapping File object into a content provider
                Uri fileProvider = FileProvider.getUriForFile(getContext(), "com.example.hidden_treasures.fileprovider", photoFile);

                // launch intent to open camera
                cameraLauncher.launch(fileProvider);
            }
        });

        btnTakeVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // make sure a previous photo or video taken is removed from the view
                setMediaPreviewInvisible();

                // create intent to open video camera
                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

                // only launch intent if it can be handled
                if (getContext().getPackageManager().resolveActivity(intent, 0) != null) {
                    videoLauncher.launch(intent);
                } else {
                    Toast.makeText(getContext(), "No apps supports this action", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /* Resets the image and video views to invisible whenever user takes a new picture/video*/
    private void setMediaPreviewInvisible() {
        ivPreview.setVisibility(View.INVISIBLE);
        vvPreview.setVisibility(View.INVISIBLE);
    }
}