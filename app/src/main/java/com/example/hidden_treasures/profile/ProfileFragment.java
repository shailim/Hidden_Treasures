package com.example.hidden_treasures.profile;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.hidden_treasures.MainActivity;
import com.example.hidden_treasures.createMarker.NewMarkerEvent;
import com.example.hidden_treasures.login.LoginActivity;
import com.example.hidden_treasures.models.ParseMarker;
import com.example.hidden_treasures.R;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    public static final String TAG = "ProfileFragment";

    TextView tvUsername;
    RecyclerView gridView;
    Button logoutBtn;
    List<ParseMarker> markers = new ArrayList<>();
    GridAdapter adapter;

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
        // if user is not logged in, redirect to log in page
        if (ParseUser.getCurrentUser() == null) {
            startActivity(new Intent(getContext(), LoginActivity.class));
        }
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

        // get the user's created markers
        queryMarkers();

        adapter =  new GridAdapter(getContext(), markers);
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

    /* Retrieves all the markers the user created in descending chronological order */
    public void queryMarkers() {
        ParseQuery<ParseMarker> query = ParseQuery.getQuery(ParseMarker.class);
        query.whereEqualTo("created_by", ParseUser.getCurrentUser());
        query.addDescendingOrder("createdAt");
        query.findInBackground(new FindCallback<ParseMarker>() {
            @Override
            public void done(List<ParseMarker> parsePosts, ParseException e) {
                if (parsePosts != null) {
                    markers.addAll(parsePosts);
                    adapter.notifyDataSetChanged();
                } else {
                    Log.e(TAG, "Couldn't query markers");
                }
            }
        });
    }

    /* sets onClickListeners for any buttons */
    public void setOnClickListeners() {
        // Log out button
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: redirect to map
                ParseUser.logOut();
            }
        });
    }

    /* subscribe for event when user creates a new marker */
    @Subscribe
    public void onNewMarkerEvent(NewMarkerEvent event) {
        markers.add(0, event.marker);
        adapter.notifyItemInserted(0);
        Log.i(TAG, "added new marker to list in profile");
    }
}