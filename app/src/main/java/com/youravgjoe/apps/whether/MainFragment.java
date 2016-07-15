package com.youravgjoe.apps.whether;

import android.content.Context;
import java.text.SimpleDateFormat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Calendar;
import java.util.TimeZone;


public class MainFragment extends Fragment {
    private OnFragmentInteractionListener mListener;
    ListView mForecastList;
    ArrayAdapter<String> mForecastAdapter;
    TextView mLocationTextView;

    String mLocation;

    private static final String LOCATION_SETTING = "location";
    private static final String UNITS_SETTING = "units";
    private static final String CELSIUS = "Celsius";
    private static final String FAHRENHEIT = "Fahrenheit";
    private static final String DEFAULT_LOCATION = "London";
    private static final String ERROR_BAD_LOCATION = "Error retrieving data. Please try another city or zip code.";

    public MainFragment() {
        // Required empty public constructor
    }

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mForecastList = (ListView) rootView.findViewById(R.id.listview_forecast);
        mLocationTextView = (TextView) rootView.findViewById(R.id.location);

        if(readPref(LOCATION_SETTING).size() == 0) {
            mLocation = DEFAULT_LOCATION;
        } else {
            mLocation = readPref(LOCATION_SETTING).get(0);
        }
        String[] cityAndCountry = mLocation.split(",");
        if(cityAndCountry.length > 0)
          Log.d("cityAndCountry", cityAndCountry[0] + " " + cityAndCountry[1]);

        mLocationTextView.setText(String.format(getString(R.string.location_intro), mLocation));

        new GetWeatherTask().execute(cityAndCountry[0].trim());

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    public class GetWeatherTask extends AsyncTask<String, Void, List<String>> {

        @Override
        protected List<String> doInBackground(String... params) {

            String baseUrl = "http://api.openweathermap.org/data/2.5/forecast/?q=";

            String search = params[0];

            search = search.replace(" ", "%20");

            String endOfUrl = "&units=imperial&APPID=4f087bf7b1cdc161443d65c7be0feccd";

            String builtUri = baseUrl + search + endOfUrl;

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                URL url = new URL(builtUri);

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e("MainFragment", "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("MainFragment", "Error closing stream", e);
                    }
                }
            }

            // parse forecastJsonStr into forecastArray

            List<String> forecastList = new ArrayList<>();

            try {
                forecastList = formatForecastJsonForOutput(forecastJsonStr);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return forecastList;
        }

        @Override
        protected void onPostExecute(final List<String> forecastArray) {
            if(getActivity() != null && forecastArray != null) {
                mForecastAdapter = new ArrayAdapter<>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast, forecastArray);
                mForecastList.setAdapter(mForecastAdapter);
            } else {
                List<String> errorList = new ArrayList<>();
                errorList.add(ERROR_BAD_LOCATION);
                mForecastAdapter = new ArrayAdapter<>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast, errorList);
                mForecastList.setAdapter(mForecastAdapter);
            }

            mForecastList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    if(!TextUtils.equals(mForecastAdapter.getItem(position), ERROR_BAD_LOCATION)) {
                        Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
                        detailIntent.putExtra("detail", mForecastAdapter.getItem(position));
                        getActivity().startActivity(detailIntent);
                    }
                }
            });
        }
    }

    public List<String> formatForecastJsonForOutput(String forecastJson) throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "main";
        final String OWM_MAX = "temp_max";
        final String OWM_MIN = "temp_min";
        final String OWM_DESCRIPTION = "main";

        JSONObject weatherJson = new JSONObject(forecastJson);
        JSONArray weatherArray = weatherJson.getJSONArray(OWM_LIST);

        Time dayTime = new Time();
        dayTime.setToNow();

        // we start at the day returned by local time. Otherwise this is a mess.
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        // now we work exclusively in UTC
        dayTime = new Time();

        Calendar calendar = Calendar.getInstance();

        int numOfMeasurementsLeftToday = (24 - calendar.get(Calendar.HOUR_OF_DAY)) / 3;

        Log.d("left today", numOfMeasurementsLeftToday + "");

        List<String> resultStringList = new ArrayList<>();
        String todayHighAndLow;

        String today = "error";
        String todayDescription = "error";

        double todayMax = -1000;
        double todayMin = 1000;

        for (int i = 0; i < numOfMeasurementsLeftToday; i++) {

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this saturday".
            long dateTime;

            // Cheating to convert this to UTC time, which is what we want anyhow
            dateTime = dayTime.setJulianDay(julianStartDay);

            today = getReadableDateString(dateTime);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            todayDescription = weatherObject.getString(OWM_DESCRIPTION);

            Log.d("today count", i + "");

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);

            if(temperatureObject.getDouble(OWM_MAX) > todayMax) {
                todayMax = temperatureObject.getDouble(OWM_MAX);
                Log.d("MAX > todayMax", todayMax + "");
            }
            if(temperatureObject.getDouble(OWM_MIN) < todayMin) {
                todayMin = temperatureObject.getDouble((OWM_MIN));
                Log.d("MIN < todayMin", todayMin + "");
            }
        }

        todayHighAndLow = formatHighLows(todayMax, todayMin);
        resultStringList.add(today + " - " + todayDescription + " - " + todayHighAndLow);

        // For now, using the format "Day, description, hi/low"
        String day = "error getting day";
        String description = "error getting description";
        String highAndLow;
        double max = -1000;
        double min = 1000;

        for (int i = numOfMeasurementsLeftToday; i < weatherArray.length(); i++) {

            // Get the JSON object representing the day
            JSONObject dayForecast;

            for(int j = 0; j < 8; j++) {
                // Get the JSON object representing the day
                dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;

                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay + i / 8); // this 8 is the number of times it measures in a day.

                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);

                if(temperatureObject.getDouble(OWM_MAX) > max) {
                    max = temperatureObject.getDouble(OWM_MAX);
                }
                if(temperatureObject.getDouble(OWM_MIN) < min) {
                    min = temperatureObject.getDouble(OWM_MIN);
                }
            }

            // every 8 measurements, get the max and min and reset it for the next day
            if(i % 8 == 0) {
                highAndLow = formatHighLows(max, min);
                resultStringList.add(day + " - " + description + " - " + highAndLow);

                max = -1000;
                min = 1000;
            } else if (weatherArray.length() - i == 0) {
                highAndLow = formatHighLows(max, min);
                resultStringList.add(day + " - " + description + " - " + highAndLow);
            }
        }

        return resultStringList;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_forecast_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            Toast.makeText(getActivity(), "Refreshing weather...", Toast.LENGTH_SHORT).show();
            String[] cityAndCountry = mLocation.split(",");
            new GetWeatherTask().execute(cityAndCountry[0].trim());
            if(cityAndCountry.length > 0)
             Log.d("cityAndCountry", cityAndCountry[0] + " " + cityAndCountry[1]);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /* The date/time conversion code is going to be moved outside the asynctask later,
     * so for convenience we're breaking it out into its own method now.
     */
    private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {

        Log.d("formatHighLows", high + " / " + low);

        // For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "° / " + roundedLow + "°";
        return highLowStr;
    }

    public List<String> readPref(String prefName) {
        SharedPreferences sharedPreferences = this.getActivity().getSharedPreferences(prefName, Context.MODE_PRIVATE);
        HashSet<String> hashSet = (HashSet<String>) sharedPreferences.getStringSet(prefName, new HashSet<String>());
        return new ArrayList<>(hashSet);
    }
}
