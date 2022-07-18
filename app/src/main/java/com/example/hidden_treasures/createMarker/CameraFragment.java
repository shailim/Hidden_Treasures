package com.example.hidden_treasures.createMarker;

import static androidx.core.content.FileProvider.getUriForFile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.example.hidden_treasures.R;
import com.example.hidden_treasures.login.LoginActivity;
import com.parse.ParseUser;

import java.io.File;

public class CameraFragment extends Fragment {

    private final String TAG = "CameraFragment";

    private ImageButton cameraButton;
    private File photoFile;

    private ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    Log.i(TAG, "taking pic");
                    if (result == true) {
                        Log.i(TAG, "took picture");
                        //decoding image
                        Bitmap takenImage = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                        Log.i(TAG, photoFile.getAbsolutePath());

                        // to set create fragment as a child fragment
                        FragmentManager childFragMan = getChildFragmentManager();
                        FragmentTransaction childFragTrans = childFragMan.beginTransaction();

                        // create a new create marker fragment instance and pass in image
                        CreateFragment createFragment = CreateFragment.newInstance(takenImage, photoFile);
                        // add the child fragment to current camera fragment
                        childFragTrans.add(R.id.cameraFragmentLayout, createFragment);
                        childFragTrans.addToBackStack(null);
                        childFragTrans.commit();
                    } else {
                        Log.e(TAG, "picture wasn't taken");
                    }
                }
            });

    public CameraFragment() {
        // Required empty public constructor
    }

    public static CameraFragment newInstance() {
        return new CameraFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cameraButton = view.findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.i(TAG, "clicked on camera button");

                // getting a file reference
                photoFile = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_image_" + System.currentTimeMillis() + ".png");

                // wrapping File object into a content provider
                Uri fileProvider = getUriForFile(getContext(), getString(R.string.fileprovider_authority), photoFile);

                // launch intent to open camera
                cameraLauncher.launch(fileProvider);

                Log.i(TAG, "launched camera");
            }
        });
    }
}