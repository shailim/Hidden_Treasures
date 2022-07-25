package com.example.hidden_treasures.markers;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.bumptech.glide.Glide;
import com.example.hidden_treasures.R;
import com.example.hidden_treasures.util.onSwipeTouchListener;
import com.google.android.gms.maps.model.Marker;


import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MarkerDetailFragment extends Fragment {

    private static final String TAG = "MarkerDetailFragment";

    private AmazonS3Client s3Client;

    private static final String MEDIA_KEY = "mediaUrl";
    private static final String TITLE = "title";
    private static final String VIEW_COUNT = "viewCount";
    private static final String DATE = "date";
    private static final String LIST = "list";

    private String mediaKey;
    private String title;
    private int viewCount;
    private Date date;
    private List<Marker> list;

    private int curPos = 0;

    private ImageView ivMarkerDetail;
    private TextView tvTitle;
    private TextView tvDate;

    public MarkerDetailFragment() {
        // Required empty public constructor
    }


    /* a url for image and the title for marker are passed as arguments from Map fragment */
    public static MarkerDetailFragment newInstance(String mediaKey, String title, int viewCount, Date date, List<Marker> list) {
        MarkerDetailFragment fragment = new MarkerDetailFragment();
        Bundle args = new Bundle();
        args.putString(MEDIA_KEY, mediaKey);
        args.putString(TITLE, title);
        args.putInt(VIEW_COUNT, viewCount);
        args.putSerializable(DATE, date);
        args.putSerializable(LIST, (Serializable) list);
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
            list = (ArrayList) getArguments().getSerializable(LIST);
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
        ivMarkerDetail = view.findViewById(R.id.ivMarkerDetail);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvDate = view.findViewById(R.id.tvDate);

        if (list.size() == 0) {
            // set the values to the views
            tvTitle.setText(title);
            tvDate.setText(calculateTimeAgo(date));

            // get a signed url for the image
            String url = getSignedUrl(mediaKey).toString();
            Glide.with(getContext()).load(url).into(ivMarkerDetail);
        } else {
            // showing the first of the markers in the list
            MarkerData data = (MarkerData) list.get(0).getTag();
            tvTitle.setText(list.get(0).getTitle());
            tvDate.setText(calculateTimeAgo(data.getDate()));

            // get a signed url for the image
            String url = getSignedUrl(data.getImageKey()).toString();
            Glide.with(getContext()).load(url).into(ivMarkerDetail);

        }
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

            @Override
            public void onSwipeLeft() {
                super.onSwipeLeft();
                // replace data with next marker
                if (list.size() > 0 && list.get(curPos + 1) != null) {
                    MarkerData data = (MarkerData) list.get(curPos+1).getTag();
                    tvTitle.setText(list.get(curPos+1).getTitle());
                    tvDate.setText(calculateTimeAgo(data.getDate()));

                    // get a signed url for the image
                    String url = getSignedUrl(data.getImageKey()).toString();
                    Glide.with(getContext()).load(url).into(ivMarkerDetail);
                    curPos++;
                }
            }

            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
                // replace data with previous marker
                if (list.size() > 0 && list.get(curPos - 1) != null) {
                    MarkerData data = (MarkerData) list.get(curPos-1).getTag();
                    tvTitle.setText(list.get(curPos-1).getTitle());
                    tvDate.setText(calculateTimeAgo(data.getDate()));

                    // get a signed url for the image
                    String url = getSignedUrl(data.getImageKey()).toString();
                    Glide.with(getContext()).load(url).into(ivMarkerDetail);
                    curPos--;
                }
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

    public static String calculateTimeAgo(Date createdAt) {

        int SECOND_MILLIS = 1000;
        int MINUTE_MILLIS = 60 * SECOND_MILLIS;
        int HOUR_MILLIS = 60 * MINUTE_MILLIS;
        int DAY_MILLIS = 24 * HOUR_MILLIS;

        try {
            createdAt.getTime();
            long time = createdAt.getTime();
            long now = System.currentTimeMillis();

            final long diff = now - time;
            if (diff < MINUTE_MILLIS) {
                return "just now";
            } else if (diff < 2 * MINUTE_MILLIS) {
                return "a minute ago";
            } else if (diff < 50 * MINUTE_MILLIS) {
                return diff / MINUTE_MILLIS + " m";
            } else if (diff < 90 * MINUTE_MILLIS) {
                return "an hour ago";
            } else if (diff < 24 * HOUR_MILLIS) {
                return diff / HOUR_MILLIS + " h";
            } else if (diff < 48 * HOUR_MILLIS) {
                return "yesterday";
            } else {
                return diff / DAY_MILLIS + " d";
            }
        } catch (Exception e) {
            Log.i("Error:", "getRelativeTimeAgo failed", e);
            e.printStackTrace();
        }

        return "";
    }
}