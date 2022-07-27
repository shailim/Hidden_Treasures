package com.example.hidden_treasures.profile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.bumptech.glide.Glide;
import com.example.hidden_treasures.MarkerRoomDB.MarkerEntity;
import com.example.hidden_treasures.R;
import com.example.hidden_treasures.util.S3Helper;

import org.greenrobot.eventbus.EventBus;

import java.net.URL;
import java.util.Date;
import java.util.List;

public class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> {

    List<MarkerEntity> markers;
    Context context;

    public GridAdapter(Context context, List<MarkerEntity> markers, String accessId, String secret, String bucket) {
        this.context = context;
        this.markers = markers;
    }

    @NonNull
    @Override
    public GridAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.grid_item, parent, false);
        return new GridAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GridAdapter.ViewHolder holder, int position) {
        MarkerEntity marker = markers.get(position);
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

        public void bind(MarkerEntity marker) {
            String imageKey = marker.imageKey;
            // get a signed url for the image
            String url = S3Helper.getSignedUrl(context, imageKey).toString();
            Glide.with(context).load(url).into(ivMarkerImage);
            ivMarkerImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // posting new marker event
                    EventBus.getDefault().post(new ImageClickEvent(imageKey, marker.title, marker.view_count, new Date(marker.createdAt)));
                }
            });
        }
    }
}
