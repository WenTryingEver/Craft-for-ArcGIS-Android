package dongwei.myapplication;

import com.esri.arcgisruntime.geometry.Point;

import com.esri.arcgisruntime.data.Feature;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/7/26 0026.
 */

public class CraftTask {
    public Boolean advance;
    public Feature feature;
    public int indexst;
    public int indexed;

    public CraftTask(Feature feature,Boolean advance)
    {
        this.advance=advance;
        this.feature=feature;
    }




}
