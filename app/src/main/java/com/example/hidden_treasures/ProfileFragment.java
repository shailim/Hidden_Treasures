package com.example.hidden_treasures;

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
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    public static final String TAG = "ProfileFragment";

    TextView tvUsername;
    RecyclerView gridView;
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // if user is not logged in, redirect to log in page
        if (ParseUser.getCurrentUser() == null) {
            startActivity(new Intent(getContext(), LoginActivity.class));
        }
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvUsername = view.findViewById(R.id.tvUsername);
        gridView = view.findViewById(R.id.gridView);

        tvUsername.setText(ParseUser.getCurrentUser().getUsername());

        queryMarkers();

        adapter =  new GridAdapter(getContext(), markers);
        gridView.setAdapter(adapter);
        gridView.setLayoutManager(new GridLayoutManager(getContext(), 3));
    }

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
}