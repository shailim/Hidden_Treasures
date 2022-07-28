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
import com.example.hidden_treasures.markers.ParseMarker;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Callable;

public class CollectionDetailFragment extends Fragment {

    private static final String MARKERS = "markers";

    private List<ParseMarker> markers;
    public CollectionAdapter adapter;
    RecyclerView gridView;
    ImageButton backButton;

    public CollectionDetailFragment() {
        // Required empty public constructor
    }

    public static CollectionDetailFragment newInstance(List<ParseMarker> markers) {
        CollectionDetailFragment fragment = new CollectionDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(MARKERS, (Serializable) markers);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            markers = (List<ParseMarker>) getArguments().getSerializable(MARKERS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_collection_detail, container, false);
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
                    Log.i("CollectionDetail", "no fragment to go back to");
                }
            }
        });

        adapter = new CollectionAdapter(getContext(), markers);
        gridView.setAdapter(adapter);
        gridView.setLayoutManager(new GridLayoutManager(getContext(), 3));
    }
}