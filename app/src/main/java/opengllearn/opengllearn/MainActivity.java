package opengllearn.opengllearn;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    MyGLSurfaceView vGL;
    int count;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vGL = findViewById(R.id.gl);

        findViewById(R.id.text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Bitmap b = BitmapFactory.decodeResource(getResources(),
//                        count++ % 2 == 1 ? R.mipmap.photo : R.mipmap.ic_launcher);

                Bitmap b;

                if (count++ %2 == 1) {
                    b = Bitmap.createBitmap(300, 400,
                            Bitmap.Config.ARGB_8888);
                    b.eraseColor(Color.parseColor("#FF0000"));//填充颜色
                } else {
                    b = Bitmap.createBitmap(600, 800,
                            Bitmap.Config.ARGB_8888);
                    b.eraseColor(Color.parseColor("#0000FF"));//填充颜色
                }

                vGL.setBitmap(b);
                vGL.requestRender();
            }
        });
    }
}
