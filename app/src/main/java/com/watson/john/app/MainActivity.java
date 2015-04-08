package com.watson.john.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.content.Context;
import android.widget.RelativeLayout;

import com.watson.john.app.MainActivityView;

import com.watson.john.app.R;

public class MainActivity extends Activity {
    private MainActivityView view; //displays and manages the game

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spot_on);

        //create a new SpotOnView and add it to the RelativeLayout
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.relativeLayout);

        //the getPreferences gets the default SharedPreferences file associated
        //with this Activity
        view = new MainActivityView(this,getPreferences(Context.MODE_PRIVATE),layout);
        layout.addView(view,0); //add view to the layout at position 0 - behind all other layouts
    }

    //called when this Activity moves to the background
    //does not save game state
    @Override
    public void onPause() {
        super.onPause();
        view.pause(); //release resources held by the view
    }

    //called when this Activity is brought to the foreground
    @Override
    public void onResume() {
        super.onResume();
        view.resume(this); //re-initialize resources released in onPause
    }
}














