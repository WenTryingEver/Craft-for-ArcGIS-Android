package dongwei.myapplication;

import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;

/**
 * Created by Administrator on 2017/8/1 0001.
 */

public class LayerAttribute {
    public boolean visible;
    public int transparency;
    public Layer layer;
    public ServiceFeatureTable serviceFeatureTable;

    public LayerAttribute(ServiceFeatureTable st, Layer layer, boolean visible, int transparency)
    {
        this.layer=layer;
        this.visible=visible;
        this.transparency=transparency;
        this.serviceFeatureTable=st;

    }
}
