package dongwei.myapplication;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;

import java.util.Collections;
import java.util.List;

import dji.ui.widget.VisionWidget;

/**
 * Created by Administrator on 2017/7/31 0031.
 */

public class LayerAdapter extends RecyclerView.Adapter<LayerAdapter.ViewHolder> implements ItemTouchHelperAdapter{
    public List<LayerAttribute> fls;
    final static int TYPE_ADD = 1;
    final static int TYPE_LAYER = 2;
    static class  ViewHolder extends RecyclerView.ViewHolder
    {
        TextView layername;
        View layerview;
        CheckBox checkvisuable;
        SeekBar seekbar;
        TextView transtext;


        public ViewHolder(View view)
        {
            super(view);
            layerview=view;
            layername=(TextView)view.findViewById(R.id.layername);
            checkvisuable=(CheckBox)view.findViewById(R.id.checkvisuable);
            seekbar=(SeekBar)view.findViewById(R.id.seekbartrans);
            transtext=(TextView)view.findViewById(R.id.transtextview);

        }
    }
    public  LayerAdapter(List<LayerAttribute> fls)
    {
        this.fls=fls;

    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view=null;
        switch (viewType) {
            case TYPE_ADD:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.layeritemadd, parent, false);

                final ViewHolder holder1=new ViewHolder(view);
                holder1.layerview.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {


                    }
                });
                return holder1;

            case TYPE_LAYER:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.layeritem,parent,false);
                final ViewHolder holder=new ViewHolder(view);
                holder.checkvisuable.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int position=holder.getAdapterPosition();
                        fls.get(position).visible=holder.checkvisuable.isChecked();

                    }
                });
                holder.seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        int position=holder.getAdapterPosition();
                        holder.transtext.setText("透明度："+Integer.toString(i)+"%");
                        fls.get(position).transparency=i;
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }

                });
                return holder;

        }




        return null;

    }
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        if (getItemViewType(position) == TYPE_LAYER) {
            String fl=fls.get(position).layer.getName();

            holder.layername.setText(fl);
            holder.checkvisuable.setChecked(fls.get(position).visible);
            holder.transtext.setText("透明度："+Integer.toString(fls.get(position).transparency)+"%" );
            holder.seekbar.setProgress(fls.get(position).transparency);
        }


    }


    @Override public int getItemCount() {

        return fls.size()+1;
    }


    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        try {
            Collections.swap(fls,fromPosition,toPosition);

            notifyItemMoved(fromPosition,toPosition);
        }
        catch (Exception e)
        {

        }



    }

    @Override
    public void onItemDissmiss(int position) {
        //移除数据
        fls.remove(position);
        notifyItemRemoved(position);
    }
    @Override
    public int getItemViewType(int position) {
        return (position == fls.size()) ? TYPE_ADD : TYPE_LAYER;
    }

}
