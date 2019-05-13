package com.example.goosechase;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.goosechase.Entity.Goose;
import com.example.goosechase.Helpers.MyLocation;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineDasharray;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineTranslate;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

import java.util.ArrayList;
import java.util.List;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineDasharray;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineTranslate;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

public class MainActivity extends AppCompatActivity {
    private MapView mapView;
    private MapboxMap mapboxMap;
    private Marker myMarker;
    private Style mapboxStyle;
    private double myLat;
    private double myLng;
    final Handler handler = new Handler();
    private final int LINE_COLOR = Color.parseColor("#001360");
    private Marker gooseMarker;
    private MyLocation myLocation;
    private boolean shouldGetLocation = true;
    private Goose generatedGoose;
    private FeatureCollection dashedLineDirectionsFeatureCollection;

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        shouldGetLocation = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        shouldGetLocation = true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_key));

        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        myLocation = MyLocation.getInstance();
        myLocation.setActivity(this);
        myLocation.getLocationPermission(permission -> myLocation.getDeviceLocation((lat, lng) -> {
            myLat = lat;
            myLng = lng;
            instantiateMap();
            getLocation();
        }));
    }

    public void generateGooseButton(View view) {
        if (generatedGoose != null) {
            generatedGoose.destroy();
        }
        generatedGoose = new Goose(myLat, myLng, mapboxMap, mapView, getApplicationContext(), MainActivity.this, mapboxStyle);
        generatedGoose.setDashedLineDirectionsFeatureCollection(dashedLineDirectionsFeatureCollection);
        generatedGoose.start();


    }

    private void getLocation() {
        if (shouldGetLocation == false) {
            runDelay();
            return;
        }
        myLocation.getDeviceLocation((lat, lng) -> {
            System.out.println("GOT YOUR LAT LONG " + lat + ":" + lng);
            runDelay();
            myLat = lat;
            myLng = lng;

            if (myMarker != null) {
                myMarker.setPosition(new LatLng(lat, lng));
                if (generatedGoose != null) {
                    generatedGoose.move(lat, lng);
                }
            }
        });
    }

    private void runDelay() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                getLocation();
            }
        }, 1000);
    }

    private void instantiateMap() {

        mapView.getMapAsync(new OnMapReadyCallback() {
            @SuppressLint("WrongConstant")
            @Override
            public void onMapReady(@NonNull MapboxMap mbm) {
                mapboxMap = mbm;
                mapboxMap.getUiSettings().setAttributionEnabled(false);
                mapboxMap.getUiSettings().setLogoEnabled(false);

                mapboxMap.setCameraPosition(new CameraPosition.Builder().target(new LatLng(myLat, myLng))
                        .zoom(16)
                        .build());
                mapboxMap.setStyle("mapbox://styles/delisioinc/cjqab7qvj9ifd2sp6ns8qg27w", new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {

                        mapboxStyle = style;
                        // Map is set up and the style has loaded. Now you can add data or make other map adjustments

                        myMarker = mapboxMap.addMarker(new MarkerOptions()
                                .position(new LatLng(myLat, myLng))
                                .title("You")
                                .icon(getUserIcon()));
                        initDottedLineSourceAndLayer(style);
                    }
                });
            }
        });
    }

    private void initDottedLineSourceAndLayer(@NonNull Style loadedMapStyle) {
        dashedLineDirectionsFeatureCollection = FeatureCollection.fromFeatures(new Feature[]{});
        loadedMapStyle.addSource(new GeoJsonSource("SOURCE_ID", dashedLineDirectionsFeatureCollection));
        loadedMapStyle.addLayerBelow(
                new LineLayer(
                        "DIRECTIONS_LAYER_ID", "SOURCE_ID").withProperties(
                        lineWidth(4.5f),
                        lineColor(LINE_COLOR),
                        lineTranslate(new Float[]{0f, 4f}),
                        lineDasharray(new Float[]{1.2f, 1.2f})
                ), "road-label-small");
    }

    public Icon getUserIcon() {
        int height = 75;
        int width = 75;
        BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.mipmap.user_icon_goose_app);
        Bitmap b = bitmapdraw.getBitmap();
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
        IconFactory mIconFactory = IconFactory.getInstance(this);
        Icon icon = mIconFactory.fromBitmap(smallMarker);
        return icon;
    }
}
