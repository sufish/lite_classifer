package com.example.classifier;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int RESULT_LOAD_IMAGE = 1;
    private ImageView imageView;
    private TextView resultText;
    private Executor executor = Executors.newSingleThreadExecutor();
    private Classifier classifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button select = (Button) findViewById(R.id.select);
        imageView = (ImageView) findViewById(R.id.image);
        resultText = (TextView) findViewById(R.id.result);
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

        classifier = new Classifier("labels.txt", "mobilenet_quant_v1_224.tflite", getAssets());
        try {
            classifier.load();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "加载失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            final Bitmap originImage = BitmapFactory.decodeFile(picturePath);
            imageView.setImageBitmap(originImage);
            final ProgressDialog progress = new ProgressDialog(this);
            progress.setTitle("请等待");
            progress.setMessage("正在识别");
            progress.setCancelable(false);
            progress.show();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    final List<Result> results = classifier.classify(originImage);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.hide();
                            resultText.setText(results.get(0).getName() + ":" + String.valueOf(results.get(0).getScore()));
                        }
                    });
                }
            });
        }
    }
}
