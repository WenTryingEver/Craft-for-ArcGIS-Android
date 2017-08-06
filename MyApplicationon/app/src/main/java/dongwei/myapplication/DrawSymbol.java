package dongwei.myapplication;

/**
 * Created by Administrator on 2017/7/19 0019.
 */

import android.graphics.Color;

import com.esri.arcgisruntime.symbology.FillSymbol;
import com.esri.arcgisruntime.symbology.LineSymbol;
import com.esri.arcgisruntime.symbology.MarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;

/**
 * 要素编辑状态符号化信息
 * Created by gis-luq on 15/5/21.
 */
public class DrawSymbol {

    private static int SIZE = 10;//节点大小

    public static MarkerSymbol markerSymbol  = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE,Color.RED, SIZE);
    public static MarkerSymbol markerSymbolcraft  = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE,Color.RED, 4);

    public static SimpleMarkerSymbol mRedMarkerSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE,Color.RED, SIZE);
    public static SimpleMarkerSymbol mBlackMarkerSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE,Color.BLACK, SIZE);
    public static SimpleMarkerSymbol mGreenMarkerSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE,Color.GREEN, SIZE);
    public static LineSymbol mLineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.DASH,Color.GRAY, 2);

    public static SimpleLineSymbol outlineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.DASH, Color.rgb(0, 0, 128), 1);
    public static SimpleFillSymbol mFillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.DIAGONAL_CROSS, Color.rgb(0, 0, 250), outlineSymbol);
    public static SimpleFillSymbol mFillSymbol1 = new SimpleFillSymbol(SimpleFillSymbol.Style.DIAGONAL_CROSS, Color.rgb(250, 0, 0), outlineSymbol);
    public static MarkerSymbol selectmarkerSymbol  = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE,Color.BLUE, SIZE);

}
