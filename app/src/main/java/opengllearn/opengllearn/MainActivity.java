package opengllearn.opengllearn;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    MyGLSurfaceView vGL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vGL = findViewById(R.id.gl);

        findViewById(R.id.text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap b = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round);
                vGL.setBitmap(b);
                vGL.requestRender();
            }
        });
    }
}
