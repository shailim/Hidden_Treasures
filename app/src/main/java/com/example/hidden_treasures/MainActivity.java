package com.example.hidden_treasures;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // find the bottom navigation view
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // initialize the fragment manager
        final FragmentManager fragmentManager = getSupportFragmentManager();

        // handling navigation selection
        bottomNavigationView.setOnItemSelectedListener(
                new NavigationBarView.OnItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        Fragment fragment;
                        switch (item.getItemId()) {
                            case R.id.action_create:
                                fragment = CreateFragment.newInstance();
                                break;
                            case R.id.action_profile:
                                fragment = ProfileFragment.newInstance();
                                break;
                            default:
                                fragment = MapFragment.newInstance();
                                break;
                        }
                        // set the selected fragment to the fragment container
                        fragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment).addToBackStack(null).commit();
                        return true;
                    }
                });
        // default tab is map
        bottomNavigationView.setSelectedItemId(R.id.action_map);
    }
}