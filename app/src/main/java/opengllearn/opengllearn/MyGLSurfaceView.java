package opengllearn.opengllearn;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.AttributeSet;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer {
    String vertexShader;
    String fragmentShader;

    int mTexID = -1;

    int programHandler;
    int vecShaderHandler;
    int fragShaderHandler;

    int glPositionHandler;
    int glCoordinateHandler;
    int glTextureHandler;

    private FloatBuffer verticesBuf;
    private FloatBuffer texCoordsBuf;

    int mSurfaceWidth;
    int mSurfaceHeight;
    int mImageWidth;
    int mImageHeight;

    float posX;
    float posY;

    private final float[] mScreenVerticesData = {
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f
    };

    private final float[] mVerticesData = {
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f };
    private final float[] mTexCoordsData = {
            0, 1,
            1, 1,
            0, 0,
            1, 0
    };

    Bitmap mBitmap;

    public MyGLSurfaceView(Context context) {
        super(context);
        init();
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        vertexShader = Utils.readTextFileFromRawResource(getContext(), R.raw.vertex);
        fragmentShader = Utils.readTextFileFromRawResource(getContext(), R.raw.fragment);

        verticesBuf = ByteBuffer.allocateDirect(mVerticesData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        verticesBuf.put(mVerticesData).position(0);
        texCoordsBuf = ByteBuffer.allocateDirect(mTexCoordsData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        texCoordsBuf.put(mTexCoordsData).position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(1, 1, 0, 1);

        vecShaderHandler = Utils.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        fragShaderHandler = Utils.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        programHandler = Utils.createAndLinkProgram(vecShaderHandler, fragShaderHandler);
        glPositionHandler   = GLES20.glGetAttribLocation(programHandler, "a_Position");
        glCoordinateHandler = GLES20.glGetAttribLocation(programHandler, "a_TexCoordinate");
        glTextureHandler    = GLES20.glGetUniformLocation(programHandler, "u_Texture");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mSurfaceWidth  = width;
        mSurfaceHeight = height;
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mBitmap != null && ! mBitmap.isRecycled()) {
            GLES20.glUseProgram(programHandler);

            boolean init = mBitmap.getWidth() != mImageWidth
                    || mBitmap.getHeight() != mImageHeight;

            mTexID = Utils.gen2DTexture(mTexID, mBitmap, init);

            fitScreen();

            GLES20.glVertexAttribPointer(glPositionHandler, 2,
                    GLES20.GL_FLOAT, false, 0, verticesBuf);
            GLES20.glEnableVertexAttribArray(glPositionHandler);
            GLES20.glVertexAttribPointer(glCoordinateHandler, 2,
                    GLES20.GL_FLOAT, false, 0, texCoordsBuf);
            GLES20.glEnableVertexAttribArray(glCoordinateHandler);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glUniform1i(glTextureHandler, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
//            mSurfaceTexture.updateTexImage();
        }
    }

    private void fitScreen() {
        int w = mBitmap.getWidth();
        int h = mBitmap.getHeight();
        int showW;
        int shwoH;
        if (mSurfaceWidth < (float)mSurfaceHeight * w / h) {
            showW = mSurfaceWidth;
            shwoH = mSurfaceWidth * h / w;
        } else {
            shwoH = mSurfaceHeight;
            showW = mSurfaceHeight * w / h;
        }
        Log.e("TAG", "fitScreen showW=" + showW + ", shwoH=" + shwoH);

        posX = (float)showW / mSurfaceWidth;
        posY = (float)shwoH / mSurfaceHeight;
        Log.e("TAG", "fitScreen posX=" + posX + ", posY=" + posY);

        for (int i=0; i<mVerticesData.length; i++) {
            mVerticesData[i] = (i%2==0) ?
                    mScreenVerticesData[i] * posX : mScreenVerticesData[i] * posY;
        }
        verticesBuf.put(mVerticesData).position(0);
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }
}
