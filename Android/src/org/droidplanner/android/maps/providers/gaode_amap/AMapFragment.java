package org.droidplanner.android.maps.providers.gaode_amap;

import android.location.LocationListener;

import com.google.android.gms.common.ConnectionResult;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.property.FootPrint;
import com.o3dr.services.android.lib.util.googleApi.GoogleApiClientManager;

import org.droidplanner.android.maps.DPMap;
import org.droidplanner.android.maps.MarkerInfo;
import org.droidplanner.android.maps.providers.DPMapProvider;
import org.droidplanner.android.utils.prefs.AutoPanMode;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by joe on 2015/9/25.
 */
public class AMapFragment implements DPMap, GoogleApiClientManager.ManagerListener {
    @Override
    public void addFlightPathPoint(LatLong coord) {

    }

    @Override
    public void addCameraFootprint(FootPrint footprintToBeDraw) {

    }

    @Override
    public void clearMarkers() {

    }

    @Override
    public void clearFlightPath() {

    }

    @Override
    public LatLong getMapCenter() {
        return null;
    }

    @Override
    public float getMapZoomLevel() {
        return 0;
    }

    @Override
    public Set<MarkerInfo> getMarkerInfoList() {
        return null;
    }

    @Override
    public float getMaxZoomLevel() {
        return 0;
    }

    @Override
    public float getMinZoomLevel() {
        return 0;
    }

    @Override
    public DPMapProvider getProvider() {
        return null;
    }

    @Override
    public void goToDroneLocation() {

    }

    @Override
    public void goToMyLocation() {

    }

    @Override
    public void loadCameraPosition() {

    }

    @Override
    public List<LatLong> projectPathIntoMap(List<LatLong> pathPoints) {
        return null;
    }

    @Override
    public void removeMarkers(Collection<MarkerInfo> markerInfoList) {

    }

    @Override
    public void saveCameraPosition() {

    }

    @Override
    public void selectAutoPanMode(AutoPanMode mode) {

    }

    @Override
    public void setMapPadding(int left, int top, int right, int bottom) {

    }

    @Override
    public void setOnMapClickListener(OnMapClickListener listener) {

    }

    @Override
    public void setOnMapLongClickListener(OnMapLongClickListener listener) {

    }

    @Override
    public void setOnMarkerClickListener(OnMarkerClickListener listener) {

    }

    @Override
    public void setOnMarkerDragListener(OnMarkerDragListener listener) {

    }

    @Override
    public void setLocationListener(LocationListener listener) {

    }

    @Override
    public void updateCamera(LatLong coord, float zoomLevel) {

    }

    @Override
    public void updateCameraBearing(float bearing) {

    }

    @Override
    public void updateDroneLeashPath(PathSource pathSource) {

    }

    @Override
    public void updateMarker(MarkerInfo markerInfo) {

    }

    @Override
    public void updateMarker(MarkerInfo markerInfo, boolean isDraggable) {

    }

    @Override
    public void updateMarkers(List<MarkerInfo> markersInfos) {

    }

    @Override
    public void updateMarkers(List<MarkerInfo> markersInfos, boolean isDraggable) {

    }

    @Override
    public void updateMissionPath(PathSource pathSource) {

    }

    @Override
    public void updatePolygonsPaths(List<List<LatLong>> paths) {

    }

    @Override
    public void zoomToFit(List<LatLong> coords) {

    }

    @Override
    public void zoomToFitMyLocation(List<LatLong> coords) {

    }

    @Override
    public void skipMarkerClickEvents(boolean skip) {

    }

    @Override
    public void updateRealTimeFootprint(FootPrint footprint) {

    }

    @Override
    public void onGoogleApiConnectionError(ConnectionResult connectionResult) {

    }

    @Override
    public void onUnavailableGooglePlayServices(int i) {

    }

    @Override
    public void onManagerStarted() {

    }

    @Override
    public void onManagerStopped() {

    }
}
