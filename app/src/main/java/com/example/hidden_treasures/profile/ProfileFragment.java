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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.bumptech.glide.Glide;
import com.example.hidden_treasures.MarkerRoomDB.AppDatabase;
import com.example.hidden_treasures.MarkerRoomDB.MarkerEntity;
import com.example.hidden_treasures.MarkerRoomDB.MarkerViewModel;
import com.example.hidden_treasures.collections.ParseCollection;
import com.example.hidden_treasures.createMarker.NewMarkerEvent;
import com.example.hidden_treasures.login.LoginActivity;
import com.example.hidden_treasures.markers.MarkerDetailFragment;
import com.example.hidden_treasures.markers.ParseMarker;
import com.example.hidden_treasures.R;
import com.example.hidden_treasures.util.S3Helper;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProfileFragment extends Fragment {

    public static final String TAG = "ProfileFragment";

    TextView tvUsername;
    LottieAnimationView logoutBtn;
    Button seeMyMarkers;
    Button seeCollection;
    List<MarkerEntity> markers = new ArrayList<>();
    List<ParseMarker> collection = new ArrayList<>();
    GridAdapter adapter;

    // holds the image views for the siz profile and collection markers
    ImageView[] imagesViews = new ImageView[6];

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
        logoutBtn = view.findViewById(R.id.logoutBtn);
        seeMyMarkers = view.findViewById(R.id.seeMyMarkers);
        seeCollection = view.findViewById(R.id.seeMyCollection);

        imagesViews[0] = view.findViewById(R.id.ivMarkerImage1);
        imagesViews[1] = view.findViewById(R.id.ivMarkerImage2);
        imagesViews[2] = view.findViewById(R.id.ivMarkerImage3);
        imagesViews[3] = view.findViewById(R.id.ivCollectionImage1);
        imagesViews[4] = view.findViewById(R.id.ivCollectionImage2);
        imagesViews[5] = view.findViewById(R.id.ivCollectionImage3);

        tvUsername.setText(ParseUser.getCurrentUser().getUsername());

        // get the user's markers
        queryMarkers();
        //query the user's collection
        queryCollection();


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
            markers.clear();
            markers.addAll(list);
            setFirstThreeProfileMarkers();
        });
    }

    // Retrieves markers in user's collection in descending chronological order
    public void queryCollection() {
        ParseQuery<ParseCollection> query = ParseQuery.getQuery(ParseCollection.class);
        query.whereEqualTo("userId", ParseUser.getCurrentUser().getObjectId());
        query.orderByDescending("created_at");
        query.findInBackground(new FindCallback<ParseCollection>() {
            @Override
            public void done(List<ParseCollection> objects, ParseException e) {
                if (e == null) {
                    for (ParseCollection collectionObject : objects) {
                        collection.add(collectionObject.getMarker());
                    }
                    setFirstThreeCollectionMarkers();
                }
            }
        });
    }

    // sets the image view for the first three profile markers
    public void setFirstThreeProfileMarkers() {
        for (int i = 0; i < 3 && i < markers.size(); i++) {
            String imageKey = markers.get(i).imageKey;
            // get a signed url for the image
            String url = S3Helper.getSignedUrl(getContext(), imageKey).toString();
            Glide.with(getContext()).load(url).into(imagesViews[i]);
            setMarkerClickListener(imagesViews[i], markers.get(i).imageKey, markers.get(i).title, markers.get(i).view_count, markers.get(i).createdAt);
        }
    }

    // sets the image view for the first three collection markers
    public void setFirstThreeCollectionMarkers() {
        for (int i = 3; i < 6 && i < collection.size() + 3; i++) {
            String imageKey = null;
            try {
                imageKey = ((ParseMarker) collection.get(i-3).fetchIfNeeded()).getImage();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            // get a signed url for the image
            String url = S3Helper.getSignedUrl(getContext(), imageKey).toString();
            Glide.with(getContext()).load(url).into(imagesViews[i]);
            setMarkerClickListener(imagesViews[i], collection.get(i-3).getImage(), collection.get(i-3).getTitle(), collection.get(i-3).getViewCount(), collection.get(i-3).getTime());
        }
    }

    // sets the onclick Listener for the image views
    public void setMarkerClickListener(ImageView iv, String imageKey, String title, int views, long createdAt) {
        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // posting new marker event
                EventBus.getDefault().post(new ImageClickEvent(imageKey, title, views, new Date(createdAt)));
            }
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

        seeMyMarkers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // go to profile detail fragment
                FragmentManager childFragMan = getChildFragmentManager();
                FragmentTransaction childFragTrans = childFragMan.beginTransaction();

                // create a new marker detail fragment instance and pass in image url, title
                ProfileDetailFragment profileDetailFragment = ProfileDetailFragment.newInstance(markers);
                // add the child fragment to current profile fragment
                childFragTrans.add(R.id.profileFragmentLayout, profileDetailFragment);
                childFragTrans.addToBackStack(null);
                childFragTrans.commit();
            }
        });

        seeCollection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // go to collection detail fragment
                FragmentManager childFragMan = getChildFragmentManager();
                FragmentTransaction childFragTrans = childFragMan.beginTransaction();

                // create a new marker detail fragment instance and pass in image url, title
                CollectionDetailFragment collectionDetailFragment = CollectionDetailFragment.newInstance(collection);
                // add the child fragment to current profile fragment
                childFragTrans.add(R.id.profileFragmentLayout, collectionDetailFragment);
                childFragTrans.addToBackStack(null);
                childFragTrans.commit();
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
        MarkerDetailFragment markerDetailFrag = MarkerDetailFragment.newInstance(null, event.imageKey, event.title, event.viewCount, event.date, new ArrayList<>());
        // add the child fragment to current profile fragment
        childFragTrans.add(R.id.profileFragmentLayout, markerDetailFrag);
        childFragTrans.addToBackStack(null);
        childFragTrans.commit();
    }
}