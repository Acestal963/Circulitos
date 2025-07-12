package upvictoria.pm_may_ago_2024.iti_271415.pg2u2_eq02;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    static {
        if (!OpenCVLoader.initDebug()) {
            throw new RuntimeException("OpenCV no se pudo cargar.");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnConfig = findViewById(R.id.btnConfig);
        Button btnCamera = findViewById(R.id.btnCamera);

        btnConfig.setOnClickListener(v -> {
            Intent intent = new Intent(this, ConfigActivity.class);
            startActivity(intent);
        });

        btnCamera.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        });
    }
}