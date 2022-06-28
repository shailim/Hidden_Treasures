package com.example.hidden_treasures;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.w3c.dom.Text;


public class MarkerDetailFragment extends Fragment {

    private static final String TAG = "MarkerDetailFragment";

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

        // don't show nav bar here
        getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.INVISIBLE);

        // get references to views in marker detail layout
        TextView tvPlaceName = view.findViewById(R.id.tvPlaceName);
        TextView tvPlaceDescription = view.findViewById(R.id.tvPlaceDescription);
        ImageView ivMarkerDetail = view.findViewById(R.id.ivMarkerDetail);
        Button btnCloseMarker = view.findViewById(R.id.btnCloseMarker);

        // set the values to the views
        tvPlaceName.setText(placeName);
        if (placeDescription != null) {
            tvPlaceDescription.setText(placeDescription);
        }
        Glide.with(getContext()).load(mediaUrl).into(ivMarkerDetail);

        // create a listener for the close marker detail button to go back to map fragment
        btnCloseMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // show the navbar again
                getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);

                FragmentManager fm = getParentFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    // get the previous fragment (map fragment)
                    fm.popBackStackImmediate();
                } else {
                    Log.i(TAG, "no fragment to go back to");
                }
            }
        });

        // set a touch listener to close the marker detail view when the user swipes down
        view.setOnTouchListener(new onSwipeTouchListener(getContext()) {

            @Override
            public void onSwipeDown() {
                super.onSwipeDown();
                // show the navbar again
                getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);

                FragmentManager fm = getParentFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    // get the previous fragment (map fragment)
                    fm.popBackStackImmediate();
                } else {
                    Log.i(TAG, "no fragment to go back to");
                }
            }
        });
    }
}