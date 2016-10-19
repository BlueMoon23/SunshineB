package app.sunshine.android.example.com.sunshine2;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.BundleCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.ShareActionProvider;
import android.widget.TextView;

/**
 * Created by jonat on 09/10/2016.
 */

public class DetailActivity extends AppCompatActivity{



    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if(savedInstanceState==null){
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        /*Inflate the menu, this adds items to the action bar if it is present*/
        getMenuInflater().inflate(R.menu.detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        /*Handle action bar item clicks here.  The action bar will
        * automatically handle clicks on the home/up button, so long as
        * you specify a parent activity in the manifest file*/

        int id = item.getItemId();

        if(id==R.id.action_settings){
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    * A placeholder fragment containing a simple view
    */

    public static class DetailFragment extends Fragment{

        private static final String LOG_TAG = DetailFragment.class.getSimpleName();

        private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";
        private String mForecastStr;

        public DetailFragment(){
            setHasOptionsMenu(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState){
            View rootview = inflater.inflate(R.layout.fragment_detail, container, false);

            //The detail activity called by the intent.  Inspect the intent for forecast data.
            Intent intent = getActivity().getIntent();
            if(intent!=null && intent.hasExtra(Intent.EXTRA_TEXT)){
                mForecastStr = intent.getStringExtra(Intent.EXTRA_TEXT);
                ((TextView) rootview.findViewById(R.id.detail_text))
                        .setText(mForecastStr);
            }
            return rootview;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
            //Inflate the menu, this adds items to the action bar if it is present.
            inflater.inflate(R.menu.detailfragment, menu);

            //Retrieve the share menu item.
            MenuItem menuItem = menu.findItem(R.id.action_share);

            //Get the provider and hold onto it to set/change the share intent
            ShareActionProvider mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

            //Attach an intent to this ShareActionProvider.

            if(mShareActionProvider != null){
                mShareActionProvider.setShareIntent(createShareForecastIntent());
            }else {
                Log.d(LOG_TAG, "Share Action provider is null");
            }
        }

        private Intent createShareForecastIntent(){
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, mForecastStr + FORECAST_SHARE_HASHTAG);
            return shareIntent;
        }
    }
}
