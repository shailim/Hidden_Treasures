package com.example.hidden_treasures.util;

import android.content.Context;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.example.hidden_treasures.R;

import java.net.URL;

public class S3Helper {
    private static AmazonS3Client s3Client;
    private static String bucketName;


    private static void initializeS3(Context context) {
        BasicAWSCredentials credentials = new BasicAWSCredentials(context.getString(R.string.aws_accessID), context.getString(R.string.aws_secret_key));
        s3Client = new AmazonS3Client(credentials);
        bucketName = context.getString(R.string.s3_bucket);
    }

    // generates a signed url to access the image in s3
    public static URL getSignedUrl(Context context, String key) {
        initializeS3(context);
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, key)
                        .withMethod(HttpMethod.GET);
        return s3Client.generatePresignedUrl(generatePresignedUrlRequest);
    }
}
