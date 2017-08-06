package dongwei.myapplication;

import android.support.v7.widget.RecyclerView;

/**
 * Created by Administrator on 2017/8/1 0001.
 */
public interface ItemTouchHelperAdapter {
    //数据交换
    void onItemMove(int fromPosition,int toPosition);
    //数据删除
    void onItemDissmiss(int position);


}