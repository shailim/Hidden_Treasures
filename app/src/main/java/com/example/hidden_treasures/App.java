package com.example.hidden_treasures;

import android.app.Application;

import com.example.hidden_treasures.collections.ParseCollection;
import com.example.hidden_treasures.markers.ParseMarker;
import com.parse.Parse;
import com.parse.ParseObject;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        ParseObject.registerSubclass(ParseMarker.class);
        ParseObject.registerSubclass(ParseCollection.class);

        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.back4app_app_id))
                .clientKey(getString(R.string.back4app_client_key))
                .server(getString(R.string.back4app_server_url))
                .build());
    }
}

