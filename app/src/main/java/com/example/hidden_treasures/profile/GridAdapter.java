package com.example.hidden_treasures.profile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.bumptech.glide.Glide;
import com.example.hidden_treasures.createMarker.NewMarkerEvent;
import com.example.hidden_treasures.map.MarkerDetailFragment;
import com.example.hidden_treasures.models.ParseMarker;
import com.example.hidden_treasures.R;
import com.parse.ParseFile;

import org.greenrobot.eventbus.EventBus;

import java.net.URL;
import java.util.Date;
import java.util.List;

public class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> {

    List<ParseMarker> markers;
    Context context;

    private AmazonS3Client s3Client;
    private String bucketName;

    public GridAdapter(Context context, List<ParseMarker> markers, String accessId, String secret, String bucket) {
        this.context = context;
        this.markers = markers;
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessId, secret);
        s3Client = new AmazonS3Client(credentials);
        bucketName = bucket;
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
            String imageKey = marker.getImage();
            // get a signed url for the image
            String url = getSignedUrl(imageKey).toString();
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

    // generates a signed url to access the image in s3
    private URL getSignedUrl(String key) {
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, key)
                        .withMethod(HttpMethod.GET);
        return s3Client.generatePresignedUrl(generatePresignedUrlRequest);
    }
}
