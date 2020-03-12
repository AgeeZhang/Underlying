package com.zzj.underlyingTest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.zzj.underlying.DataConver;
import com.zzj.underlyingTest.R;

import static android.os.Environment.DIRECTORY_PICTURES;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i("test", DataConver.getTestString());
        Log.i("dateTime",System.currentTimeMillis()+"");
        String url = Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES) + "/20190403164349.jpg";
        Bitmap bitmap = BitmapFactory.decodeFile(url);
        bitmap = ImageUtil.alignBitmapForNv21(bitmap);
        byte[] data = ImageUtil.bitmapToNv21(bitmap, bitmap.getWidth(), bitmap.getHeight());
        data=DataConver.rotateYUV420Degree90(data,bitmap.getWidth(),bitmap.getHeight());
        Log.i("test", DataConver.getTestString());
        Log.i("dateTime",System.currentTimeMillis()+"");
    }
}
