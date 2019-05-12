package com.example.goosechase.Entity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.TextView;

import com.example.goosechase.R;
import com.google.gson.JsonElement;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.core.constants.Constants.PRECISION_5;
import static com.mapbox.core.constants.Constants.PRECISION_6;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineDasharray;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineTranslate;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

public class Goose {
    private double destinationLat;
    private double destinationLng;
    private double currentLat;
    private double currentLng;
    private double baseLat;
    private double baseLng;
    private Context context;
    private Activity activity;
    private MapboxMap mapboxMap;
    private FeatureCollection dashedLineDirectionsFeatureCollection;
    private Marker gooseMarker;
    private List<Point> gooseRoute;
    private int route_position_index = 5;
    private final int LINE_COLOR = Color.parseColor("#001360");

    public Goose(double baseLat, double baseLng, MapboxMap mapboxMap, Context context, Activity activity, Style style) {
        this.baseLat = baseLat;
        this.baseLng = baseLng;
        this.mapboxMap = mapboxMap;
        this.context = context;
        this.activity = activity;
        generateDestinationLat();
        generateDestinationLng();
        generateGooseRoute(style);

    }

    //call move whenever user location is updated (from some listener), REMEBER TO UPDATE
    public void move(double userLat, double userLng) {
        //get the distance between the goose and the user.
        double distMetres = distance(currentLat, currentLng, userLat, userLng) / 1000;
        if (distMetres > 50) return; //not close enough to warrant moving the goose.
        if (route_position_index < gooseRoute.size() - 1) { //not yet reached end of the route.
            route_position_index++;
            currentLat = gooseRoute.get(route_position_index).latitude();
            currentLng = gooseRoute.get(route_position_index).longitude();
            //now update the marker
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    gooseMarker.setPosition(new LatLng(currentLat, currentLng));
                    //UPDATE ROUTE
                    drawNavigationPolylineRoute();
                }
            });
        }

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

    private void generateGooseRoute(Style style) {
        System.out.println("Generating the goose route");
        Point destinationPoint = Point.fromLngLat(destinationLng, destinationLat);
        Point originPoint = Point.fromLngLat(baseLng, baseLat);
        initDottedLineSourceAndLayer(style);
        getRoute(destinationPoint, originPoint);
    }

    private void generateMarker() {
        gooseMarker = mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(currentLat, currentLng))
                .title("Goose")
                .icon(getGooseIcon()));
    }

    public Icon getGooseIcon() {
        IconFactory mIconFactory = IconFactory.getInstance(context);
        Bitmap bitmapIcon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.goose);
        Icon icon = mIconFactory.fromBitmap(bitmapIcon);
        return icon;
    }

    //the initial lat of the goose will
    private void generateDestinationLat() {
        destinationLat = ranPosGenerator(baseLat);
    }

    private void generateDestinationLng() {
        destinationLng = ranPosGenerator(baseLng);
    }

    private double ranPosGenerator(double baseVal) {
        return baseVal + (((Math.random() * 2) - 1) / 100);
    }

    private void setInitialStart(DirectionsRoute route) {
        LineString lineString = LineString.fromPolyline(route.geometry(), PRECISION_6);
        List<Point> coordinates = lineString.coordinates();
        gooseRoute = coordinates;

        currentLat = coordinates.get(route_position_index).latitude();
        currentLng = coordinates.get(route_position_index).longitude();
    }

    /**
     * Make a call to the Mapbox Directions API to get the route from the device location to the
     * place picker location
     *
     * @param destination the location chosen by moving the map to the desired destination location
     */
    @SuppressWarnings({"MissingPermission"})
    private void getRoute(Point destination, Point origin) {
        MapboxDirections client = MapboxDirections.builder()
                .origin(origin)
                .destination(destination)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .profile(DirectionsCriteria.PROFILE_WALKING)
                .accessToken(context.getString(R.string.mapbox_key))
                .build();
        client.enqueueCall(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                System.out.println("Got response");
                System.out.println(response.body());
                if (response.body() == null) {
                    return;
                } else if (response.body().routes().size() < 1) {
                    return;
                }

                System.out.println("got the goose route");
                setInitialStart(response.body().routes().get(0));
                generateMarker();
                drawNavigationPolylineRoute();

//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        // This code will run in another thread. Usually as soon as start() gets called!
//                        for (int i = 0; i < gooseRoute.size(); i++) {
//                            try {
//                                Thread.sleep(1000);
//                                move(baseLat, baseLng);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//
//                        }
//                    }
//                }).start();

            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {

                if (!throwable.getMessage().equals("Coordinate is invalid: 0,0")) {

                }
            }
        });

    }

    //TODO move this to some other class. goose shouldnt be responsible for drawing this
    private void drawNavigationPolylineRoute() {
        if (mapboxMap != null) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    List<Feature> directionsRouteFeatureList = new ArrayList<>();
                    List<Point> relativeRoute = new ArrayList<>();
                    for (int i = 0; i < route_position_index+1; i++) {
                        relativeRoute.add(gooseRoute.get(i));
                    }
                    for (int i = 0; i < relativeRoute.size(); i++) {
                        directionsRouteFeatureList.add(Feature.fromGeometry(LineString.fromLngLats(relativeRoute)));
                    }
                    dashedLineDirectionsFeatureCollection = FeatureCollection.fromFeatures(directionsRouteFeatureList);
                    GeoJsonSource source = style.getSourceAs("SOURCE_ID");
                    if (source != null) {
                        source.setGeoJson(dashedLineDirectionsFeatureCollection);
                    }
                }
            });
        }
    }

    //gives distance in KM
    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;

        dist = dist * 1.609344;

        return (dist);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::  This function converts decimal degrees to radians             :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    /*::  This function converts radians to decimal degrees             :*/
    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    public double getCurrentLat() {
        return currentLat;
    }

    public double getCurrentLng() {
        return currentLng;
    }

    public double getBaseLat() {
        return baseLat;
    }

    public double getBaseLng() {
        return baseLng;
    }


}
