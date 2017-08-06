package dongwei.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.data.Attachment;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureEditResult;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.ArcGISMapImageLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.gimbal.GimbalState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.model.LocationCoordinate2D;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.Camera;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.DownloadListener;
import dji.sdk.camera.MediaFile;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;
import dji.thirdparty.okhttp3.internal.framed.FrameReader;
import dji.thirdparty.okhttp3.internal.http.StreamAllocation;
import dji.ui.widget.FPVOverlayWidget;
import dji.ui.widget.FPVWidget;
import me.iwf.photopicker.PhotoPicker;
import me.iwf.photopicker.PhotoPreview;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;
import org.apache.commons.io.FileUtils;
import android.os.Handler;
import android.os.Looper;
import static dji.common.mission.waypoint.WaypointActionType.START_TAKE_PHOTO;

import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

public class MainActivity extends AppCompatActivity  {
    List<LayerAttribute> layerAttributes=new ArrayList<LayerAttribute>();
    private MapView mMapView;
    private LocationDisplay mLocationDisplay;
    FeatureLayer featureLayer;
    ArcGISMap map;
    private int requestCode = 2;
    String[] reqPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
            .ACCESS_COARSE_LOCATION};
    ServiceFeatureTable serviceFeatureTable;
    SensorManager sensorManager;
    SensorEventListener listener;
    LinearLayout barlayout;
    LinearLayout craftguidelayout;
    DrawTool dt;
    private ProgressDialog progressDialog;
    CraftTool ct;
    Drawable drawable;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        //////////////////////////////////////////sdk分割线///////////////////////
        drawable=getDrawable(R.drawable.light_blue_oval);
        progressDialog=new ProgressDialog(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        barlayout=(LinearLayout)findViewById(R.id.editbar);
        barlayout.setVisibility(View.INVISIBLE);
        craftguidelayout=(LinearLayout)findViewById(R.id.craftguide);
        craftguidelayout.setVisibility(View.INVISIBLE);
        // set an on touch listener to listen for click events
        initialMap();
        initialLayerManage();
        initialsensors();
        initialButtons();

    }


    void initialLayerManage()
    {
        // create feature layer with its service feature table
        // create the service feature table
        serviceFeatureTable= new ServiceFeatureTable(getResources().getString(R.string.sample_service_url));
        final ServiceFeatureTable serviceFeatureTable1= new ServiceFeatureTable("http://www.giswe.com:6080/arcgis/rest/services/craft_point/FeatureServer/0");
        // create the feature layer using the service feature table
        featureLayer = new FeatureLayer(serviceFeatureTable);
        featureLayer.setSelectionColor(Color.YELLOW);
        featureLayer.setSelectionWidth(10);
        featureLayer.setName("解译标识");
        final FeatureLayer featureLayer1 = new FeatureLayer(serviceFeatureTable1);
        featureLayer1.setSelectionColor(Color.YELLOW);
        featureLayer1.setSelectionWidth(10);

        featureLayer1.setName("备注点");
        RecyclerView rcv=(RecyclerView)findViewById(R.id.layerRcycleView);


        LayerAdapter adapter=new LayerAdapter(layerAttributes);
        LinearLayoutManager manager=new LinearLayoutManager(MainActivity.this);
        rcv.setLayoutManager(manager);


        //先实例化Callback
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter);
        //用Callback构造ItemtouchHelper
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        //调用ItemTouchHelper的attachToRecyclerView方法建立联系
        touchHelper.attachToRecyclerView( rcv);
        rcv.setAdapter(adapter);
        map.getOperationalLayers().add(featureLayer);
        map.getOperationalLayers().add(featureLayer1);
        map.loadAsync();


        layerAttributes.add(new LayerAttribute(serviceFeatureTable,featureLayer,true,100));
        layerAttributes.add(new LayerAttribute(serviceFeatureTable1,featureLayer1,true,100));



    }
    //初始化地图相关数据
    void initialMap()
    {
        map = new ArcGISMap(Basemap.Type.IMAGERY_WITH_LABELS, 30,114, 5);
        // inflate MapView from layout
        mMapView = (MapView) findViewById(R.id.mapViewLayout);
        mMapView.setMap(map);
    }

    //初始化手机感应器
    void initialsensors(){
        // get the MapView's LocationDisplay
        mLocationDisplay = mMapView.getLocationDisplay();
        // Listen to changes in the status of the location data source.
        mLocationDisplay.addDataSourceStatusChangedListener(new LocationDisplay.DataSourceStatusChangedListener() {
            @Override
            public void onStatusChanged(LocationDisplay.DataSourceStatusChangedEvent dataSourceStatusChangedEvent) {

                // If LocationDisplay started OK, then continue.
                if (dataSourceStatusChangedEvent.isStarted())
                    return;

                // No error is reported, then continue.
                if (dataSourceStatusChangedEvent.getError() == null)
                    return;

                // If an error is found, handle the failure to start.
                // Check permissions to see if failure may be due to lack of permissions.
                boolean permissionCheck1 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[0]) ==
                        PackageManager.PERMISSION_GRANTED;
                boolean permissionCheck2 = ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[1]) ==
                        PackageManager.PERMISSION_GRANTED;

                if (!(permissionCheck1 && permissionCheck2)) {
                    // If permissions are not already granted, request permission from the user.
                    ActivityCompat.requestPermissions(MainActivity.this, reqPermissions, requestCode);
                } else {
                    // Report other unknown failure types to the user - for example, location services may not
                    // be enabled on the device.
                    String message = String.format("Error in DataSourceStatusChangedListener: %s", dataSourceStatusChangedEvent
                            .getSource().getLocationDataSource().getError().getMessage());
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();


                }
            }
        });

        //获取到一个传感器管理器
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        SensorEventListener listener = new SensorEventListener() {
            //当手机的加速度发生变化时调用
            @Override
            public void onSensorChanged(SensorEvent event) {
                //获取手机在不同方向上加速度的变化
                float valuesX = Math.abs(event.values[0]);
                float valuesY = Math.abs(event.values[1]);
                float valuesZ = Math.abs(event.values[2]);

                if (valuesX > 13 || valuesY > 13 ) {
                    mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
                    if (!mLocationDisplay.isStarted())
                        mLocationDisplay.startAsync();
                }
                else if(valuesZ>20){
                    // This mode is better suited for waypoint navigation when the user is walking.
                    mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);
                    if (!mLocationDisplay.isStarted())
                        mLocationDisplay.startAsync();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        //获得一个加速度传感器
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME);
        mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
        mLocationDisplay.startAsync();

        Compass mCompass = new Compass(this, null, mMapView);
        mMapView.addView(mCompass);
    }

    //初始化对应的按钮事件
    void initialButtons(){
        final FloatingActionMenu  fmenu=(FloatingActionMenu)findViewById(R.id.menu);
        fmenu.setMenuButtonColorNormalResId(R.color.light_blue_500);
        fmenu.setMenuButtonColorPressedResId(R.color.path_blue);

        FloatingActionButton fab1=( FloatingActionButton) findViewById(R.id.edit);
        fab1.setColorNormalResId(R.color.light_blue_500);
        fab1.setImageResource(R.drawable.editor);
        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //barlayout.setVisibility(View.VISIBLE);
                serviceFeatureTable=layerAttributes.get(0).serviceFeatureTable;
                featureLayer=(FeatureLayer)layerAttributes.get(0).layer;
                dt=new DrawTool(MainActivity.this,serviceFeatureTable,mMapView,featureLayer.getFeatureTable(),featureLayer, featureLayer.getSpatialReference(),progressDialog);
                fmenu.close(true);


            }
        });

        FloatingActionButton fab2=( FloatingActionButton) findViewById(R.id.craft);
        fab2.setColorNormalResId(R.color.light_blue_500);
        fab2.setImageResource(R.drawable.craft);
        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //barlayout.setVisibility(View.VISIBLE);

                craftguidelayout.setVisibility(View.VISIBLE);
                //LinearLayout craftlay,Drawable background,MapView mapview
                ct=new CraftTool(craftguidelayout,drawable,mMapView,MainActivity.this,serviceFeatureTable);
                fmenu.close(true);
            }
        });

        FloatingActionButton fab3=( FloatingActionButton) findViewById(R.id.webpost);
        fab3.setColorNormalResId(R.color.light_blue_500);
        fab3.setImageResource(R.drawable.webpost);
        fab3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //barlayout.setVisibility(View.VISIBLE);
                RelativeLayout rl=(RelativeLayout)findViewById(R.id.layermanagerlay);
                rl.setVisibility(View.VISIBLE);
                fmenu.close(true);
            }
        });



        Button btnlayermanagerok=(Button)findViewById(R.id.btnlayermanagerok);
        btnlayermanagerok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               map.getOperationalLayers().clear();
                for (int i=0;i<layerAttributes.size();i++)
                {
                    layerAttributes.get(i).layer.setVisible(layerAttributes.get(i).visible);
                    layerAttributes.get(i).layer.setOpacity((float)layerAttributes.get(i).transparency/100);

                    map.getOperationalLayers().add(layerAttributes.get(i).layer);
                }
                RelativeLayout rl=(RelativeLayout)findViewById(R.id.layermanagerlay);
                rl.setVisibility(View.INVISIBLE);

            }
        });

        Button btnlayermanagercancel=(Button)findViewById(R.id.btnlayermanagercancel);
        btnlayermanagercancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RelativeLayout rl=(RelativeLayout)findViewById(R.id.layermanagerlay);
                rl.setVisibility(View.INVISIBLE);
            }
        });

    }


    //活动结束之后
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

       dt.applyToactivityResult(requestCode,resultCode,data);
    }

    @Override
    protected void onDestroy()
    {
        unregisterReceiver(ct.mReceiver);
        ct.removeListener();
        super.onDestroy();
        //解除对加速度传感器的监听
        sensorManager.unregisterListener(listener);

    }

}
