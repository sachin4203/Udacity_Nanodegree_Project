package com.sachinbak.moviezilla;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    static int activeId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (activeId == 0){
            activeId = R.id.action_sort_popularity;
        } else {
            menu.findItem(activeId).setChecked(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        MainActivityFragment fragment = MainActivityFragment.instance;

        if (id == R.id.action_sort_rating) {
            fragment.sortOrder = "vote_count.desc";
            fragment.moreParams = "vote_average.gte=7&vote_average.lte=10"; // you don't want shows with few votes
        } else if (id == R.id.action_sort_popularity) {
            fragment.sortOrder = "popularity.desc";
            fragment.moreParams = "";
        }
        item.setChecked(true);
        if (id == R.id.action_sort_popularity || id == R.id.action_sort_rating){
            fragment.updateUI(false);
            activeId = id;
        } else if (id == R.id.action_favorites){
            fragment.updateUI(true);
            activeId = id;
        }
        return super.onOptionsItemSelected(item);
    }
}
