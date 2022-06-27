package com.example.hidden_treasures;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.w3c.dom.Text;


public class MarkerDetailFragment extends Fragment {

    private static final String MEDIA_URL = "mediaUrl";
    private static final String PLACE_NAME = "placeName";
    private static final String PLACE_DESCRIPTION = "placeDescription";

    private String mediaUrl;
    private String placeName;
    private String placeDescription;

    public MarkerDetailFragment() {
        // Required empty public constructor
    }



    public static MarkerDetailFragment newInstance(String mediaUrl, String placeName, String placeDescription) {
        MarkerDetailFragment fragment = new MarkerDetailFragment();
        Bundle args = new Bundle();
        args.putString(MEDIA_URL, mediaUrl);
        args.putString(PLACE_NAME, placeName);
        args.putString(PLACE_DESCRIPTION, placeDescription);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mediaUrl = getArguments().getString(MEDIA_URL);
            placeName = getArguments().getString(PLACE_NAME);
            placeDescription = getArguments().getString(PLACE_DESCRIPTION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_marker_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}