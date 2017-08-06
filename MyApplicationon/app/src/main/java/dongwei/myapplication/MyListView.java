package dongwei.myapplication;

import android.content.Context;
import android.view.View;
import android.widget.ListView;

import java.util.List;

/**
 * Created by Administrator on 2017/7/25 0025.
 */

public class MyListView extends ListView{
    public MyListView(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxWidth = meathureWidthByChilds() + getPaddingLeft() + getPaddingRight();
        super.onMeasure(MeasureSpec.makeMeasureSpec(maxWidth,MeasureSpec.UNSPECIFIED),heightMeasureSpec);//注意，这个地方一定是MeasureSpec.UNSPECIFIED
    }
    public int meathureWidthByChilds() {
        int maxWidth = 0;
        View view = null;
        for (int i = 0; i < getAdapter().getCount(); i++) {
            view = getAdapter().getView(i, view, this);
            view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            if (view.getMeasuredWidth() > maxWidth){
                maxWidth = view.getMeasuredWidth();
            }
            view = null;
        }
        return maxWidth;
    }
}
