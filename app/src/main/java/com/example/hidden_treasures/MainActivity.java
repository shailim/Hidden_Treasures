package com.example.hidden_treasures;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {


    private MapFragment mapFragment;
    private CameraFragment cameraFragment;
    private ProfileFragment profileFragment;

    // find the bottom navigation view
    public BottomNavigationView bottomNavigationView;

    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        GenerateTestData testData = new GenerateTestData();
//        try {
//            testData.getData(this);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            mapFragment = MapFragment.newInstance();
            cameraFragment = CameraFragment.newInstance();
            profileFragment = ProfileFragment.newInstance();
        }

        handleBottomNavSelection();
        // default tab is map
        bottomNavigationView.setSelectedItemId(R.id.action_map);
    }

    public void handleBottomNavSelection() {
        // handling navigation selection
        bottomNavigationView.setOnItemSelectedListener(
                new NavigationBarView.OnItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_create:
                                displayCreateFragment();
                                break;
                            case R.id.action_profile:
                                displayProfileFragment();
                                break;
                            default:
                                displayMapFragment();
                                break;
                        }
                        return true;
                    }
                });
    }

    public void switchTab(int id, String title, Location location, String imageUrl) {
        //set navbar to visible again
        bottomNavigationView.setVisibility(View.VISIBLE);
        //set selected tab to map
        bottomNavigationView.setSelectedItemId(id);
        displayMapFragment(title, location, imageUrl);
        handleBottomNavSelection();
    }

    /* shows map fragment and hides the other fragments */
    public void displayMapFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (mapFragment.isAdded()) { // if the fragment is already in container
            ft.show(mapFragment);
        } else { // fragment needs to be added to frame container
            ft.add(R.id.fragmentContainer, mapFragment);
        }
        // Hide create fragment
        if (cameraFragment.isAdded()) { ft.hide(cameraFragment); }
        // Hide profile fragment
        if (profileFragment.isAdded()) { ft.hide(profileFragment); }
        // Commit changes
        ft.commit();
    }

    /* shows map fragment and hides the other fragments */
    public void displayMapFragment(String title, Location location, String imageUrl) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (mapFragment.isAdded()) { // if the fragment is already in container
            ft.show(mapFragment);
        } else { // fragment needs to be added to frame container
            ft.add(R.id.fragmentContainer, mapFragment);
        }
        mapFragment.addCreatedMarker(title, location, imageUrl);
        // Hide create fragment
        if (cameraFragment.isAdded()) { ft.remove(cameraFragment); }
        // Hide profile fragment
        if (profileFragment.isAdded()) { ft.hide(profileFragment); }
        // Commit changes
        ft.commit();
    }

    /* shows create fragment and hides the other fragments */
    public void displayCreateFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (cameraFragment.isAdded()) { // if the fragment is already in container
            ft.remove(cameraFragment);
            //cameraFragment = CameraFragment.newInstance();
            ft.add(R.id.fragmentContainer, CameraFragment.newInstance());
            //ft.show(cameraFragment);
        } else { // fragment needs to be added to frame container
            ft.add(R.id.fragmentContainer, cameraFragment);
        }
        // Hide map fragment
        if (mapFragment.isAdded()) { ft.hide(mapFragment); }
        // Hide profile fragment
        if (profileFragment.isAdded()) { ft.hide(profileFragment); }
        // Commit changes
        ft.commit();
    }

    /* shows profile fragment and hides the other fragments */
    public void displayProfileFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (profileFragment.isAdded()) { // if the fragment is already in container
            ft.show(profileFragment);
        } else { // fragment needs to be added to frame container
            ft.add(R.id.fragmentContainer, profileFragment);
        }
        // Hide map fragment
        if (mapFragment.isAdded()) { ft.hide(mapFragment); }
        // Hide create fragment
        if (cameraFragment.isAdded()) { ft.remove(cameraFragment); }
        // Commit changes
        ft.commit();
    }
}