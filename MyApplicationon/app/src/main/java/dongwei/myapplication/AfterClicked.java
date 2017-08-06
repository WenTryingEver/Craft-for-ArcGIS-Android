package dongwei.myapplication;


import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.geometry.Point;

/**
 * Created by Administrator on 2017/8/5 0005.
 */

public interface AfterClicked {
    public void excuteWhenClicked(Feature feature,boolean bool);
}
