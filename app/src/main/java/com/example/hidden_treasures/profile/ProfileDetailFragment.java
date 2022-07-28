package com.example.hidden_treasures.profile;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.example.hidden_treasures.MarkerRoomDB.MarkerEntity;
import com.example.hidden_treasures.R;

import java.io.Serializable;
import java.util.List;

public class ProfileDetailFragment extends Fragment {

    private static final String MARKERS = "markers";

    private List<MarkerEntity> markers;
    public GridAdapter adapter;
    RecyclerView gridView;
    ImageButton backButton;

    public ProfileDetailFragment() {
        // Required empty public constructor
    }

    public static ProfileDetailFragment newInstance(List<MarkerEntity> markers) {
        ProfileDetailFragment fragment = new ProfileDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(MARKERS, (Serializable) markers);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            markers = (List<MarkerEntity>) getArguments().getSerializable(MARKERS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gridView = view.findViewById(R.id.gridView);
        backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getParentFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    // get the previous fragment (map fragment)
                    fm.popBackStackImmediate();
                } else {
                    Log.i("ProfileDetail", "no fragment to go back to");
                }
            }
        });

        adapter = new GridAdapter(getContext(), markers);
        gridView.setAdapter(adapter);
        gridView.setLayoutManager(new GridLayoutManager(getContext(), 3));
    }
}