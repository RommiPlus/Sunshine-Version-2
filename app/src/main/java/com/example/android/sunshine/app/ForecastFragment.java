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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

import static com.example.android.sunshine.app.Utility.getPreferredLocation;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link ListView} layout.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int FORECAST_LOADER = 0;
    // For the forecast view we're showing only a small subset of the stored data.
    // Specify the columns we need.
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COl_ID = 0;
    static final int COL_WEATHER_ID = 1;
    static final int COL_WEATHER_DATE = 2;
    static final int COL_WEATHER_DESC = 3;
    static final int COL_WEATHER_MAX_TEMP = 4;
    static final int COL_WEATHER_MIN_TEMP = 5;
    static final int COL_LOCATION_SETTING = 6;
    static final int COL_WEATHER_CONDITION_ID = 7;
    static final int COL_COORD_LAT = 8;
    static final int COL_COORD_LONG = 9;

    private static final String SCROLL_POSITION = "SCROLL_POSITION";
    private static final String LOG_TAG = ForecastFragment.class.getSimpleName();

    private ForecastAdapter mForecastAdapter;
    private RecyclerView mRecyclerView;
    private int mPosition = RecyclerView.NO_POSITION;
    private boolean mIsUseTodayLayout;
    private boolean mIsFirstLoad = true;

    public ForecastFragment() {
    }

    public static final String TAG = ForecastFragment.class.getSimpleName();

    public void setUseTodayLayout(boolean isTrue) {
        mIsUseTodayLayout = isTrue;

        if (mForecastAdapter != null) {
            mForecastAdapter.setUseTodayLayout(isTrue);
        }
    }

    private ForecastAdapter.ForecastAdapterOnClickHandler
            mListener = new ForecastAdapter.ForecastAdapterOnClickHandler() {
        @Override
        public void onClick(long date, ForecastAdapter.ForecastAdapterViewHolder viewHolder) {
            // CursorAdapter returns a cursor at the correct position for getItem(), or null
            // if it cannot seek to that position.
            mPosition = viewHolder.getAdapterPosition();
            String locationSetting = getPreferredLocation(getActivity());

            Uri dataUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                    locationSetting, date);

            if (mIsFirstLoad) {
                ((DetailFragment.OnClickItemChangedListener) getActivity()).onNewDataReady(dataUri);
                mIsFirstLoad = false;
            } else {
                ((DetailFragment.OnClickItemChangedListener) getActivity()).onClickItem(dataUri);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_map) {
            openPreferredLocationInMap();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview_forecast);

        // Set the layout manager
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        TextView emptyView = (TextView) rootView.findViewById(R.id.recyclerview_forecast_empty);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // The CursorAdapter will take data from our cursor and populate the ListView.
        mForecastAdapter = new ForecastAdapter(getActivity(), null, mListener, emptyView);
        mForecastAdapter.setUseTodayLayout(mIsUseTodayLayout);

        // Get a reference to the ListView, and attach this adapter to it.
        mRecyclerView.setAdapter(mForecastAdapter);

        if (savedInstanceState != null && savedInstanceState.containsKey(SCROLL_POSITION)) {
            mPosition = savedInstanceState.getInt(SCROLL_POSITION);
            Log.v(TAG, "mPosition: " + mPosition);
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        setRetainInstance(true);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SCROLL_POSITION, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    // since we read the location when we create the loader, all we need to do is restart things
    void onLocationChanged() {
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }

    private void openPreferredLocationInMap() {
        if (null != mForecastAdapter) {
            Cursor cursor = mForecastAdapter.getCursor();

            if (null != cursor) {
                cursor.moveToPosition(0);
                String posLat = cursor.getString(COL_COORD_LAT);
                String posLong = cursor.getString(COL_COORD_LONG);

                // Using the URI scheme for showing a location found on a map.  This super-handy
                // intent can is detailed in the "Common Intents" page of Android's developer site:
                // http://developer.android.com/guide/components/intents-common.html#Maps
                Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(geoLocation);

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.d(LOG_TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!");
                }
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String locationSetting = getPreferredLocation(getActivity());

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());

        return new CursorLoader(getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mForecastAdapter.swapCursor(cursor)
        ;
        if (mPosition != RecyclerView.NO_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to restore
            // to, do so now.
            mRecyclerView.smoothScrollToPosition(mPosition);
        }

        updateEmptyView();
    }

    public void updateEmptyView() {
        if (mForecastAdapter.getItemCount() == 0) {
            TextView textView = (TextView) getView().findViewById(R.id.recyclerview_forecast_empty);
            if (textView != null) {
                int message = R.string.no_weather_info;
                @SunshineSyncAdapter.LocationStatus int location = Utility.getLocationStauts(getActivity());
                switch (location) {
                    case SunshineSyncAdapter.LOCATION_STATUS_SERVER_DOWN:
                        message = R.string.location_status_server_down;
                        break;
                    case SunshineSyncAdapter.LOCATION_STATUS_SERVER_INVALID:
                        message = R.string.location_status_server_invalid;
                        break;
                    case SunshineSyncAdapter.LOCATION_STATUS_INVALID:
                        message = R.string.empty_forecast_list_invalid_location;
                        break;
                    default:
                        if (!Utility.isNetworkConnected(getActivity())) {
                            message = R.string.network_error;
                        }
                }
                textView.setText(message);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mForecastAdapter.swapCursor(null);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_location_status))) {
            updateEmptyView();
        }
    }
}
