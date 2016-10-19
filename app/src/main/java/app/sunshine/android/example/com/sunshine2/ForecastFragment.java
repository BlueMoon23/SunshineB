package app.sunshine.android.example.com.sunshine2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;


/**
 * Created by jonat on 11/09/2016.
 */
public class ForecastFragment extends Fragment {

    ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if(id==R.id.action_refresh){
            updateWeather();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        //The array adapter will take data from source and use it to populate the list it is attached to.
        mForecastAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                new ArrayList<String>());

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l){
                String forecast = mForecastAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), DetailActivity.class).putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(intent);
            }
        });

        return rootView;
    }

    private void updateWeather(){
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = prefs.getString(getString(R.string.pref_location_key),getString(R.string.pref_location_default));
        weatherTask.execute(location);
    }

    @Override
    public void onStart(){
        super.onStart();
        updateWeather();
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        private String getReadableDateString(long time){
            //Because the API returns a UNIX Stamp (measured in seconds),
            //it muse be converted to milliseconds in order to be connected to a valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        //Prepare the weather highs and lows for presentation.
        private String formatHighLows(double high, double low, String unitType){

                if (unitType.equals(getString(R.string.pref_units_imperial))){
                    high = (high * 1.8) + 32;
                    low = (low * 1.8) + 32;
                }else if(!unitType.equals(getString(R.string.pref_units_metric))){
                    Log.d(LOG_TAG, "Unit Type not found: " + unitType);
                }
                //Assume the user doesn't care about tenths of a degree.
                long roundedHigh = Math.round(high);
                long roundedLow = Math.round(low);

                String highLowStr = roundedHigh + "/" + roundedLow;
                return highLowStr;
            }

            /*Take the string representing the complete forecast in JSON format and
            * pull out the date we need to construct the strings for the wireframes.
            *
            * Fortunately parsing is easy: constructor takes the JSON string and converts it
            * into an object hierarchy for us.
            */

            private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException{
                //These are the names that need to be extracted.
                final String OWM_LIST="list";
                final String OWM_WEATHER="weather";
                final String OWM_TEMPERATURE="temp";
                final String OWM_MAX="max";
                final String OWM_MIN="min";
                final String OWM_DESCRIPTION="main";

                JSONObject forecastJson = new JSONObject(forecastJsonStr);
                JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

                /*OWM returns daily forecasts based upon the local time of the city that is
                * being asked for, which means that we need to know the GMT offset to translate this data properly.
                *
                * Since the data is also sent in order and the first date is always the current day, we're going
                * to take advantage of that to get a nice normalised UTC date for all of our weather.
                */

                Time dayTime = new Time();
                dayTime.setToNow();

                //We start at the day returned by the local time.  Otherwise this is a mess.
                int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

                dayTime =  new Time();

                String[] resultsStr = new String[numDays];

                /*Data is fetched in Celcius by default.
                * If users prefer to see in Fahrenheit, convert the values here.
                * We do this rather than fetching in Fahrenheit so that the user can
                * change this option without us having to refetch the data once
                * we start storing the values in a database.*/
                SharedPreferences sharedPrefs =
                        PreferenceManager.getDefaultSharedPreferences(getActivity());
                String unitType = sharedPrefs.getString(
                        getString(R.string.pref_units_key),
                        getString(R.string.pref_units_metric)
                );
                for(int i=0; i<weatherArray.length(); i++){
                    String day;
                    String description;
                    String highAndLow;



                //Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                //The date/time is returned as a long.  We need to convert that to something readable.
                long dateTime;

                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);



                //Description is in a child array called "weather" which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);


                //Temperatures are in a child element called "temp".
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low, unitType);
                resultsStr[i] = day + " - " + description + " - " + highAndLow;

            }

            for (String s : resultsStr){
                Log.v(LOG_TAG, "Forecast entry: " + s);
            }

            return resultsStr;
        }

        @Override
        protected String[] doInBackground(String... params) {

            //Check there is a post code
            if(params.length==0){
                return null;
            }

            //These two need to be declared outside the try/catch so that they can be closed in the final block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            //Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            String format="json";
            String units="metric";
            int numDays=7;


            try {
                //Construct the URL for the OpenWeatherMapQuery
                //Possible parameters are available at OWM's forecast API page at http://openweathermap.org/API#forecast
                final String FORECAST_BASE_URL="http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM="q";
                final String FORMAT_PARAM="mode";
                final String UNITS_PARAM="units";
                final String DAYS_PARAM="cnt";
                final String APPID_PARAM="APPID";

                Uri builtUri=Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                //Create the request to OpenWeatherMap, and open the connection

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                //Read the input string into a String.
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    //Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    //Since it's JSON adding a new line isn't really necessary, but it will make debugging easier.
                    buffer.append(line + 'n');
                }

                if (buffer.length() == 0) {
                    //Stream was empty, no point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();

                Log.v(LOG_TAG, "Forecast string: " + forecastJsonStr);

            } catch (IOException e) {
                Log.e("PlaceholderFragment", "Error", e);
                //If the code didn't properly get the data, there is no point in parsing it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            }catch (JSONException e){
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String[] result){
            if(result!=null){
                mForecastAdapter.clear();
                for(String dayForecastStr : result){
                    mForecastAdapter.add(dayForecastStr);
                }
            }
        }
    }
}
