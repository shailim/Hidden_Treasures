package com.example.hidden_treasures.profile;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.example.hidden_treasures.MarkerRoomDB.AppDatabase;
import com.example.hidden_treasures.MarkerRoomDB.MarkerEntity;
import com.example.hidden_treasures.MarkerRoomDB.MarkerViewModel;
import com.example.hidden_treasures.createMarker.NewMarkerEvent;
import com.example.hidden_treasures.login.LoginActivity;
import com.example.hidden_treasures.markers.MarkerDetailFragment;
import com.example.hidden_treasures.markers.ParseMarker;
import com.example.hidden_treasures.R;
import com.parse.ParseUser;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    public static final String TAG = "ProfileFragment";

    TextView tvUsername;
    RecyclerView gridView;
    LottieAnimationView logoutBtn;
    List<MarkerEntity> markers = new ArrayList<>();
    GridAdapter adapter;

    private MarkerViewModel viewModel;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance() {
        ProfileFragment fragment = new ProfileFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MarkerViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvUsername = view.findViewById(R.id.tvUsername);
        gridView = view.findViewById(R.id.gridView);
        logoutBtn = view.findViewById(R.id.logoutBtn);

        tvUsername.setText(ParseUser.getCurrentUser().getUsername());

        // get the user's markers
        queryMarkers();

        adapter =  new GridAdapter(getContext(), markers, getString(R.string.aws_accessID), getString(R.string.aws_secret_key), getString(R.string.s3_bucket));
        gridView.setAdapter(adapter);
        gridView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        // set onClickListeners for any buttons
        setOnClickListeners();
    }


    @Override
    public void onStart() {
        super.onStart();
        // register to the event bus
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        // unregister from the event bus
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    /* Retrieves all the com.example.hidden_treasures.markers the user created in descending chronological order */
    public void queryMarkers() {
        // get from cache, not from server
        viewModel.getUserMarkers().observe(getViewLifecycleOwner(), list -> {
            markers.addAll(list);
            adapter.notifyDataSetChanged();
        });
    }

    /* sets onClickListeners for any buttons */
    public void setOnClickListeners() {
        // Log out button
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ParseUser.logOutInBackground();
                startActivity(new Intent(getContext(), LoginActivity.class));
                getActivity().finish();
            }
        });
    }

    /* subscribe for event when user creates a new marker */
    @Subscribe
    public void onNewMarkerEvent(NewMarkerEvent event) {
        Log.i(TAG, "onNewmarkerEvent");
        // store created markers in cache
        markers.add(0, storeInCache(event.marker));
        adapter.notifyItemInserted(0);
        Log.i(TAG, "added new marker to list in profile");

    }

    // stores created marker in cache
    private MarkerEntity storeInCache(ParseMarker marker) {
        try {
            MarkerEntity newMarker = new MarkerEntity(marker.getObjectId(), marker.getTime(), marker.getTitle(), marker.getLocation().getLatitude(), marker.getLocation().getLongitude(), marker.getImage(), marker.getCreatedBy(), marker.getViewCount(), marker.getScore());
            AppDatabase.databaseWriteExecutor.execute(() -> {
                viewModel.insertMarker(newMarker);
            });
            return newMarker;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Subscribe
    public void onImageClick(ImageClickEvent event) {
        // to set marker detail as a child fragment
        FragmentManager childFragMan = getChildFragmentManager();
        FragmentTransaction childFragTrans = childFragMan.beginTransaction();

        // create a new marker detail fragment instance and pass in image url, title
        MarkerDetailFragment markerDetailFrag = MarkerDetailFragment.newInstance(event.imageKey, event.title, event.viewCount, event.date, new ArrayList<>());
        // add the child fragment to current profile fragment
        childFragTrans.add(R.id.profileFragmentLayout, markerDetailFrag);
        childFragTrans.addToBackStack(null);
        childFragTrans.commit();
    }
}