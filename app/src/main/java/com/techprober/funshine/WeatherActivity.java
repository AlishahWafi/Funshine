package com.techprober.funshine;

import android.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.techprober.funshine.model.DailyWeatherReport;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class WeatherActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, LocationListener {

    final String URL_BASE = "http://api.openweathermap.org/data/2.5/forecast";
    final String URL_COORD = "?lat=";
    final String URL_UNITS = "&units=metric";
    final String API_KEY = "&APPID=7127ba644a3ef6ad20d80eecbe583ca0";

    private GoogleApiClient mGoogleApiClient;
    private final int PERMISSION_LOCATION = 111;
    private ArrayList<DailyWeatherReport> weatherReportList = new ArrayList<>();

    private ImageView weatherIcon;
    private ImageView weatherIconMini;
    private TextView weatherDate;
    private TextView currentTemp;
    private TextView lowTemp;
    private TextView cityCountry;
    private TextView weatherDescription;

    WeatherAdapter mWeatherAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.content_weather_reports);

        mWeatherAdapter = new WeatherAdapter(weatherReportList);

        recyclerView.setAdapter(mWeatherAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        recyclerView.setLayoutManager(layoutManager);

        weatherIcon = (ImageView)findViewById(R.id.weatherIcon);
        weatherIconMini = (ImageView)findViewById(R.id.weatherIconMini);
        weatherDate = (TextView)findViewById(R.id.weatherDate);
        currentTemp = (TextView)findViewById(R.id.currentTemp);
        lowTemp = (TextView)findViewById(R.id.lowTemp);
        cityCountry = (TextView)findViewById(R.id.cityCountry);
        weatherDescription = (TextView)findViewById(R.id.weatherDescription);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public void downloadWeatherData(Location location) {
        final String fullCoords = URL_COORD + location.getLatitude() + "&lon=" + location.getLongitude();
        final String url = URL_BASE + fullCoords + URL_UNITS + API_KEY;

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {

                    Log.v("Response", "RES" + response);

                    JSONObject city = response.getJSONObject("city");
                    String cityName = city.getString("name");
                    String country = city.getString("country");

                    JSONArray list = response.getJSONArray("list");

                    for (int x = 0; x < 5; x++){
                        JSONObject obj = list.getJSONObject(x);
                        JSONObject main = obj.getJSONObject("main");
                        Double currentTemp = main.getDouble("temp");
                        Double maxTemp = main.getDouble("temp_max");
                        Double minTemp = main.getDouble("temp_min");

                        JSONArray weatherArr = obj.getJSONArray("weather");
                        JSONObject weather = weatherArr.getJSONObject(0);
                        String weatherType = weather.getString("main");

                        String rawDate = obj.getString("dt_txt");

                        DailyWeatherReport report = new DailyWeatherReport(cityName, rawDate, country, currentTemp.intValue(), maxTemp.intValue(), minTemp.intValue(), weatherType);

                        weatherReportList.add(report);
                    }

                } catch (Exception e){
                    Log.v("ERR", "Error: "+e.getLocalizedMessage());
                }

                updateUI();
                mWeatherAdapter.notifyDataSetChanged();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.v("FUN", "Err: " + error.getLocalizedMessage());
            }
        });

        Volley.newRequestQueue(this).add(jsonRequest);
    }

    public void updateUI() {

        if(weatherReportList.size() > 0){
            DailyWeatherReport report = weatherReportList.get(0);

            switch (report.getWeather()){

                case DailyWeatherReport.WEATHER_TYPE_CLOUDS:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
                    break;

                case DailyWeatherReport.WEATHER_TYPE_RAIN:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.rainy));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.rainy));
                    break;

                case DailyWeatherReport.WEATHER_TYPE_SNOW:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.snow));
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.snow));
                    break;

                default:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
            }

            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy");
            String formattedDate = df.format(c.getTime());

            weatherDate.setText(report.getFormattedDate());
            currentTemp.setText(Integer.toString(report.getCurrentTemp())+"°c");
            lowTemp.setText(Integer.toString(report.getMinTemp())+"°c");
            cityCountry.setText(report.getCityName() + ", " + report.getCountry());
            weatherDescription.setText(report.getWeather());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        downloadWeatherData(location);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION);
        } else {
            startLocatioServices();
        }

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    public void startLocatioServices(){
        try {
            LocationRequest req = LocationRequest.create().setPriority(LocationRequest.PRIORITY_LOW_POWER);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, req, this);
        } catch (SecurityException exception){

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_LOCATION: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocatioServices();
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public class WeatherAdapter extends RecyclerView.Adapter<WeatherReportViewHolder>{

        private ArrayList<DailyWeatherReport> mDailyWeatherReports;

        public WeatherAdapter(ArrayList<DailyWeatherReport> mDailyWeatherReports) {
            this.mDailyWeatherReports = mDailyWeatherReports;
        }

        @Override
        public void onBindViewHolder(WeatherReportViewHolder holder, int position) {
            DailyWeatherReport report = mDailyWeatherReports.get(position);
            holder.updateUI(report);
        }

        @Override
        public int getItemCount() {
            return mDailyWeatherReports.size();
        }

        @Override
        public WeatherReportViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_weather, parent, false);

            return new WeatherReportViewHolder(card);
        }
    }

    public class WeatherReportViewHolder extends RecyclerView.ViewHolder {

        private ImageView weatherIcon;
        private TextView weatherDescription;
        private TextView weatherDate;
        private TextView tempHigh;
        private TextView tempLow;

        public WeatherReportViewHolder(View itemView) {
            super(itemView);

            weatherIcon = (ImageView)itemView.findViewById(R.id.weather_Icon);
            weatherDate = (TextView)itemView.findViewById(R.id.weather_day);
            weatherDescription = (TextView)itemView.findViewById(R.id.weather_description);
            tempHigh = (TextView)itemView.findViewById(R.id.weather_temp_high);
            tempLow = (TextView)itemView.findViewById(R.id.weather_temp_low);
        }

        public void updateUI(DailyWeatherReport report) {

            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy");
            String formattedDate = df.format(c.getTime());

            weatherDate.setText(report.getFormattedDate());
            weatherDescription.setText(report.getWeather());
            tempHigh.setText(Integer.toString(report.getMaxTemp())+"°c");
            tempLow.setText(Integer.toString(report.getMinTemp())+"°c");

            switch (report.getWeather()){

                case DailyWeatherReport.WEATHER_TYPE_CLOUDS:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.cloudy_mini));
                    break;

                case DailyWeatherReport.WEATHER_TYPE_RAIN:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.rainy_mini));
                    break;

                case DailyWeatherReport.WEATHER_TYPE_SNOW:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.snow_mini));
                    break;

                default:
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.sunny_mini));
            }

        }
    }
}
