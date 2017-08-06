package dongwei.myapplication;



import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeException;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.data.Attachment;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureEditResult;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.data.Field;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult;
import com.esri.arcgisruntime.mapping.view.MapView;


import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;

import java.util.Iterator;
import java.util.List;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import me.iwf.photopicker.PhotoPicker;
import me.iwf.photopicker.PhotoPreview;


/**
 * Created by Administrator on 2017/7/19 0019.
 */

public class DrawTool  {


    int currentmode;
    Context context;
    private GraphicsOverlay tempgo;
    private MapView mapview;
    FeatureTable fb;
    ServiceFeatureTable st;
    int status;//展示绘制状态 若有图斑在绘制中 则设置为1 否则设置为0
    private PointCollection points;
    ProgressDialog progressDialog;
    boolean STARTEDITNODE;
    double selecttolerance;
    LinearLayout calloutLayout;
    private MyListView list;
    private ArrayList<String> attachmentList = new ArrayList<>();
    List<Attachment> attachments;
    CustomList adapter;

    ArrayList<Integer> selectedindex = new ArrayList<Integer>();
    Feature feature;
    MainActivity activity;
    FeatureLayer featurelayer;
    AlertDialog.Builder builder;
    RecyclerView recyclerView;
    private Callout mCallout;
    public DrawTool(MainActivity activity,ServiceFeatureTable st, MapView mapview,FeatureTable fb,FeatureLayer ft, SpatialReference wgs84,ProgressDialog progressDialog)
    {
        this.mapview=mapview;
        this.fb=fb;
        tempgo=addGraphicsOverlay();
        points= new PointCollection(wgs84);

        status=0;
        this.progressDialog=progressDialog;
        STARTEDITNODE=false;
        selecttolerance=4;
        this.st=st;
        this.activity=activity;
        this.featurelayer=ft;
        mCallout=mapview.getCallout();
        attachments=new ArrayList<>() ;
        activity.findViewById(R.id.editbar).setVisibility(View.VISIBLE);


        initialCallout();
        initialButtons();
    }

    public void DrawOnSingleTap(int x,int y) {
        status=1;
        tempgo.getGraphics().clear();
        Graphic graphic;
        // get the point that was clicked and convert it to a point in map coordinates
        Point clickPoint = mapview.screenToLocation(new android.graphics.Point(x, y));
        points.add(clickPoint);

        if(points.size()==1)
        {
            //only one draw a point
            graphic = new Graphic(points.get(0), DrawSymbol.markerSymbol);
        }
        else if(points.size()==2)
        {
            //2 points  draw line
            graphic = new Graphic(new Polyline(points), DrawSymbol.mLineSymbol);
        }
        else
        {
            //draw polygon
            graphic = new Graphic(new Polygon(points), DrawSymbol.mFillSymbol);
            for(int i=0;i<points.size();i++)
            {
                tempgo.getGraphics().add( new Graphic(points.get(i), DrawSymbol.markerSymbol));
            }

        }

        tempgo.getGraphics().add(graphic);
    }

    public void DrawOnDoubleTap(int x,int y) {
        Graphic graphic;
        if(status==1)//编辑状态中 双击形成图形并停止编辑
        {
            status=0;//回到无编辑状态
            tempgo.getGraphics().clear();
            graphic = new Graphic(new Polygon(points), DrawSymbol.mFillSymbol1);
            tempgo.getGraphics().add(graphic);
            // check features can be added, based on edit capabilities
            // create the attributes for the feature
            java.util.Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put("dm", "null"); // Coded Values: [1: Manatee] etc...
            attributes.put("username", "null"); // Coded Values: [0: No] , [1: Yes]
            //attributes.put("comments", "Definitely a manatee");

            // Create a new feature from the attributes and an existing point geometry, and then add the feature
            Feature addedFeature = fb.createFeature(attributes, new Polygon(points));

            points.clear();
            final ListenableFuture<Void> addFeatureFuture = fb.addFeatureAsync(addedFeature);
            addFeatureFuture.addDoneListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        // check the result of the future to find out if/when the addFeatureAsync call succeeded - exception will be
                        // thrown if the edit failed
                        addFeatureFuture.get();
                        progressDialog.setTitle("更新编辑至服务器");
                        progressDialog.setMessage("正在更新 请稍等");
                        progressDialog.show();
                        applyEditsToServer();


                    } catch (InterruptedException | ExecutionException e) {
                        // executionException may contain an ArcGISRuntimeException with edit error information.
                        if (e.getCause() instanceof ArcGISRuntimeException) {
                            ArcGISRuntimeException agsEx = (ArcGISRuntimeException)e.getCause();


                        } else {
                            ;
                        }
                    }
                }
            });
        }
        else //停止编辑的状态 双击可编辑节点
        {
            // get the screen point where user tapped
        }
    }

    private GraphicsOverlay addGraphicsOverlay() {
        //create the graphics overlay
        GraphicsOverlay graphicsOverlay = new GraphicsOverlay();
        //add the overlay to the map view
        mapview.getGraphicsOverlays().add(graphicsOverlay);
        return graphicsOverlay;
    }
    /**
     * Applies edits to the FeatureService
     */
    public void applyEditsToServer() {
        tempgo.getGraphics().clear();
        STARTEDITNODE=false;
        ServiceFeatureTable sfb=(ServiceFeatureTable)fb;
        final ListenableFuture<List<FeatureEditResult>> applyEditsFuture = sfb.applyEditsAsync();
        applyEditsFuture.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    // get results of edit
                    List<FeatureEditResult> featureEditResultsList = applyEditsFuture.get();
                    if (!featureEditResultsList.get(0).hasCompletedWithErrors()) {
                       Toast.makeText(activity, "Applied Geometry Edits to Server. ObjectID: " + featureEditResultsList.get(0).getObjectId(), Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(activity, "failed to apply to server", Toast.LENGTH_SHORT).show();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    Toast.makeText(activity, "Update feature failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                }
            }
        });
    }

    public void deleteFeature(Feature feature)
    {
        final ListenableFuture<Void> deleteFeatureFuture = fb.deleteFeatureAsync(feature);
        deleteFeatureFuture.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // check the result of the future to find out if/when the addFeatureAsync call succeeded - exception will be
                    // thrown if the edit failed
                    deleteFeatureFuture.get();
                    progressDialog.setTitle("删除要素");
                    progressDialog.setMessage("正在删除 请稍等");
                    progressDialog.show();
                    applyEditsToServer();


                } catch (InterruptedException | ExecutionException e) {

                }
            }
        });
    }

    public void editeNode(Feature feature, Point clickpt) {
//||this.feature.getAttributes().get("OBJECTID").toString()!=feature.getAttributes().get("OBJECTID").toString()
        if(STARTEDITNODE==false ||this.feature.getAttributes().get("OBJECTID").toString()!=feature.getAttributes().get("OBJECTID").toString())
        {

            tempgo.getGraphics().clear();
            points.clear();
            selectedindex.clear();
            Polygon polygon = (Polygon) feature.getGeometry();
            Iterable<Point> points1=polygon.getParts().getPartsAsPoints();
            Iterator iter = points1.iterator();

            Point pt;
            while(iter.hasNext()) {
                pt =(Point)iter.next();
                points.add(pt);
                tempgo.getGraphics().add( new Graphic(pt, DrawSymbol.markerSymbol));
            }
            STARTEDITNODE=true;
            this.feature=feature;
        }
        else
        {
            //首先判断选中点
            for(int i=0;i<points.size();i++)
            {
                //确实触碰到了屏幕上的结点
                if(Math.abs(points.get(i).getX()-clickpt.getX())<selecttolerance && Math.abs(points.get(i).getY()-clickpt.getY())<selecttolerance)
                {
                    boolean repeated=false;

                    //避免重复加入一个点 首先对是凑存在数组中进行判断 若点击的是原来的点 则清除选择
                    for(int j=0;j<selectedindex.size();j++)
                    {
                        if(i==selectedindex.get(j))
                        {
                            repeated=true;
                            selectedindex.remove(j);
                            tempgo.getGraphics().add(new Graphic(new Point(points.get(selectedindex.get(j)).getX(),points.get(selectedindex.get(j)).getY()), DrawSymbol.markerSymbol));
                            break;
                        }
                    }
                    if(repeated==false) {
                        selectedindex.add(i);
                        tempgo.getGraphics().add(new Graphic(new Point(points.get(i).getX(),points.get(i).getY()), DrawSymbol.selectmarkerSymbol));
                    }


                    break;//触碰了就跳出循环
                }
            }
        }





    }


    public void LongPressEdit(Point clickpt)
    {
        if(selectedindex.size()==1)
        {
            //存在两种情况 第一种是添加到新位置 第二个是删除
            //首先碰撞测试 如果是自己 则删除
            if(Math.abs(points.get(selectedindex.get(0)).getX()-clickpt.getX())<selecttolerance && Math.abs(points.get(selectedindex.get(0)).getY()-clickpt.getY())<selecttolerance)
            {
                points.remove(selectedindex.get(0).intValue());//从points中移除

               //重新绘制
               reDrawPoints();
            }
            else
            {
                //否则 将其移动至新的点
                points.set(selectedindex.get(0).intValue(), clickpt);

                //重新绘制
                reDrawPoints();

            }

        }
        else if(selectedindex.size()==2)
        {
            //如果选中了两个点 则把新的点添加至  选择的两个点之间
            if(Math.abs(selectedindex.get(1)-selectedindex.get(0))==1)
            {
                int maxindex=selectedindex.get(1)>selectedindex.get(0)?selectedindex.get(1):selectedindex.get(0);
                //必须要是相邻的两个点
                points.add(maxindex, clickpt);

                //重新绘制
                reDrawPoints();
            }

        }
    }

    public void reDrawPoints()
    {
        selectedindex.clear();
        tempgo.getGraphics().clear();

        for(int i=0;i<points.size();i++){
            tempgo.getGraphics().add( new Graphic(points.get(i), DrawSymbol.markerSymbol));
        }
        //draw polygon
        Graphic graphic = new Graphic(new Polygon(points), DrawSymbol.mFillSymbol);
        feature.setGeometry(graphic.getGeometry());
        final ListenableFuture<Void> updateFeatureFuture = fb.updateFeatureAsync(feature);

    }

    public void setAttributeListener()
    {
        DefaultMapViewOnTouchListener clicklistener=new DefaultMapViewOnTouchListener(activity, mapview) {
            @Override
            public boolean onSingleTapConfirmed(final MotionEvent e) {
                //针对属性编辑的事件

                // get the point that was clicked and convert it to a point in map coordinates
                final Point clickPoint = mMapView.screenToLocation(new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY())));
                getClickedFeature(clickPoint);
                if(feature!=null)
                {
                    showAttributeEditor(clickPoint);
                }


                return super.onSingleTapConfirmed(e);
            }
        };

        mapview.setOnTouchListener(clicklistener);




    }

    public void setDrawListener()
    {
        DefaultMapViewOnTouchListener clicklistener=new DefaultMapViewOnTouchListener(activity, mapview) {
            @Override
            public boolean onSingleTapConfirmed(final MotionEvent e) {

                DrawOnSingleTap(Math.round(e.getX()), Math.round(e.getY()));

                return super.onSingleTapConfirmed(e);
            }
            @Override
            public boolean onDoubleTap(MotionEvent e) {

                    DrawOnDoubleTap(Math.round(e.getX()), Math.round(e.getY()));

                return super.onDoubleTap(e);
            }
        };

        mapview.setOnTouchListener(clicklistener);

    }

    public void setNodeListener()
    {
        DefaultMapViewOnTouchListener clicklistener=new DefaultMapViewOnTouchListener(activity, mapview) {
            @Override
            public boolean onSingleTapConfirmed(final MotionEvent e) {
                final Point clickPoint = mMapView.screenToLocation(new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY())));
                getClickedFeature(clickPoint);
                if(feature!=null)
                {
                    editeNode(feature,clickPoint);
                }

                return super.onSingleTapConfirmed(e);
            }
            @Override
            public boolean onDoubleTap(MotionEvent e) {

                applyEditsToServer();

                return super.onDoubleTap(e);
            }
            @Override
            public void onLongPress(MotionEvent e)
            {
                final Point clickPoint = mMapView.screenToLocation(new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY())));
                if (STARTEDITNODE==true)
                {
                    Toast.makeText(activity.getApplicationContext(),"Long pressed", Toast.LENGTH_SHORT).show();
                    //数组中包含一个节点
                    LongPressEdit(clickPoint);
                }

            }
        };

        mapview.setOnTouchListener(clicklistener);

    }
    public void setDeleteListener()
    {
        DefaultMapViewOnTouchListener clicklistener=new DefaultMapViewOnTouchListener(activity, mapview) {

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                final Point clickPoint = mMapView.screenToLocation(new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY())));
                getClickedFeature(clickPoint);

                if(feature!=null)
                {
                    deleteFeature(feature);
                }
                return super.onDoubleTap(e);
            }

        };

        mapview.setOnTouchListener(clicklistener);

    }

    void getClickedFeature(Point clickPoint)
    {

        ServiceFeatureTable serviceFeatureTable  =st;
        int tolerance = 10;
        double mapTolerance = tolerance * mapview.getUnitsPerDensityIndependentPixel();
        // create objects required to do a selection with a query
        Envelope envelope = new Envelope(clickPoint.getX() - mapTolerance, clickPoint.getY() - mapTolerance, clickPoint.getX() + mapTolerance, clickPoint.getY() + mapTolerance, featurelayer.getSpatialReference());
        QueryParameters query = new QueryParameters();
        query.setGeometry(envelope);
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
                }
                else
                {
                    feature=null;
                }

            }
        });


    }


    EditText editdm;
    EditText editbz;
    private PhotoAdapter photoAdapter;
    private ArrayList<String> selectedPhotos=new ArrayList<String>();

    private void initialCallout()
    {
        // create a text view for the callout
        calloutLayout = new LinearLayout(activity);
        calloutLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout element = new LinearLayout(activity);
        element.setOrientation(LinearLayout.HORIZONTAL);
        TextView textview = new TextView(activity);
        textview.setId(R.id.textview);
        textview.setTextColor(Color.BLACK);
        textview.setTextSize(18);
        textview.setPadding(0,10,10,0);
        textview.setBackground(null);
        textview.setText("土地代码");
        textview.setWidth(150);
        textview.setHeight(60);
        element.addView(textview);
        editdm = new EditText(activity);
        editdm.setTextColor(Color.BLACK);
        editdm.setTextSize(18);
        editdm.setPadding(0,10,10,0);
        editdm.setWidth(250);
        editdm.setHeight(60);
        element.addView(editdm);

        calloutLayout.addView(element);

        LinearLayout element1 = new LinearLayout(activity);
        element1.setOrientation(LinearLayout.HORIZONTAL);
        textview = new TextView(activity);
        textview.setId(R.id.textview);
        textview.setTextColor(Color.BLACK);
        textview.setTextSize(18);
        textview.setPadding(0,10,10,0);
        textview.setBackground(null);
        textview.setText("备注");
        textview.setWidth(150);
        textview.setHeight(60);
        element1.addView(textview);

        editbz = new EditText(activity);
        editbz.setTextColor(Color.BLACK);
        editbz.setTextSize(18);
        editbz.setPadding(0,10,10,0);
        editbz.setWidth(250);
        editbz.setHeight(60);
        element1.addView(editbz);
        calloutLayout.addView(element1);


        LinearLayout element2 = new LinearLayout(activity);
        element2.setOrientation(LinearLayout.HORIZONTAL);
        textview = new TextView(activity);
        textview.setId(R.id.textview);
        textview.setTextColor(Color.BLACK);
        textview.setTextSize(18);
        textview.setPadding(0,10,10,0);
        textview.setText("现场图片");
        textview.setWidth(150);
        textview.setHeight(60);
        element2.addView(textview);
        calloutLayout.addView(element2);



        list = new MyListView(activity);
        list.setMinimumWidth(400);
        //list.setLayoutManager(new StaggeredGridLayoutManager(3, OrientationHelper.VERTICAL));
        // create custom adapter
        adapter = new CustomList(activity, attachmentList);
        // set custom adapter on the list
        list.setAdapter(adapter);
        // listener on attachment items to download the attachment
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, final int position, long id) {
                fetchAttachmentAsync(position, view);
            }
        });

        // Build a alert dialog with specified style
        builder = new AlertDialog.Builder(activity, R.style.MyAlertDialogStyle);
        //set onlong click listener to delete the attachment
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, final int pos, long id) {

                builder.setMessage(activity.getApplication().getString(R.string.delete_query));
                builder.setCancelable(true);

                builder.setPositiveButton(activity.getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        deleteAttachment(pos);
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(activity.getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
                return true;
            }
        });


        calloutLayout.addView(list);

        recyclerView = new RecyclerView(activity);
        recyclerView.setMinimumWidth(80);
        recyclerView.setMinimumHeight(80);
        photoAdapter = new PhotoAdapter(activity, selectedPhotos);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(3, OrientationHelper.VERTICAL));
        recyclerView.setAdapter(photoAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(activity,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        if (photoAdapter.getItemViewType(position) == PhotoAdapter.TYPE_ADD) {
                            PhotoPicker.builder()
                                    .setPhotoCount(PhotoAdapter.MAX)
                                    .setShowCamera(true)
                                    .setPreviewEnabled(false)
                                    .setSelected(selectedPhotos)
                                    .start(activity);
                        } else {
                            PhotoPreview.builder()
                                    .setPhotos(selectedPhotos)
                                    .setCurrentItem(position)
                                    .start(activity);
                        }
                    }
                }));

        calloutLayout.addView(recyclerView);

        LinearLayout element0 = new LinearLayout(activity);
        element0.setOrientation(LinearLayout.HORIZONTAL);
        element0.setGravity(Gravity.CENTER);
        ImageButton ibtcancel=new ImageButton(activity);
        ImageButton ibtok=new ImageButton(activity);
        //Drawable white = getResources().getDrawable(R.drawable.tumblr_white_oval);
        //ibtok.(R.color.white);
        // ibtcancel.setBackgroundColor(R.color.white);
        //ibtcancel.setBackground(white);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        //params.leftMargin = 0;
        //ibtcancel.setLayoutParams(params);
        params.leftMargin = 100;
        ibtok.setLayoutParams(params);


        ibtcancel.setImageResource(R.drawable.attricancel);
        ibtok.setImageResource(R.drawable.attriok);
        element0.addView(ibtcancel);
        element0.addView(ibtok);


        calloutLayout.addView(element0);
        ibtcancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallout.dismiss();
            }
        });

        ibtok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                updateAttribute();
                mCallout.dismiss();
            }
        });
    }

    //从服务器获取图片
    private void fetchAttachmentAsync(final int position, final View view) {

        progressDialog.setTitle(activity.getApplication().getString(R.string.downloading_attachments));
        progressDialog.setMessage(activity.getApplication().getString(R.string.wait));
        progressDialog.show();

        // create a listenableFuture to fetch the attachment asynchronously
        final ListenableFuture<InputStream> listenableFuture = attachments.get(position).fetchDataAsync();
        listenableFuture.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    String fileName = attachmentList.get(position);
                    // create a drawable from InputStream
                    Drawable d = Drawable.createFromStream(listenableFuture.get(), fileName);
                    // create a bitmap from drawable
                    Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
                    File root = Environment.getExternalStorageDirectory();
                    File fileDir = new File(root.getAbsolutePath() + "/ArcGIS/Attachments");
                    // create folder /ArcGIS/Attachments in external storage
                    boolean isDirectoryCreated = fileDir.exists();
                    if (!isDirectoryCreated) {
                        isDirectoryCreated = fileDir.mkdirs();
                    }
                    File file = null;
                    if (isDirectoryCreated) {
                        file = new File(fileDir, fileName);
                        FileOutputStream fos = new FileOutputStream(file);
                        // compress the bitmap to PNG format
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                        fos.flush();
                        fos.close();
                    }

                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    // open the file in gallery
                    Intent i = new Intent();
                    i.setAction(android.content.Intent.ACTION_VIEW);
                    i.setDataAndType(Uri.fromFile(file), "image/png");
                    activity.startActivity(i);

                } catch (Exception e) {

                }

            }
        });
    }

    /**
     * Delete the attachment from the feature
     *
     * @param pos position of the attachment in the list view to be deleted
     */
    private void deleteAttachment(int pos) {
        progressDialog.setTitle(activity.getApplication().getString(R.string.deleting_attachments));
        progressDialog.setMessage(activity.getApplication().getString(R.string.wait));
        progressDialog.show();

        ListenableFuture<Void> deleteResult =((ArcGISFeature)feature).deleteAttachmentAsync(attachments.get(pos));
        attachmentList.remove(pos);
        adapter.notifyDataSetChanged();

        deleteResult.addDoneListener(new Runnable() {
            @Override
            public void run() {
                ListenableFuture<Void> tableResult = featurelayer.getFeatureTable().updateFeatureAsync((ArcGISFeature)feature);
                // apply changes back to the server
                tableResult.addDoneListener(new Runnable() {
                    @Override
                    public void run() {
                        applyEditsToServer();
                    }
                });
            }
        });
    }
    //更新属性时调用的函数
    private void updateAttribute()
    {
        if(editdm.getText().toString()==null&&editbz.getText().toString()==null)
        {
            Toast.makeText(activity, "null both", Toast.LENGTH_LONG).show();
        }
        else
        {
            feature.getAttributes().put("dm",editdm.getText().toString());
            feature.getAttributes().put("username",editbz.getText().toString());
            Toast.makeText(activity, editbz.getText().toString(), Toast.LENGTH_LONG).show();
            //更新属性
            ListenableFuture<Void> updatefeature = featurelayer.getFeatureTable().updateFeatureAsync(feature);
            updatefeature.addDoneListener(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setTitle("属性更新至服务器");
                    progressDialog.setMessage("正在更新 请稍等");
                    progressDialog.show();
                    applyEditsToServer();

                    //更新图片

                    for(int i=0;i<selectedPhotos.size();i=i+1)
                    {
                        progressDialog.setTitle("上传图片至服务器");
                        progressDialog.setMessage("正在上传第 "+Integer.toString(i)+" 张图片，请稍等");
                        byte[] imageByte = new byte[0];
                        try {
                            File imageFile = new File(selectedPhotos.get(0));

                            imageByte = FileUtils.readFileToByteArray(imageFile);
                            //Toast.makeText(getApplicationContext(),imageByte.length, Toast.LENGTH_SHORT).show();
                            ListenableFuture<Attachment> addResult =((ArcGISFeature) feature).addAttachmentAsync(imageByte, "image/png", System.currentTimeMillis() + ".png");
                            addResult.addDoneListener(new Runnable() {
                                @Override
                                public void run() {
                                    final ListenableFuture<Void> tableResult =featurelayer.getFeatureTable().updateFeatureAsync(feature);
                                    tableResult.addDoneListener(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressDialog.show();
                                            applyEditsToServer();
                                        }
                                    });
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            });





        }


    }

    public void applyToactivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == -1 &&
                (requestCode == PhotoPicker.REQUEST_CODE || requestCode == PhotoPreview.REQUEST_CODE)) {

            List<String> photos = null;
            if (data != null) {
                photos = data.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS);
            }
            selectedPhotos.clear();

            if (photos != null) {

                selectedPhotos.addAll(photos);
            }
            photoAdapter.notifyDataSetChanged();
        }
    }

    void initialButtons()
    {
        //相应设置的工具条button事件
        final ImageButton btattri=(ImageButton)activity.findViewById(R.id.btattribute);
        final ImageButton btnsedit=(ImageButton) activity.findViewById(R.id.btsmart);

        final ImageButton btnnode=(ImageButton) activity.findViewById(R.id.bteditnode);
        final ImageButton btndelete=(ImageButton) activity.findViewById(R.id.btdelete);

        btattri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btattri.setBackground(activity.getDrawable(R.drawable.light_blue_oval));
                btnsedit.setBackground(activity.getDrawable(R.drawable.tumblr_white_oval));
                btnnode.setBackground(activity.getDrawable(R.drawable.tumblr_white_oval));
                btndelete.setBackground(activity.getDrawable(R.drawable.tumblr_white_oval));
                mCallout.setContent(calloutLayout);
                setAttributeListener();

            }
        });

        btnsedit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btattri.setBackground(activity.getDrawable(R.drawable.tumblr_white_oval));
                btnsedit.setBackground(activity.getDrawable(R.drawable.light_blue_oval));
                btnnode.setBackground(activity.getDrawable(R.drawable.tumblr_white_oval));
                btndelete.setBackground(activity.getDrawable(R.drawable.tumblr_white_oval));
                setDrawListener();
            }


        });

        btnnode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btattri.setBackground(activity.getDrawable(R.drawable.tumblr_white_oval));
                btnsedit.setBackground(activity.getDrawable(R.drawable.tumblr_white_oval));
                btnnode.setBackground(activity.getDrawable(R.drawable.light_blue_oval));
                btndelete.setBackground(activity.getDrawable(R.drawable.tumblr_white_oval));
                setNodeListener();
                //mode=AppMode.NODEMODE;//进入节点编辑

            }
        });

        btndelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btattri.setBackground(activity.getDrawable(R.drawable.tumblr_white_oval));
                btnsedit.setBackground(activity.getDrawable(R.drawable.tumblr_white_oval));
                btnnode.setBackground(activity.getDrawable(R.drawable.tumblr_white_oval));
                btndelete.setBackground(activity.getDrawable(R.drawable.light_blue_oval));

                setDeleteListener();
            }
        });


    }

    void showAttributeEditor(final Point clickpoint)
    {

        ArcGISFeature mSelectedArcGISFeature = (ArcGISFeature) feature;
        // get the number of attachments
        final ListenableFuture<List<Attachment>> attachmentResults =
                mSelectedArcGISFeature.fetchAttachmentsAsync();
        attachmentResults.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    attachments = attachmentResults.get();
                    // if selected feature has attachments, display them in a list fashion
                    if (!attachments.isEmpty()) {
                        for (final Attachment attachment : attachments) {
                            // create a listenableFuture to fetch the attachment asynchronously
                            attachmentList.add(attachment.getName());
                        }

                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter = new CustomList(activity, attachmentList);
                                list.setAdapter(adapter);
                                adapter.notifyDataSetChanged();
                            }
                        });

                    }
                    final Map<String, Object> attr = feature.getAttributes();

                    editdm.setText( (String)attr.get("dm"));
                    editbz.setText( (String)attr.get("username"));
                    mCallout.setLocation(clickpoint);
                    // remove any existing callouts
                    if (mCallout.isShowing()) {
                        attachmentList.clear();
                        selectedPhotos.clear();

                        mCallout.dismiss();
                    }
                    mCallout.show();

                } catch (Exception e) {


                }
            }
        });
    }


}
