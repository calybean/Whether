package com.youravgjoe.apps.whether;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

public class SettingsActivity extends AppCompatActivity {

    private static final String LOCATION_SETTING = "location";
    private static final String UNITS_SETTING = "units";
    private static final String CELSIUS = "Celsius";
    private static final String FAHRENHEIT = "Fahrenheit";
    private static final String DEFAULT_LOCATION = "London";

    private String mSearch;
    private String mLocation;
    private TextView mLocationTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final LinearLayout locationSetting = (LinearLayout) findViewById(R.id.location_setting);
        LinearLayout unitsSetting = (LinearLayout) findViewById(R.id.units_setting);
        mLocationTextView = (TextView) findViewById(R.id.location);
        final TextView units = (TextView) findViewById(R.id.units);

//        this.getSharedPreferences(UNITS_SETTING, 0).edit().clear().commit();

        if (readPref(UNITS_SETTING).size() == 0) {
            writePref(UNITS_SETTING, CELSIUS);
            units.setText(CELSIUS);
        } else {
            units.setText(readPref(UNITS_SETTING).get(0));
        }

        if (readPref(LOCATION_SETTING).size() == 0) {
            writePref(LOCATION_SETTING, DEFAULT_LOCATION);
            mLocationTextView.setText(DEFAULT_LOCATION);
        } else {
            mLocation = readPref(LOCATION_SETTING).get(0);
            mLocationTextView.setText(mLocation);
        }

        locationSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog.Builder locationDialog = new AlertDialog.Builder(SettingsActivity.this);
                locationDialog.setTitle("Set Location");
                locationDialog.setMessage("Type in a Zip Code:");

                final EditText locationEditText = new EditText(SettingsActivity.this);
                locationEditText.setSingleLine();
                locationEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

                float scale = getResources().getDisplayMetrics().density;
                int padding = (int) (16 * scale + 0.5f);

                locationDialog.setView(locationEditText, padding, 0, padding, 0);

                locationDialog.setPositiveButton("SET", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mSearch = locationEditText.getText().toString();
                        new ValidateLocationTask().execute(locationEditText.getText().toString());
                        dialog.dismiss();
                    }
                });
                locationDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                locationDialog.show();


//                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                imm.hideSoftInputFromWindow(locationEditText.getWindowToken(), 0);

//                if(isMapsInstalled()) {
//                    Uri mapIntentUri = Uri.parse("geo:0,0?q=" + mLocation);
//                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapIntentUri);
//                    mapIntent.setPackage("com.google.android.apps.maps");
//                    startActivity(mapIntent);
//                }
            }
        });

        unitsSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentUnits = readPref(UNITS_SETTING).get(0);

                if(TextUtils.equals(FAHRENHEIT, currentUnits)) {
                    currentUnits = CELSIUS;
                } else {
                    currentUnits = FAHRENHEIT;
                }
                writePref(UNITS_SETTING, currentUnits);
                units.setText(currentUnits);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
        Intent backIntent = new Intent(SettingsActivity.this, MainActivity.class);
        startActivity(backIntent);
    }

    public class ValidateLocationTask extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... params) {

            String baseUrl = "http://api.openweathermap.org/data/2.5/forecast/?q=";

            String search = params[0];

            search = search.replace(" ", "%20");

            String endOfUrl = "&units=metric&APPID=4f087bf7b1cdc161443d65c7be0feccd";

            String builtUri = baseUrl + search + endOfUrl;

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String weatherJsonString = null;

            try {
                URL url = new URL(builtUri);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }
                if (buffer.length() == 0) {
                    return null;
                }
                weatherJsonString = buffer.toString();
            } catch (IOException e) {
                Log.e("SettingsActivity", "Error ", e);
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("SettingsActivity", "Error closing stream", e);
                    }
                }
            }

            String[] locationInfo = new String[2];

            try {
                JSONObject weatherJson = new JSONObject(weatherJsonString);
                JSONObject locationJson = weatherJson.getJSONObject("city");
                locationInfo[0] = locationJson.getString("name"); // city name is in 0th spot
                locationInfo[1] = locationJson.getString("country"); // country name is in 1st spot

                if(TextUtils.equals(locationInfo[0], "")) {
                    locationInfo[0] = mSearch;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return locationInfo;
        }

        @Override
        protected void onPostExecute(final String[] locationInfo) {
            if(locationInfo != null) {
                if(TextUtils.equals(locationInfo[0], null) && TextUtils.equals(locationInfo[1], null)) {
                    Log.e("SettingsActivity", "Could not get location");
                    Toast.makeText(SettingsActivity.this, "Error: Invalid Location", Toast.LENGTH_LONG).show();
                    mLocation = null;
                } else {
                    mLocation = mSearch;
                    mLocationTextView.setText(mLocation);
                    writePref(LOCATION_SETTING, mLocation);

//                    mLocation = locationInfo[0] + ", " + locationInfo[1];
//                    writePref(LOCATION_SETTING, mLocation);
//                    mLocationTextView.setText(mLocation);
                }
            } else {
                // do nothing?
            }
        }
    }

    private boolean isMapsInstalled() {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo("com.google.android.apps.maps", PackageManager.GET_ACTIVITIES);
            return pm.getApplicationInfo("com.google.android.apps.maps", 0).enabled;
        }
        catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }


    public List<String> readPref(String prefName) {
        SharedPreferences sharedPreferences = getSharedPreferences(prefName, MODE_PRIVATE);
        HashSet<String> hashSet = (HashSet<String>) sharedPreferences.getStringSet(prefName, new HashSet<String>());
        return new ArrayList<>(hashSet);
    }

    public void writePref(String prefName, String value) {
        List<String> temp = new ArrayList<>();
        temp.add(value);
        HashSet<String> hashSet = new HashSet<>(temp);
        SharedPreferences sharedPreferences = getSharedPreferences(prefName, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        sharedPreferences.edit().clear().commit();
        editor.putStringSet(prefName, hashSet);
        editor.apply();
    }
}
