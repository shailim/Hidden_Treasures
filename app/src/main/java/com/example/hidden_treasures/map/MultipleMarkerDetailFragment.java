package com.example.hidden_treasures.map;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.hidden_treasures.R;
import com.example.hidden_treasures.markers.MarkerData;
import com.example.hidden_treasures.util.onSwipeTouchListener;
import com.google.android.gms.maps.model.Marker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MultipleMarkerDetailFragment extends Fragment {

    private static final String TAG = "MultipleMarker";

    private static final String LIST = "list";

    private List<Marker> list = new ArrayList<>();
    private int curPosition = 0;

    public MultipleMarkerDetailFragment() {
        // Required empty public constructor
    }

    public static MultipleMarkerDetailFragment newInstance(List<Marker> markers) {
        MultipleMarkerDetailFragment fragment = new MultipleMarkerDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(LIST, (Serializable) markers);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            list = (List<Marker>) getArguments().getSerializable(LIST);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_multiple_marker_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (list.get(0) != null) {
            MarkerData data = (MarkerData) list.get(0).getTag();
            FragmentManager childFragMan = getChildFragmentManager();
            FragmentTransaction childFragTrans = childFragMan.beginTransaction();

            // create a new marker detail fragment instance and pass in image url, title
            MarkerDetailFragment markerDetailFrag = MarkerDetailFragment.newInstance(data.getImageKey(), list.get(0).getTitle(), data.getViewCount(), data.getDate());
            // add the child fragment to current map fragment
            childFragTrans.add(R.id.multipleMarkerFragment, markerDetailFrag);
            childFragTrans.addToBackStack(null);
            childFragTrans.commit();
        }
    }

    /* When user swipes right, the next marker's detail view is shown
     * When user swipes left, previous marker's detail view is shown*/
    private void setSwipeListener(@NonNull View view) {
        view.setOnTouchListener(new onSwipeTouchListener(getContext()) {
            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
                // go to next marker in list and show a detail view for it
            }

            @Override
            public void onSwipeLeft() {
                super.onSwipeLeft();
                // go to the previous marker in list
            }

            @Override
            public void onSwipeDown() {
                super.onSwipeDown();
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