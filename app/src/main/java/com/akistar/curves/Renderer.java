package com.akistar.curves;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.support.constraint.ConstraintLayout;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.GL_LINE_STRIP;
import static android.opengl.GLES20.glDrawArrays;



public class Renderer implements GLSurfaceView.Renderer {
    public static float texCoords[] = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
    };

    public static short inds[] = {
            1, 0, 3,
            1, 3, 2,

    };

    private int _programId;
    private int _programIdGraf;
    private int _textureId;
    private Context _context;


    float overviewScale;
    float translationX;
    float translationY;


    public static int _glViewWidth;
    public static int _glViewHeight;

    private int _aPositionLocation;
    private int _aPositionLocationGraf;
    private int _aTexCoordLocation;
    private int _uProjMLocation;
    private int _uModelMLocation;
    private int _uTextureLocation;



    public static Bitmap image;

    public FloatBuffer _vertices;
    public FloatBuffer _texCoords;
    public ShortBuffer _indices;

    public int _imgW;
    public int _imgH;

    private float[] _screenProjM = new float[16];
    private float[] _modelM = new float[16];



    public static MainActivity.CurvesToolValue curvesToolValue;
    private int[] curveTextures = new int[1];
    private int curvesImageHandle;


    public Renderer(Context context) {
        _context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {

        int vertextShaderId = ShaderUtils.createShader(_context, GLES20.GL_VERTEX_SHADER, R.raw.vertex_shader);
        int fragmentShaderId = ShaderUtils.createShader(_context, GLES20.GL_FRAGMENT_SHADER, R.raw.fragment_shader);


        _programId = ShaderUtils.createProgram(vertextShaderId, fragmentShaderId);


        _aPositionLocation = GLES20.glGetAttribLocation(_programId, "a_Position");
        _aTexCoordLocation = GLES20.glGetAttribLocation(_programId, "a_TexCoord");

        _uProjMLocation = GLES20.glGetUniformLocation(_programId, "u_ProjM");
        _uModelMLocation = GLES20.glGetUniformLocation(_programId, "u_ModelM");
        _uTextureLocation = GLES20.glGetUniformLocation(_programId, "sourceImage");

        
        curvesImageHandle = GLES20.glGetUniformLocation(_programId, "curvesImage");



        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        image = BitmapFactory.decodeResource(_context.getResources(), R.drawable.cat);

        _imgW = image.getWidth();
        _imgH = image.getHeight();


        // инициализируем буферы вершин, индексов и текстурных координат
        float[] verts = arrayFromRectF(new RectF(0, 0, _imgW, _imgH));

        _vertices = ByteBuffer.allocateDirect(verts.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        _vertices.put(verts);
        _vertices.position(0);

        _indices = ByteBuffer.allocateDirect(verts.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        _indices.put(inds);
        _indices.position(0);

        _texCoords = ByteBuffer.allocateDirect(texCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        _texCoords.put(texCoords);
        _texCoords.position(0);

        _textureId = TextureUtils.loadTexture(image, true);

        curvesToolValue=MainActivity.curvesToolValue;



    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {
        _glViewWidth = i;
        _glViewHeight = i1;

        Matrix.orthoM(_screenProjM, 0, 0, i, i1, 0, 0, 1);

        // вычисляем насколько нужно уменьшить и сдвинуть изображение, чтобы оно влезло в
        // glSurfaceView и было по центру
        overviewScale = Math.min((float) i / _imgW, (float)i1 / _imgH);
        translationX = i / 2.0f - (_imgW / 2.0f) * overviewScale;
        translationY = i1 / 2.0f - (_imgH / 2.0f) * overviewScale;


        // задаем матрицу модели
        Matrix.setIdentityM(_modelM, 0);
        Matrix.translateM(_modelM, 0, translationX, translationY, 0);
        Matrix.scaleM(_modelM, 0, overviewScale, overviewScale, 1.0f);
    }



    private void shaderParams() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, _textureId);


        GLES20.glUniform1i(_uTextureLocation, 0);



        GLES20.glUniformMatrix4fv(_uProjMLocation, 1, false, _screenProjM, 0);
        GLES20.glUniformMatrix4fv(_uModelMLocation, 1, false, _modelM, 0);


        GLES20.glEnableVertexAttribArray(_aPositionLocation);
        GLES20.glVertexAttribPointer(_aPositionLocation, 3, GLES20.GL_FLOAT, false, 0, _vertices);

        GLES20.glEnableVertexAttribArray(_aTexCoordLocation);
        GLES20.glVertexAttribPointer(_aTexCoordLocation, 2, GLES20.GL_FLOAT, false, 0, _texCoords);


    }
    private void curveShaderParams(){
        curvesToolValue.fillBuffer();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GL10.GL_TEXTURE_2D, curveTextures[0]);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 200, 1, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, curvesToolValue.curveBuffer);
        GLES20.glUniform1i(curvesImageHandle, 1);
    }



    @Override
    public void onDrawFrame(GL10 gl10) {


        GLES20.glClearColor(1, 1, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(_programId);
        GLES20.glViewport(0, 0, _glViewWidth, _glViewHeight);

        shaderParams();
        curveShaderParams();
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, _indices);







    }

    private float[] arrayFromRectF(RectF rect) {
        float x1 = rect.left;
        float x2 = rect.right;
        float y1 = rect.bottom;
        float y2 = rect.top;


        PointF one = new PointF(x1, y2);
        PointF two = new PointF(x1, y1);
        PointF three = new PointF(x2, y1);
        PointF four = new PointF(x2, y2);

        return new float[]
                {
                        one.x, one.y, 0,
                        two.x, two.y, 0,
                        three.x, three.y, 0,
                        four.x, four.y, 0,
                };
    }



}
