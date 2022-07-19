package com.example.hidden_treasures.map;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.bumptech.glide.Glide;
import com.example.hidden_treasures.R;
import com.example.hidden_treasures.util.onSwipeTouchListener;

import java.net.URL;
import java.util.Date;


public class MarkerDetailFragment extends Fragment {

    private static final String TAG = "MarkerDetailFragment";

    private AmazonS3Client s3Client;

    private static final String MEDIA_KEY = "mediaUrl";
    private static final String TITLE = "title";
    private static final String VIEW_COUNT = "viewCount";
    private static final String DATE = "date";

    private String mediaKey;
    private String title;
    private int viewCount;
    private Date date;

    private Button btnCloseMarker;

    public MarkerDetailFragment() {
        // Required empty public constructor
    }


    /* a url for image and the title for marker are passed as arguments from Map fragment */
    public static MarkerDetailFragment newInstance(String mediaKey, String title, int viewCount, Date date) {
        MarkerDetailFragment fragment = new MarkerDetailFragment();
        Bundle args = new Bundle();
        args.putString(MEDIA_KEY, mediaKey);
        args.putString(TITLE, title);
        args.putInt(VIEW_COUNT, viewCount);
        args.putSerializable(DATE, date);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mediaKey = getArguments().getString(MEDIA_KEY);
            title = getArguments().getString(TITLE);
            viewCount = getArguments().getInt(VIEW_COUNT);
            date = (Date) getArguments().getSerializable(DATE);
            Log.i(TAG, "data: " + viewCount + " " + date);
        }
        BasicAWSCredentials credentials = new BasicAWSCredentials(getString(R.string.aws_accessID), getString(R.string.aws_secret_key));
        s3Client = new AmazonS3Client(credentials);
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
        getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.GONE);

        // get references to views in marker detail layout
        TextView tvPlaceName = view.findViewById(R.id.tvPlaceName);
        ImageView ivMarkerDetail = view.findViewById(R.id.ivMarkerDetail);
        btnCloseMarker = view.findViewById(R.id.btnCloseMarker);

        // set the values to the views
        tvPlaceName.setText(title);

        // get a signed url for the image
        String url = getSignedUrl(mediaKey).toString();
        Glide.with(getContext()).load(url).into(ivMarkerDetail);

        // set onClick listeners for any buttons
        setOnClickListeners();
        // set swipe listener for swiping down
        setSwipeListener(view);
    }

    // generates a signed url to access the image in s3
    private URL getSignedUrl(String key) {
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(getString(R.string.s3_bucket), key)
                        .withMethod(HttpMethod.GET);
        return s3Client.generatePresignedUrl(generatePresignedUrlRequest);
    }

    /* When user swipes down, the marker detail is removed and returns to map fragment */
    private void setSwipeListener(@NonNull View view) {
        // set a touch listener to close the marker detail view when the user swipes down
        view.setOnTouchListener(new onSwipeTouchListener(getContext()) {

            @Override
            public void onSwipeDown() {
                super.onSwipeDown();
                backToMap();
            }
        });
    }

    /* Sets onCLick listeners for buttons */
    private void setOnClickListeners() {
        // button to go back to map fragment
        btnCloseMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                backToMap();
            }
        });
    }

    /* returns to previous map fragment*/
    private void backToMap() {
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
}