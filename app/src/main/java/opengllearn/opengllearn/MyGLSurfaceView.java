package opengllearn.opengllearn;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer {
    final String TAG = getClass().getSimpleName();

    String vertexShader;
    String fragmentShader;

    int mTexID = -1;
    int mCircleTexID = -1;

    int programHandler;
    int vecShaderHandler;
    int fragShaderHandler;

    int glPositionHandler;
    int glCoordinateHandler;
    int glTextureHandler;

    private FloatBuffer circleVerticesBuf;
    private FloatBuffer verticesBuf;
    private FloatBuffer texCoordsBuf;

    int mSurfaceWidth;
    int mSurfaceHeight;
    int mImageWidth;
    int mImageHeight;

    float posX;
    float posY;

    float vertexWidth;
    float vertexHeight;
    float eyeWidth;
    float eyeHeight;
    float[] center = {0, 0};
    float[] lastEyePos = new float[4*2];

    float degree;
    float dis;

    onEyeMoveListener listener;

    public interface onEyeMoveListener {
        void onEyeMoved(float degree, float dis);
    }

    private final float[] mScreenVerticesData = {
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f
    };
    private final float[] mCircleVerticesData = {
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

            drawCircle();
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

    boolean init = true;
    private void positionCircle() {
        if (! init)
            return;
init =false;
        float showRatio = 150f/1080;
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();

        if (width < height) {
            eyeWidth  = 2*Math.abs(mVerticesData[0]) * showRatio;
            eyeHeight = 2*Math.abs(mVerticesData[1]) * showRatio * width / height;
            for (int i=0; i<mCircleVerticesData.length; i++) {
                mCircleVerticesData[i] = (i%2==0) ?
                        showRatio * mVerticesData[i] : showRatio * mVerticesData[i]*width/height;
            }
        } else {
            eyeHeight = 2*Math.abs(mVerticesData[1]) * showRatio;
            eyeWidth  = 2*Math.abs(mVerticesData[0]) * showRatio * height / width;
            for (int i=0; i<mCircleVerticesData.length; i++) {
                mCircleVerticesData[i] = (i%2==1) ?
                        showRatio * mVerticesData[i] : showRatio * mVerticesData[i]*height/width;
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void drawCircle() {
        positionCircle();
        Drawable drawable = getContext().getResources().getDrawable(R.drawable.oval_edge, null);
        Bitmap centerBitmap = BitmapUtil.getBitmapWithDrawable(drawable);

        mCircleTexID = Utils.gen2DTexture(mCircleTexID, centerBitmap, false);
//        mTexID = Utils.gen2DTexture(mTexID, centerBitmap, false);

        circleVerticesBuf = ByteBuffer.allocateDirect(mCircleVerticesData.length*4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        circleVerticesBuf.put(mCircleVerticesData).position(0);

        GLES20.glVertexAttribPointer(glPositionHandler, 2,
                GLES20.GL_FLOAT, false, 0, circleVerticesBuf);
        GLES20.glEnableVertexAttribArray(glPositionHandler);
        GLES20.glVertexAttribPointer(glCoordinateHandler, 2,
                GLES20.GL_FLOAT, false, 0, texCoordsBuf);
        GLES20.glEnableVertexAttribArray(glCoordinateHandler);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(glTextureHandler, 0);



//        GLES20.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GL10.GL_BLEND);

//        GLES20.glEnable(GL10.GL_DEPTH_TEST);
//        GLES20.glEnable(GL10.GL_ALPHA_TEST);  // Enable Alpha Testing (To Make BlackTansparent)

//        GLES20.glFun  glAlphaFunc(GL10.GL_GREATER,0.1f);  // Set Alpha Testing (To Make Black Transparent)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public void setListener(onEyeMoveListener listener) {
        this.listener = listener;
    }

    float lastX = -1, lastY = -1;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event != null) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    float x = event.getX();
                    float y = event.getY();
                    // y
                    boolean isEgde = false;
                    if (Math.abs(y - lastY) > 3) {
                        float dis = Math.abs(y - lastY) * 0.0015f;
                        if (y - lastY > 0) { // down
                            for (int i=1; i<mCircleVerticesData.length; i+=2) {
                                if (mCircleVerticesData[i] - dis < -1) {
                                    isEgde = true;
                                    break;
                                }
                            }

                            if (isEgde) {
                                mCircleVerticesData[1] = -1;
                                mCircleVerticesData[3] = -1;
                                mCircleVerticesData[5] = -1+eyeHeight;
                                mCircleVerticesData[7] = -1+eyeHeight;
                            } else {
                                mCircleVerticesData[1] -= dis;
                                mCircleVerticesData[3] -= dis;
                                mCircleVerticesData[5] -= dis;
                                mCircleVerticesData[7] -= dis;
                            }
                        } else { // up
                            for (int i=1; i<mCircleVerticesData.length; i+=2) {
                                if (mCircleVerticesData[i] + dis > 1) {
                                    isEgde = true;
                                    break;
                                }
                            }

                            if (isEgde) {
                                mCircleVerticesData[1] = 1;
                                mCircleVerticesData[3] = 1;
                                mCircleVerticesData[5] = 1-eyeHeight;
                                mCircleVerticesData[7] = 1-eyeHeight;
                            } else {
                                mCircleVerticesData[1] += dis;
                                mCircleVerticesData[3] += dis;
                                mCircleVerticesData[5] += dis;
                                mCircleVerticesData[7] += dis;
                            }
                        }
                    }

                    // x
                    isEgde = false;
                    if (Math.abs(x - lastX) > 3) {
                        float dis = Math.abs(x - lastX) * 0.0015f * mSurfaceHeight/mSurfaceWidth;
                        if (x - lastX > 0) { // right
                            for (int i=0; i<mCircleVerticesData.length; i+=2) {
                                if (mCircleVerticesData[i] + dis > 1) {
                                    isEgde = true;
                                    break;
                                }
                            }

                            if (isEgde) {
                                mCircleVerticesData[0] = 1-eyeWidth;
                                mCircleVerticesData[2] = 1;
                                mCircleVerticesData[4] = 1-eyeWidth;
                                mCircleVerticesData[6] = 1;
                            } else {
                                mCircleVerticesData[0] += dis;
                                mCircleVerticesData[2] += dis;
                                mCircleVerticesData[4] += dis;
                                mCircleVerticesData[6] += dis;
                            }
                        } else { // left
                            for (int i=0; i<mCircleVerticesData.length; i+=2) {
                                if (mCircleVerticesData[i] - dis < -1) {
                                    isEgde = true;
                                    break;
                                }
                            }

                            if (isEgde) {
                                mCircleVerticesData[0] = -1+eyeWidth;
                                mCircleVerticesData[2] = -1;
                                mCircleVerticesData[4] = -1+eyeWidth;
                                mCircleVerticesData[6] = -1;
                            } else {
                                mCircleVerticesData[0] -= dis;
                                mCircleVerticesData[2] -= dis;
                                mCircleVerticesData[4] -= dis;
                                mCircleVerticesData[6] -= dis;
                            }
                        }
                    }
                    requestRender();
                    lastX = event.getX();
                    lastY = event.getY();
                    calculateParams();
                    break;

                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
                    lastY = event.getY();
                    break;
                case MotionEvent.ACTION_UP:
                    break;
                case MotionEvent.ACTION_CANCEL:
                    break;
            }

            return true;
        } else {
            return super.onTouchEvent(event);
        }
    }

    private void calculateParams() {
//        if (listener == null)
//            return;

        float eyeX = (mCircleVerticesData[0]
                + mCircleVerticesData[2]
                + mCircleVerticesData[4]
                + mCircleVerticesData[6])/4;
        float eyeY = (mCircleVerticesData[1]
                + mCircleVerticesData[3]
                + mCircleVerticesData[5]
                + mCircleVerticesData[7])/4;
        float degree = (float) Math.atan2(1000*(eyeY-center[1])/mSurfaceHeight, 1000*(eyeX-center[0])/mSurfaceWidth);
        Log.e(TAG, "degree=" + degree*180/Math.PI);
    }
}
