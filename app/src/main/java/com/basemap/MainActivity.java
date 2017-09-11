package com.basemap;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.action.IdentifyResultSpinner;
import com.esri.android.action.IdentifyResultSpinnerAdapter;
import com.esri.android.map.Callout;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapOptions;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.tasks.geocode.Locator;
import com.esri.core.tasks.geocode.LocatorFindParameters;
import com.esri.core.tasks.geocode.LocatorGeocodeResult;
import com.esri.core.tasks.identify.IdentifyParameters;
import com.esri.core.tasks.identify.IdentifyResult;
import com.esri.core.tasks.identify.IdentifyTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private String mMapViewState;
    private static ProgressDialog mProgressDialog;
    MapView mMapView = null;
    MenuItem mSearchItem = null;
    MenuItem mStreetsMenuItem = null;
    MenuItem mTopoMenuItem = null;
    MenuItem mGrayMenuItem = null;
    MenuItem mOceansMenuItem = null;

    GraphicsLayer mLocationLayer;
    Point mLocationLayerPoint;
    String mLocationLayerPointString;

    IdentifyParameters params = null;

    EditText mSearchEditText;

    Boolean mIsMapLoaded = null;

    final MapOptions mTopoBasemap = new MapOptions(MapOptions.MapType.TOPO);
    final MapOptions mStreetsBasemap = new MapOptions(MapOptions.MapType.STREETS);
    final MapOptions mGrayBasemap = new MapOptions(MapOptions.MapType.GRAY);
    final MapOptions mOceansBasemap = new MapOptions(MapOptions.MapType.OCEANS);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMapView = (MapView) findViewById(R.id.map);
        mMapView.addLayer(new ArcGISTiledMapServiceLayer(this.getResources()
                .getString(R.string.identify_task_url_for_avghouseholdsize)));
        params = new IdentifyParameters();
        params.setTolerance(20);
        params.setDPI(98);
        params.setLayers(new int[] { 4 });
        params.setLayerMode(IdentifyParameters.ALL_LAYERS);
        mMapView.setOnSingleTapListener(new OnSingleTapListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onSingleTap(final float x, final float y) {

                if (!mMapView.isLoaded()) {
                    return;
                }

                // Add to Identify Parameters based on tapped location
                Point identifyPoint = mMapView.toMapPoint(x, y);

                params.setGeometry(identifyPoint);
                params.setSpatialReference(mMapView.getSpatialReference());
                params.setMapHeight(mMapView.getHeight());
                params.setMapWidth(mMapView.getWidth());
                params.setReturnGeometry(false);

                // add the area of extent to identify parameters
                Envelope env = new Envelope();
                mMapView.getExtent().queryEnvelope(env);
                params.setMapExtent(env);

                // execute the identify task off UI thread
                MyIdentifyTask mTask = new MyIdentifyTask(identifyPoint);
                mTask.execute(params);
            }

        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mProgressDialog = new ProgressDialog(this) {
            @Override
            public void onBackPressed() {
                // Back key pressed - just dismiss the dialog
                mProgressDialog.dismiss();
            }
        };
        mMapView = (MapView) findViewById(R.id.map);
        mLocationLayer = new GraphicsLayer();
        mMapView.addLayer(mLocationLayer);
        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onStatusChanged(Object source, STATUS status) {
                if (source == mMapView && status == STATUS.INITIALIZED) {

                    if (mMapViewState == null) {
                        Log.i(TAG,
                                "MapView.setOnStatusChangedListener() status="
                                        + status.toString());
                    } else {
                        mMapView.restoreState(mMapViewState);
                    }

                }
            }
        });
        mMapView.enableWrapAround(true);



        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    private ViewGroup createIdentifyContent(final List<IdentifyResult> results) {

        // create a new LinearLayout in application context
        LinearLayout layout = new LinearLayout(this);

        // view height and widthwrap content
        layout.setLayoutParams(new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT,
                Toolbar.LayoutParams.WRAP_CONTENT));

        // default orientation
        layout.setOrientation(LinearLayout.HORIZONTAL);

        // Spinner to hold the results of an identify operation
        IdentifyResultSpinner spinner = new IdentifyResultSpinner(this, results);

        // make view clickable
        spinner.setClickable(false);
        spinner.canScrollHorizontally(BIND_ADJUST_WITH_ACTIVITY);

        // MyIdentifyAdapter creates a bridge between spinner and it's data
        MyIdentifyAdapter adapter = new MyIdentifyAdapter(this, results);
        spinner.setAdapter(adapter);
        spinner.setLayoutParams(new Toolbar.LayoutParams(Toolbar.LayoutParams.MATCH_PARENT,
                Toolbar.LayoutParams.WRAP_CONTENT));
        layout.addView(spinner);

        return layout;
    }

    public class MyIdentifyAdapter extends IdentifyResultSpinnerAdapter {
        String m_show = null;
        List<IdentifyResult> resultList;
        int currentDataViewed = -1;
        Context m_context;

        public MyIdentifyAdapter(Context context, List<IdentifyResult> results) {
            super(context, results);
            this.resultList = results;
            this.m_context = context;
        }

        // Get a TextView that displays identify results in the callout.
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String LSP = System.getProperty("line.separator");
            StringBuilder outputVal = new StringBuilder();

            // Resource Object to access the Resource fields
            Resources res = getResources();

            // Get Name attribute from identify results
            IdentifyResult curResult = this.resultList.get(position);

            if (curResult.getAttributes().containsKey(
                    res.getString(R.string.NAME))) {
                outputVal.append("Place: "
                        + curResult.getAttributes()
                        .get(res.getString(R.string.NAME)).toString());
                outputVal.append(LSP);
            }

            if (curResult.getAttributes().containsKey(
                    res.getString(R.string.ID))) {
                outputVal.append("State ID: "
                        + curResult.getAttributes()
                        .get(res.getString(R.string.ID)).toString());
                outputVal.append(LSP);
            }

            if (curResult.getAttributes().containsKey(
                    res.getString(R.string.ST_ABBREV))) {
                outputVal.append("Abbreviation: "
                        + curResult.getAttributes()
                        .get(res.getString(R.string.ST_ABBREV))
                        .toString());
                outputVal.append(LSP);
            }

            if (curResult.getAttributes().containsKey(
                    res.getString(R.string.TOTPOP_CY))) {
                outputVal.append("Population: "
                        + curResult.getAttributes()
                        .get(res.getString(R.string.TOTPOP_CY))
                        .toString());
                outputVal.append(LSP);

            }

            if (curResult.getAttributes().containsKey(
                    res.getString(R.string.LANDAREA))) {
                outputVal.append("Area: "
                        + curResult.getAttributes()
                        .get(res.getString(R.string.LANDAREA))
                        .toString());
                outputVal.append(LSP);

            }

            // Create a TextView to write identify results
            TextView txtView;
            txtView = new TextView(this.m_context);
            txtView.setText(outputVal);
            txtView.setTextColor(Color.BLACK);
            txtView.setLayoutParams(new ListView.LayoutParams(
                    Toolbar.LayoutParams.MATCH_PARENT, Toolbar.LayoutParams.WRAP_CONTENT));
            txtView.setGravity(Gravity.CENTER_VERTICAL);

            return txtView;
        }
    }

    private class MyIdentifyTask extends
            AsyncTask<IdentifyParameters, Void, IdentifyResult[]> {

        IdentifyTask task = new IdentifyTask(MainActivity.this.getResources()
                .getString(R.string.identify_task_url_for_avghouseholdsize));

        IdentifyResult[] M_Result;

        Point mAnchor;

        MyIdentifyTask(Point anchorPoint) {
            mAnchor = anchorPoint;
        }

        @Override
        protected void onPreExecute() {
            // create dialog while working off UI thread
            mProgressDialog = ProgressDialog.show(MainActivity.this, "Identify Task",
                    "Identify query ...");

        }

        protected IdentifyResult[] doInBackground(IdentifyParameters... params) {

            // check that you have the identify parameters
            if (params != null && params.length > 0) {
                IdentifyParameters mParams = params[0];

                try {
                    // Run IdentifyTask with Identify Parameters

                    M_Result = task.execute(mParams);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return M_Result;
        }

        @Override
        protected void onPostExecute(IdentifyResult[] results) {

            // dismiss dialog
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }

            ArrayList<IdentifyResult> resultList = new ArrayList<IdentifyResult>();

            IdentifyResult result_1;

            for (int index = 0; index < results.length; index++) {

                result_1 = results[index];
                String displayFieldName = result_1.getDisplayFieldName();
                Map<String, Object> attr = result_1.getAttributes();
                for (String key : attr.keySet()) {
                    if (key.equalsIgnoreCase(displayFieldName)) {
                        resultList.add(result_1);
                    }
                }
            }

            Callout callout = mMapView.getCallout();
            callout.setContent(createIdentifyContent(resultList));
            callout.show(mAnchor);


        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mSearchItem = menu.getItem(0);
        mStreetsMenuItem = menu.getItem(1);
        mTopoMenuItem = menu.getItem(2);
        mGrayMenuItem = menu.getItem(3);
        mOceansMenuItem = menu.getItem(4);
        mTopoMenuItem.setChecked(true);
        View searchRef = menu.findItem(R.id.action_search).getActionView();
        mSearchEditText = (EditText) searchRef.findViewById(R.id.searchText);

        mSearchEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    onSearchButtonClicked(mSearchEditText);
                    return true;
                }

                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.World_Street_Map:
                mMapView.setMapOptions(mStreetsBasemap);
                mStreetsMenuItem.setChecked(true);
                return true;
            case R.id.World_Topo:
                mMapView.setMapOptions(mTopoBasemap);
                mTopoMenuItem.setChecked(true);
                return true;
            case R.id.Gray:
                mMapView.setMapOptions(mGrayBasemap);
                mGrayMenuItem.setChecked(true);
                return true;
            case R.id.Ocean_Basemap:
                mMapView.setMapOptions(mOceansBasemap);
                mOceansMenuItem.setChecked(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public void onSearchButtonClicked(View view) {
        // Hide virtual keyboard
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(view.getWindowToken(), 0);

        // obtain address and execute locator task
        String address = mSearchEditText.getText().toString();
        executeLocatorTask(address);

    }

    private void executeLocatorTask(String address) {
        // Create Locator parameters from single line address string
        LocatorFindParameters findParams = new LocatorFindParameters(address);

        // Use the center of the current map extent as the find location point
        findParams.setLocation(mMapView.getCenter(),
                mMapView.getSpatialReference());

        // Calculate distance for find operation
        Envelope mapExtent = new Envelope();
        mMapView.getExtent().queryEnvelope(mapExtent);
        // assume map is in meters, other units wont work, double current
        // envelope
        double distance = (mapExtent != null && mapExtent.getWidth() > 0) ? mapExtent
                .getWidth() * 2 : 10000;
        findParams.setDistance(distance);
        findParams.setMaxLocations(2);

        // Set address spatial reference to match map
        findParams.setOutSR(mMapView.getSpatialReference());

        // Execute async task to find the address
        new LocatorAsyncTask().execute(findParams);
    }

    private class LocatorAsyncTask extends
            AsyncTask<LocatorFindParameters, Void, List<LocatorGeocodeResult>> {
        private Exception mException;

        public LocatorAsyncTask() {
        }

        @Override
        protected void onPreExecute() {
            // Display progress dialog on UI thread
            mProgressDialog.setMessage(getString(R.string.address_search));
            mProgressDialog.show();
        }

        @Override
        protected List<LocatorGeocodeResult> doInBackground(
                LocatorFindParameters... params) {
            // Perform routing request on background thread
            mException = null;
            List<LocatorGeocodeResult> results = null;

            // Create locator using default online geocoding service and tell it
            // to find the given address
            Locator locator = Locator.createOnlineLocator();
            try {
                results = locator.find(params[0]);
            } catch (Exception e) {
                mException = e;
            }
            return results;
        }

        @Override
        protected void onPostExecute(List<LocatorGeocodeResult> result) {
            // Display results on UI thread
            mProgressDialog.dismiss();
            if (mException != null) {
                Log.w(TAG, "LocatorSyncTask failed with:");
                mException.printStackTrace();
                Toast.makeText(MainActivity.this,
                        getString(R.string.addressSearchFailed),
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (result.size() == 0) {
                Toast.makeText(MainActivity.this,
                        getString(R.string.noResultsFound), Toast.LENGTH_LONG)
                        .show();
            } else {
                // Use first result in the list
                LocatorGeocodeResult geocodeResult = result.get(0);

                // get return geometry from geocode result
                Point resultPoint = geocodeResult.getLocation();
                // create marker symbol to represent location
                SimpleMarkerSymbol resultSymbol = new SimpleMarkerSymbol(
                        Color.RED, 16, SimpleMarkerSymbol.STYLE.CROSS);
                // create graphic object for resulting location
                Graphic resultLocGraphic = new Graphic(resultPoint,
                        resultSymbol);
                // add graphic to location layer
                mLocationLayer.addGraphic(resultLocGraphic);

                // create text symbol for return address
                String address = geocodeResult.getAddress();
                TextSymbol resultAddress = new TextSymbol(20, address,
                        Color.BLACK);
                // create offset for text
                resultAddress.setOffsetX(-4 * address.length());
                resultAddress.setOffsetY(10);
                // create a graphic object for address text
                Graphic resultText = new Graphic(resultPoint, resultAddress);
                // add address text graphic to location graphics layer
                mLocationLayer.addGraphic(resultText);

                // Zoom map to geocode result location
                mMapView.zoomToResolution(geocodeResult.getLocation(), 2);
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mMapViewState = mMapView.retainState();
        mMapView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start the MapView running again
        if (mMapView != null) {
            mMapView.unpause();
            if (mMapViewState != null) {
                mMapView.restoreState(mMapViewState);
            }
        }
    }
}
