package com.example.hidden_treasures;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {


    private MapFragment mapFragment;
    private CreateFragment createFragment;
    private ProfileFragment profileFragment;

    // find the bottom navigation view
    public BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            mapFragment = MapFragment.newInstance();
            createFragment = CreateFragment.newInstance();
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

    public void switchTab(int id, String title, String description, Location location, String imageUrl) {
        bottomNavigationView.setSelectedItemId(id);
        displayMapFragment(title, description, location, imageUrl);
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
        if (createFragment.isAdded()) { ft.hide(createFragment); }
        // Hide profile fragment
        if (profileFragment.isAdded()) { ft.hide(profileFragment); }
        // Commit changes
        ft.commit();
    }

    /* shows map fragment and hides the other fragments */
    public void displayMapFragment(String title, String description, Location location, String imageUrl) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (mapFragment.isAdded()) { // if the fragment is already in container
            ft.show(mapFragment);
        } else { // fragment needs to be added to frame container
            ft.add(R.id.fragmentContainer, mapFragment);
        }
        mapFragment.addCreatedMarker(title, description, location, imageUrl);
        // Hide create fragment
        if (createFragment.isAdded()) { ft.hide(createFragment); }
        // Hide profile fragment
        if (profileFragment.isAdded()) { ft.hide(profileFragment); }
        // Commit changes
        ft.commit();
    }

    /* shows create fragment and hides the other fragments */
    public void displayCreateFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (createFragment.isAdded()) { // if the fragment is already in container
            ft.show(createFragment);
        } else { // fragment needs to be added to frame container
            ft.add(R.id.fragmentContainer, createFragment);
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
        if (createFragment.isAdded()) { ft.hide(createFragment); }
        // Commit changes
        ft.commit();
    }
}