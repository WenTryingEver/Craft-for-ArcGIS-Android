package dongwei.myapplication;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.TextSymbol;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
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
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.DownloadListener;
import dji.sdk.camera.MediaFile;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;
import dji.ui.widget.FPVOverlayWidget;
import dji.ui.widget.FPVWidget;

import static dji.common.mission.waypoint.WaypointActionType.START_TAKE_PHOTO;


/**
 * Created by Administrator on 2017/7/25 0025.
 */

public class CraftTool {

    private  boolean clickListener;
    MainActivity activity;
    int currentstep;
    LinearLayout craftlay;
    private GraphicsOverlay tempgo;
    MapView mapview;
    Drawable color;
    public List<CraftTask> works=new ArrayList<CraftTask>();
    CraftTask work;

    public List<Point>flypoints=new ArrayList<Point>();
    public List<Double>altitudes=new ArrayList<Double>();
    int counter;
    int originalsize;

    //激活sdk
    private static final String TAG = MainActivity.class.getName();
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    private static BaseProduct mProduct;
    private Handler mHandler;

    private DJISDKManager.SDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {
        @Override
        public void onRegister(DJIError error) {
            Log.d(TAG, error == null ? "success" : error.getDescription());
            if(error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity.getApplicationContext(), "Register Success", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity.getApplicationContext(), "register sdk failed, check if network is available", Toast.LENGTH_LONG).show();
                    }
                });
            }
            Log.e("TAG", error.toString());
        }
        @Override
        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
            mProduct = newProduct;
            if(mProduct != null) {
                mProduct.setBaseProductListener(mDJIBaseProductListener);
            }
            notifyStatusChange();
        }
    };
    private BaseProduct.BaseProductListener mDJIBaseProductListener = new BaseProduct.BaseProductListener() {
        @Override
        public void onComponentChange(BaseProduct.ComponentKey key, BaseComponent oldComponent, BaseComponent newComponent) {
            if(newComponent != null) {
                newComponent.setComponentListener(mDJIComponentListener);
            }
            notifyStatusChange();
        }
        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }
    };
    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {
        @Override
        public void onConnectivityChange(boolean isConnected) {
            notifyStatusChange();
        }
    };
    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }
    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            activity.sendBroadcast(intent);
        }
    };

    ServiceFeatureTable st;

     class clickedexcute implements AfterClicked
    {
        public void excuteWhenClicked(Feature feature,boolean bool)
        {
            sendTask(feature,bool);
        }
    }
    public CraftTool(LinearLayout craftlay,Drawable background,MapView mapview,MainActivity activity,ServiceFeatureTable st)
    {
        this.color=background;
        this.craftlay=craftlay;
        this.mapview=mapview;
        tempgo=addGraphicsOverlay();
        currentstep=-1;
        //ChangeButton();
        counter=0;
        this.activity=activity;
        setOnClickListener();
        //Initialize DJI SDK Manager
        mHandler = new Handler(Looper.getMainLooper());
        DJISDKManager.getInstance().registerApp(activity, mDJISDKManagerCallback);


        ///////////////////
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }
        this.st=st;
        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIDemoApplication.FLAG_CONNECTION_CHANGE);
        activity.registerReceiver(mReceiver, filter);
        activity.registerReceiver(mReceiver, filter);

        initUI();
        loginAccount();

      ;




    }



    public void sendTask(Feature feature,boolean bool)
    {
        // firstly draw
        work=new CraftTask(feature,bool);
        Graphic gra;
        if(bool==true)
        {
            gra=new Graphic(feature.getGeometry(),DrawSymbol.mFillSymbol1);
        }
        else {
            gra=new Graphic(feature.getGeometry(),DrawSymbol.mFillSymbol);
        }


        tempgo.getGraphics().add(gra);
        works.add(work);
        generatePoints();
        drawOnMap();


    }
    private GraphicsOverlay addGraphicsOverlay() {
        //create the graphics overlay
        GraphicsOverlay graphicsOverlay = new GraphicsOverlay();
        //add the overlay to the map view
        mapview.getGraphicsOverlays().add(graphicsOverlay);
        return graphicsOverlay;
    }

    int getTheCapturePoints(Geometry geometry,boolean bool)
    {
        double sumx=0,sumy=0;
        double minx,miny,maxx,maxy;
        Polygon polygon = (Polygon) geometry;
        Iterable<Point> points=polygon.getParts().getPartsAsPoints();
        Iterator iter = points.iterator();
        int counter=0;

        //初始坐标
        Point pt =(Point)iter.next();
        sumx=sumx+pt.getX();
        sumy=sumy+pt.getY();
        maxx=minx=pt.getX();
        maxy=miny=pt.getY();
        counter=counter+1;
        while(iter.hasNext()) {
            pt =(Point)iter.next();
            sumx=sumx+pt.getX();
            sumy=sumy+pt.getY();
            counter=counter+1;

            if(maxx<pt.getX())
            {
                maxx=pt.getX();
            }
            if(maxy<pt.getY())
            {
                maxy=pt.getY();
            }
            if(minx>pt.getX())
            {
                minx=pt.getX();
            }
            if(miny>pt.getY())
            {
                miny=pt.getY();
            }
        }
        double longer=maxy-miny;
        if (longer<maxx-minx)
        {
            longer=maxx-minx;
        }

        double h=longer/(2*Math.tan(20));
        if(h<10)
        {
            h=10;
        }
        if(h>50)
        {
            h=50;
        }
        flypoints.add(new Point(sumx/counter,sumy/counter));
        altitudes.add(h);
        counter++;
        if(bool==false)
        {
            return 1;
        }

        flypoints.add(new Point(minx,maxy));
        altitudes.add(h);
        counter++;
        flypoints.add(new Point(minx,miny));
        altitudes.add(h);
        counter++;
        flypoints.add(new Point(maxx,maxy));
        altitudes.add(h);
        counter++;
        flypoints.add(new Point(maxx,miny));
        altitudes.add(h);
        counter++;
        return 1;
    }

    public void generatePoints()
    {
        flypoints.clear();

        for(int i=0;i<works.size();i++)
        {

            works.get(i).indexst=counter;
            getTheCapturePoints(works.get(i).feature.getGeometry(),works.get(i).advance);
            works.get(i).indexed=counter;
        }
        originalsize=tempgo.getGraphics().size();
    }
    public void drawOnMap()
    {
        for(int i=0;i<flypoints.size();i++)
        {
            LocationCoordinate2D l=webMercator2LonLat(flypoints.get(i).getX(), flypoints.get(i).getY());
            //lonLat2WebMercator(l.getLatitude(), l.getLongitude());
            //Graphic graphic = new Graphic(flypoints.get(i), DrawSymbol.markerSymbol);
            Graphic graphic = new Graphic(lonLat2WebMercator(l.getLatitude(), l.getLongitude()), DrawSymbol.markerSymbol);
            tempgo.getGraphics().add(graphic);

            //create text symbols
            TextSymbol bassRockSymbol =
                    new TextSymbol(20,  Double.toString(altitudes.get(i)), Color.rgb(0, 0, 230),
                            TextSymbol.HorizontalAlignment.LEFT, TextSymbol.VerticalAlignment.BOTTOM);

            tempgo.getGraphics().add(new Graphic(flypoints.get(i), bassRockSymbol));

        }

    }
    public void drawPointOnMap(Point point)
    {
       // if(tempgo.getGraphics().size()>originalsize)
       // {
           // tempgo.getGraphics().remove(originalsize-1);
       // }
        tempgo.getGraphics().add(new Graphic(point,DrawSymbol.markerSymbolcraft));
    }
    private Point lonLat2WebMercator(Double lat, Double lng){
        //坐标转换
        double x = lng *20037508.34/180;
        double y = Math.log(Math.tan((90+lat)*Math.PI/360))/(Math.PI/180);
        y = y *20037508.34/180;
        Point  mercator=new Point(x,y);
        return mercator;
    }
    private LocationCoordinate2D webMercator2LonLat(Double x, Double y){

        double lng = x/20037508.34*180;
        y = y/20037508.34*180;
        double lat= 180/Math.PI*(2*Math.atan(Math.exp(y*Math.PI/180))-Math.PI/2);
        LocationCoordinate2D lngLat = new LocationCoordinate2D(lat,lng);
        return lngLat;
    }

    ///////////////////////////////////zss/////////////

    Drawable drawcraftst;
    Drawable drawcrafted;
    Drawable disable;
    private ImageButton mBtnTakeOff,stop;

    private double droneLocationLat = 181, droneLocationLng = 181;
    private float mSpeed = 5.0f;
    private int count=0;
    private String gimbalStateStr,aircraftStateStr;
    private File file=null;

    private List<Waypoint> waypointList = new ArrayList<>();
    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;
    int craftstatus;


    private Camera camera;
    List<MediaFile> mediaFiles;//相机相关参数
    FPVOverlayWidget fpvOverlay;
    private void initUI() {
        LinearLayout  craftguidelayout=(LinearLayout)activity.findViewById(R.id.craftguide);
        craftguidelayout.setVisibility(View.VISIBLE);
        drawcraftst=activity.getDrawable(R.drawable.green_oval);
        drawcrafted=activity.getDrawable(R.drawable.green_oval_pressed);
        disable=activity.getDrawable(R.drawable.cyan_oval);
        craftstatus=0;
        mBtnTakeOff = (ImageButton) activity.findViewById(R.id.btCraftStart);
        mBtnTakeOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(craftstatus==0)//当前为停止状态 按下极为起飞
                {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT);
                    LinearLayout linea=(LinearLayout)activity.findViewById(R.id.craftlayout);
                    fpvOverlay=new FPVOverlayWidget(activity);
                    fpvOverlay.setLayoutParams(params);
                    FPVWidget fpvWidget=new FPVWidget(activity);
                    fpvWidget.setLayoutParams(params);
                    fpvOverlay.addView(fpvWidget);
                    linea.addView(fpvOverlay);
                    generatePoints();
                    drawOnMap();

                    setResultToToast(Integer.toString(flypoints.size()));
                    waypointList.clear();
                    for(int i=0;i<flypoints.size();i++)
                    {
                        double x=flypoints.get(i).getX();
                        double y=flypoints.get(i).getY();
                        LocationCoordinate2D lnglat=webMercator2LonLat(x,y);

                        addCoordinates(lnglat.getLatitude(),lnglat.getLongitude(),altitudes.get(i).floatValue());//添加坐标
                    }

                    count=0;
                    camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO,new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {//设置相机为拍照模式
                            setResultToToast("Set Mode: " + (error == null ? "Successfully" : error.getDescription()));
                        }
                    });
                    startMission();
                    mBtnTakeOff.setBackground(drawcrafted);
                    craftstatus=1;

                } else if (craftstatus==1) {
                    stopWaypointMission();
                    craftstatus=0;
                    mBtnTakeOff.setBackground(drawcraftst);
                }



            }
        });
        //mBtnTakeOff.setEnabled(false);
        mBtnTakeOff.setEnabled(false);



    }

    private void setResultToToast(final String string){
       activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addCoordinates(double Lat,double Lng,float altitude) {
        //指定飞行坐标
        //waypointList.clear();
        Waypoint mWaypoint = new Waypoint(Lat, Lng, altitude);//添加坐标step1
        //Add Waypoints to Waypoint arraylist;
        mWaypoint.addAction(new WaypointAction(WaypointActionType.STAY,1000));//停留1s
        mWaypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH,-90));//旋转镜头向下
        mWaypoint.addAction(new WaypointAction(START_TAKE_PHOTO,0));//拍照
        if (waypointMissionBuilder != null) {
            waypointList.add(mWaypoint);//添加坐标step2
            waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
        }else
        {
            waypointMissionBuilder = new WaypointMission.Builder();
            waypointList.add(mWaypoint);
            waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
        }
        //Toast.makeText(MainActivity.this, "add coordinate success", Toast.LENGTH_SHORT).show();
    }

    private void startMission(){

        if (waypointMissionBuilder == null){

            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }else
        {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }


        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            setResultToToast("加载任务成功");
            try{
                Thread thread = Thread.currentThread();
                thread.sleep(1500);//暂停1.5秒后程序继续执行
            }catch (InterruptedException e) {
                e.printStackTrace();
            }
            uploadWayPointMission();
        } else {
            setResultToToast("加载任务失败 " + error.getDescription());
        }

    }

    private void uploadWayPointMission(){

        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    setResultToToast("任务上传成功!");
                    try{
                        Thread thread = Thread.currentThread();
                        thread.sleep(5000);//暂停2秒后程序继续执行
                    }catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    startWaypointMission();
                } else {
                    setResultToToast("任务上传失败: " + error.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                }
            }
        });

    }

    private void startWaypointMission(){

        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("任务开始: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    private void stopWaypointMission(){

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        downloadMedia(waypointMissionBuilder.getWaypointList().size());//下载相片到手机
                    }
                });
                setResultToToast("停止任务: " + (error == null ? "Successfully" : error.getDescription()));
                waypointList.clear();
            }
        });
    }

    private void downloadMedia(int waypointCount){
        camera.setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD,new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {//设置相机为下载模式
                //setResultToToast("Set Mode: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });
        DJIDemoApplication.getProductInstance()
                .getCamera().getMediaManager().refreshFileList(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {//刷新文件列表
                //setResultToToast("refreshFileList: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });
        mediaFiles =DJIDemoApplication.getProductInstance()
                .getCamera().getMediaManager().getFileListSnapshot();//获取文件列表
        File destDir = new File(Environment.getExternalStorageDirectory().
                getPath() + "/Dji_Sdk_Test/");
        if(mediaFiles.size()>=waypointCount) {
            for(int i= mediaFiles.size()-1;i>mediaFiles.size()-waypointCount-1;i--) {

                if (mediaFiles.get(i) != null ){
                    mediaFiles.get(i).fetchFileData(destDir,null,new  DownloadListener<String>() {
                        @Override
                        public void onStart() {
                            // changeDescription("Start fetch media list");
                        }
                        @Override
                        public void onRateUpdate(long total, long current, long persize) {
                            // changeDescription("in progress");
                        }
                        @Override
                        public void onProgress(long l, long l1) {
                        }
                        @Override
                        public void onSuccess(String str) {
                            setResultToToast("Success " + str);
                        }
                        @Override
                        public void onFailure(DJIError djiError) {
                            setResultToToast("download: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                        }
                    });
                }

            }
        }//if
    }

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        return instance;
    }

    private void exportCameraState(){
        //输出相机参数
        try {
            file = new File(Environment.getExternalStorageDirectory().
                    getPath() + "/Dji_Sdk_Test/"+"CameraState"+count+".txt");
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            raf.seek(file.length());
            raf.write((aircraftStateStr+gimbalStateStr).getBytes());
            raf.close();
        } catch (Exception e) {
        }
    };

    public  BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };
    private void onProductConnectionChange()
    {
        initFlightController();
    }

    private void initFlightController() {

        BaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mBtnTakeOff.setEnabled(true);
                String str = product instanceof Aircraft ? "DJIAircraft" : "DJIHandHeld";
                setResultToToast(str+"无人机连接成功");
                initCamera();
                addListener();
                mFlightController = ((Aircraft) product).getFlightController();
                mFlightController.setMaxFlightRadiusLimitationEnabled(false,new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {//取消最大半径限制

                    }
                });


            }
        }else
        {
            mBtnTakeOff.setEnabled(false);
        }

        if (mFlightController != null) {

            mFlightController.setStateCallback(
                    new FlightControllerState.Callback() {
                        @Override
                        public void onUpdate(FlightControllerState
                                                     djiFlightControllerCurrentState) {
                            droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                            droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                            if(craftstatus==1)
                            {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        drawPointOnMap(lonLat2WebMercator(droneLocationLat, droneLocationLng));
                                    }
                                });
                            }

                            aircraftStateStr = "aircraft:\n"+droneLocationLat+","+droneLocationLng+","
                                    +djiFlightControllerCurrentState.getAircraftLocation().getAltitude()+"\n"
                                    +"PitchInDegrees: "+djiFlightControllerCurrentState.getAttitude().pitch+"\n"
                                    +"RollInDegrees: "+djiFlightControllerCurrentState.getAttitude().roll+"\n"
                                    +"YawInDegrees: "+djiFlightControllerCurrentState.getAttitude().yaw+"\n";
                            //updateDroneLocation();
                        }
                    });
            DJIDemoApplication.getProductInstance().getGimbal().setStateCallback(new GimbalState.Callback() {
                @Override
                public void onUpdate(@NonNull GimbalState gimbalState) {
                    gimbalStateStr="gimbal:\n"+"PitchInDegrees: "+gimbalState.getAttitudeInDegrees().getPitch()+"\n"
                            +"RollInDegrees: "+gimbalState.getAttitudeInDegrees().getRoll()+"\n"
                            +"YawInDegrees: "+gimbalState.getAttitudeInDegrees().getYaw()+"\n";
                }
            });
        }
    }

    //Add Listener for WaypointMissionOperator
    private void addListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().addListener(eventNotificationListener);
        }
    }

    private WaypointMissionOperatorListener eventNotificationListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent downloadEvent) {

        }
        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) {

        }
        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent executionEvent) {
            if(executionEvent.getProgress().isWaypointReached&&executionEvent.getProgress().targetWaypointIndex==count) {
                count++;
                exportCameraState();
            }
        }
        @Override
        public void onExecutionStart() {

        }
        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {
            if (error == null) {

                setResultToToast("拍摄任务成功完成!");
            } else {
                setResultToToast("任务失败: " +  error.getDescription());
            }
        }
    };

    private void initCamera() {

        camera = DJIDemoApplication.getProductInstance().getCamera();
        camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO,new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {//设置相机为拍照模式
                setResultToToast("相机设置: " + (error == null ? "成功" : error.getDescription()));
            }
        });
        DJIDemoApplication.getProductInstance()
                .getCamera().getMediaManager().refreshFileList(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {//刷新文件列表
                setResultToToast("刷新文件列表: " + (error == null ? "成功" : error.getDescription()));
            }
        });
    }

    public void removeListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
        }
    }

    Feature feature;
    void setOnClickListener()
    {

            DefaultMapViewOnTouchListener clicklistener=new DefaultMapViewOnTouchListener(activity, mapview) {
                @Override
                public boolean onSingleTapConfirmed(final MotionEvent e) {
                    final Point clickPoint = mMapView.screenToLocation(new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY())));
                    getClickedFeature(clickPoint,false,new clickedexcute());
                    return super.onSingleTapConfirmed(e);
                }
                @Override
                public boolean onDoubleTap(MotionEvent e) {

                    final Point clickPoint = mMapView.screenToLocation(new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY())));

                    getClickedFeature(clickPoint,true,new clickedexcute());

                    return super.onDoubleTap(e);
                }

            };

            mapview.setOnTouchListener(clicklistener);

        }

    void getClickedFeature(final Point clickPoint, final boolean bool, final AfterClicked clicked)
    {

        ServiceFeatureTable serviceFeatureTable  =st;
        int tolerance = 10;
        double mapTolerance = tolerance * mapview.getUnitsPerDensityIndependentPixel();
        // create objects required to do a selection with a query
        Envelope envelope = new Envelope(clickPoint.getX() - mapTolerance, clickPoint.getY() - mapTolerance, clickPoint.getX() + mapTolerance, clickPoint.getY() + mapTolerance, st.getSpatialReference());
        QueryParameters query = new QueryParameters();
        query.setGeometry(envelope);

        clickListener=false;
        // call select features
        final ListenableFuture<FeatureQueryResult> future = serviceFeatureTable.queryFeaturesAsync(query, ServiceFeatureTable.QueryFeatureFields.LOAD_ALL);
        //serviceFeatureTable.createFeature()

        // add done loading listener to fire when the selection returns
        future.addDoneListener(new Runnable() {

            @Override
            public void run() {//call get on the future to get the result
                FeatureQueryResult result = null;
                try {
                    result = future.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                // create an Iterator
                Iterator<Feature> iterator = result.iterator();
                if(iterator.hasNext())
                {
                    feature = iterator.next();
                    clicked.excuteWhenClicked(feature,bool);
                }
                else
                {
                    feature=null;
                }
            }
        });






    }

    private void loginAccount(){
        UserAccountManager.getInstance().logIntoDJIUserAccount(activity,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        setResultToToast("登录成功");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        setResultToToast("登录失败:"
                                + error.getDescription());
                    }
                });
    }

}
