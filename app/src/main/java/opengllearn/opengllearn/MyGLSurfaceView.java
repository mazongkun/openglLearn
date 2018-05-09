package opengllearn.opengllearn;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.AttributeSet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer {
    String vertexShader;
    String fragmentShader;

    int mTexID;

    int programHandler;
    int vecShaderHandler;
    int fragShaderHandler;

    int glPositionHandler;
    int glCoordinateHandler;
    int glMatrixHandler;
    int glTextureHandler;

    private FloatBuffer verticesBuf;
    private FloatBuffer texCoordsBuf;

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
    private final float[] mMatrix = {
            1,0,0,0,
            0,1,0,0,
            0,0,1,0,
            0,0,0,1
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
//        glMatrixHandler    = GLES20.glGetUniformLocation(programHandler, "u_Matrix");
        glTextureHandler    = GLES20.glGetUniformLocation(programHandler, "u_Texture");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mBitmap != null && ! mBitmap.isRecycled()) {
            GLES20.glUseProgram(programHandler);
            genTexture();

            GLES20.glVertexAttribPointer(glPositionHandler, 2,
                    GLES20.GL_FLOAT, false, 0, verticesBuf);
            GLES20.glEnableVertexAttribArray(glPositionHandler);
            GLES20.glVertexAttribPointer(glCoordinateHandler, 2,
                    GLES20.GL_FLOAT, false, 0, texCoordsBuf);
            GLES20.glEnableVertexAttribArray(glCoordinateHandler);

//            GLES20.glUniformMatrix4fv(glMatrixHandler, 1, false, mMatrix, 0);


            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glUniform1i(glTextureHandler, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        }

    }

    public void setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    private void genTexture() {
        int []texID = new int[1];
        GLES20.glGenTextures(1, texID, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texID[0]);
        mTexID = texID[0];

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_REPEAT);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
//        bitmap.recycle();
    }
}
