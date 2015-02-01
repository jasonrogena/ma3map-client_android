package ke.co.ma3map.android.helpers;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;

import ke.co.ma3map.android.carriers.Commute;
import ke.co.ma3map.android.views.CommuteRecyclerView;

/**
 * Created by jrogena on 01/02/2015.
 */
public class CommuteRecyclerAdapter extends RecyclerView.Adapter<CommuteRecyclerAdapter.ViewHolder>
        implements CommuteRecyclerView.OnItemClickedListener{

    private final ArrayList<Commute> commutes;
    private final Context context;
    private final ArrayList<CommuteRecyclerView> commuteRecyclerViews;

    public CommuteRecyclerAdapter(Context context, ArrayList<Commute> commutes){
        this.context = context;
        this.commutes = commutes;
        this.commuteRecyclerViews = new ArrayList<CommuteRecyclerView>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CommuteRecyclerView commuteRecyclerView = new CommuteRecyclerView(context, CommuteRecyclerAdapter.this, commuteRecyclerViews.size());
        this.commuteRecyclerViews.add(commuteRecyclerView);
        return new ViewHolder(commuteRecyclerView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setCommute(commutes.get(position));
    }

    @Override
    public int getItemCount() {
        return commutes.size();
    }

    @Override
    public void onClick(int mode) {
        if(mode == CommuteRecyclerView.MODE_RETRACTED){
                /*
                since this code is called before the onClick code in CommuteRecyclerView, retract
                all the CommuteRecyclerViews
                */
            for(int index = 0; index < commuteRecyclerViews.size(); index++){
                commuteRecyclerViews.get(index).retract();
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private CommuteRecyclerView commuteRecyclerView;
        public ViewHolder(CommuteRecyclerView commuteRecyclerView) {
            super(commuteRecyclerView);
            this.commuteRecyclerView = commuteRecyclerView;
        }

        public void setCommute(Commute commute){
            commuteRecyclerView.setCommute(commute);
            commuteRecyclerView.retract();
        }
    }
}
