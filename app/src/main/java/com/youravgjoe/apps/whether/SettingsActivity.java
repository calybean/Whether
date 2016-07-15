package com.youravgjoe.apps.whether;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private static final String LOCATION_SETTING = "location";
    private static final String UNITS_SETTING = "units";
    private static final String CELSIUS = "Celsius";
    private static final String FAHRENHEIT = "Fahrenheit";
    private static final String DEFAULT_LOCATION = "London";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        LinearLayout locationSetting = (LinearLayout) findViewById(R.id.location_setting);
        LinearLayout unitsSetting = (LinearLayout) findViewById(R.id.units_setting);
        final TextView location = (TextView) findViewById(R.id.location);
        final TextView units = (TextView) findViewById(R.id.units);

//        this.getSharedPreferences(UNITS_SETTING, 0).edit().clear().commit();

        if (readPref(UNITS_SETTING).size() == 0) {
            writePref(UNITS_SETTING, FAHRENHEIT);
            units.setText(FAHRENHEIT);
        } else {
            units.setText(readPref(UNITS_SETTING).get(0));
        }

        if (readPref(LOCATION_SETTING).size() == 0) {
            writePref(LOCATION_SETTING, DEFAULT_LOCATION);
            location.setText(DEFAULT_LOCATION);
        } else {
            location.setText(readPref(LOCATION_SETTING).get(0));
        }

        locationSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog.Builder locationDialog = new AlertDialog.Builder(SettingsActivity.this);
                locationDialog.setTitle("Set Location");
                locationDialog.setMessage("Type in a City or a Zip Code:");

                final EditText locationEditText = new EditText(SettingsActivity.this);

                float scale = getResources().getDisplayMetrics().density;
                int padding = (int) (16 * scale + 0.5f);

                locationDialog.setView(locationEditText, padding, 0, padding, 0);

                locationDialog.setPositiveButton("SET", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        writePref(LOCATION_SETTING, locationEditText.getText().toString());
                        location.setText(locationEditText.getText().toString());
                        dialog.dismiss();
                    }
                });

                locationDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                locationDialog.show();
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
