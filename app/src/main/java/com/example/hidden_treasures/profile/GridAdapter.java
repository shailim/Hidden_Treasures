package com.example.hidden_treasures.profile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.hidden_treasures.models.ParseMarker;
import com.example.hidden_treasures.R;
import com.parse.ParseFile;

import java.util.List;

public class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> {

    List<ParseMarker> markers;
    Context context;

    public GridAdapter(Context context, List<ParseMarker> markers) {
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
            ParseFile image = marker.getMedia();
            Glide.with(context).load(image.getUrl()).into(ivMarkerImage);
        }
    }
}
