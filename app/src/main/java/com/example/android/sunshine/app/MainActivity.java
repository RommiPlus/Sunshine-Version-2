/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

public class MainActivity extends AppCompatActivity implements DetailFragment.OnClickItemChangedListener {

    public static final String DETAILFRAGMENT_TAG = "DF_TAG";
    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private String mLocation;
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mLocation = Utility.getPreferredLocation(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (findViewById(R.id.weather_detail_container) != null) {
            mTwoPane = true;

            if (getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG) == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.weather_detail_container, new DetailFragment(), DETAILFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
            getSupportActionBar().setElevation(0f);
        }

        ForecastFragment fragment = (ForecastFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
        fragment.setUseTodayLayout(!mTwoPane);

        SunshineSyncAdapter.initializeSyncAdapter(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        String location = Utility.getPreferredLocation(this);
        // update the location in our second pane using the fragment manager
        if (location != null && !location.equals(mLocation)) {
            ForecastFragment ff = (ForecastFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
            if (null != ff) {
                ff.onLocationChanged();
            }

            DetailFragment df = (DetailFragment) getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            if (null != df) {
                df.onLocationChanged(location);
            }
            mLocation = location;
        }
    }

    @Override
    public void onClickItem(Uri data, RecyclerView.ViewHolder viewHolder) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            DetailFragment fragment = (DetailFragment)
                    getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            fragment.setData(data);
        } else {
            ForecastAdapter.ForecastAdapterViewHolder
                    holder = (ForecastAdapter.ForecastAdapterViewHolder) viewHolder;
            Pair<View, String> pair = new Pair<>((View) holder.mIconView, getString(R.string.today_weather_info));
            ActivityOptionsCompat compat = ActivityOptionsCompat.makeSceneTransitionAnimation(this, pair);

            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            intent.setData(data);
            ActivityCompat.startActivity(this, intent, compat.toBundle());
        }
    }

    @Override
    public void onNewDataReady(Uri data, RecyclerView.ViewHolder viewHolder) {
        if (mTwoPane) {
            DetailFragment fragment = (DetailFragment)
                    getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            fragment.setData(data);
        } else {
            ForecastAdapter.ForecastAdapterViewHolder
                    holder = (ForecastAdapter.ForecastAdapterViewHolder) viewHolder;
            Pair<View, String> pair = new Pair<>((View) holder.mIconView, getString(R.string.today_weather_info));
            ActivityOptionsCompat compat = ActivityOptionsCompat.makeSceneTransitionAnimation(this, pair);

            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            intent.setData(data);
            ActivityCompat.startActivity(this, intent, compat.toBundle());
        }
    }
}
