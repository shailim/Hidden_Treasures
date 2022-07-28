package com.example.hidden_treasures.profile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hidden_treasures.MarkerRoomDB.MarkerEntity;
import com.example.hidden_treasures.R;
import com.example.hidden_treasures.collections.ParseCollection;
import com.example.hidden_treasures.markers.ParseMarker;
import com.example.hidden_treasures.util.S3Helper;

import org.greenrobot.eventbus.EventBus;

import java.util.Date;
import java.util.List;

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.ViewHolder> {

    List<ParseMarker> markers;
    Context context;

    public CollectionAdapter(Context context, List<ParseMarker> markers) {
        this.context = context;
        this.markers = markers;
    }

    @NonNull
    @Override
    public CollectionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.grid_item, parent, false);
        return new CollectionAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectionAdapter.ViewHolder holder, int position) {
        ParseMarker marker = markers.get(position);
        holder.bind(marker);
    }

    @Override
    public int getItemCount() {
        return markers.size();
    }

    public class ViewHolder extends  RecyclerView.ViewHolder {

        ImageView ivMarkerImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivMarkerImage = itemView.findViewById(R.id.ivMarkerImage);
        }

        public void bind(ParseMarker marker) {
            String imageKey = marker.getImage();
            // get a signed url for the image
            String url = S3Helper.getSignedUrl(context, imageKey).toString();
            Glide.with(context).load(url).into(ivMarkerImage);
            ivMarkerImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // posting new marker event
                    EventBus.getDefault().post(new ImageClickEvent(imageKey, marker.getTitle(), marker.getViewCount(), new Date(marker.getTime())));
                }
            });
        }
    }
}
