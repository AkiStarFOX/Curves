package com.akistar.curves;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public GLSurfaceView glView;
    private Context context;
    public static float density;
    private final static int curveGranularity = 100;
    private final static int curveDataStep = 2;
    public static CurvesToolValue curvesToolValue;
    public float x;
    public float y;
    public static float width;
    public static float height;
    private Bitmap image;
    private float overviewScale;
    private float translationX;
    private float translationY;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!supportES2()) {
            finish();
            return;
        }
        curvesToolValue = new CurvesToolValue();
        glView = (GLSurfaceView) findViewById(R.id.glView);
        glView.setEGLContextClientVersion(2);
        glView.setPreserveEGLContextOnPause(true);
        glView.setRenderer(new Renderer(getApplicationContext()));

        density = getApplicationContext().getResources().getDisplayMetrics().density;

        image = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.cat);


        final GLSurfaceView layout =  glView;
        ViewTreeObserver vto = layout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener (new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ConstraintLayout main = findViewById(R.id.id2);
                layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                 width  = layout.getMeasuredWidth();
                 height = layout.getMeasuredHeight();
                overviewScale = Math.min((float) width / image.getWidth(), (float)height / image.getHeight());
                translationX = width / 2.0f - (image.getWidth() / 2.0f) * overviewScale;
                translationY = height / 2.0f - (image.getHeight() / 2.0f) * overviewScale;


                PhotoFilterCurvesControl curves = new PhotoFilterCurvesControl(getApplicationContext(),curvesToolValue);
                curves.setActualArea(x+translationX,y+translationY,image.getWidth()*overviewScale,image.getHeight()*overviewScale);
                main.addView(curves);

            }
        });


        Log.d("TAG",width + " ");






    }
    @Override
    protected void onPause() {
        super.onPause();
        glView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glView.onResume();
    }

    private boolean supportES2() {
        ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();

        return (configurationInfo.reqGlEsVersion >= 0x20000);
    }
    public static class CurvesValue {

        public float blacksLevel = 0.0f;
        public float shadowsLevel = 25.0f;
        public float midtonesLevel = 50.0f;
        public float highlightsLevel = 75.0f;
        public float whitesLevel = 100.0f;

        public float previousBlacksLevel = 0.0f;
        public float previousShadowsLevel = 25.0f;
        public float previousMidtonesLevel = 50.0f;
        public float previousHighlightsLevel = 75.0f;
        public float previousWhitesLevel = 100.0f;

        public float[] cachedDataPoints;

        public float[] getDataPoints() {
            if (cachedDataPoints == null) {
                interpolateCurve();
            }
            return cachedDataPoints;
        }



        public float[] interpolateCurve() {
            float[] points = new float[] {
                    -0.001f, blacksLevel / 100.0f,
                    0.0f, blacksLevel / 100.0f,
                    0.25f, shadowsLevel / 100.0f,
                    0.5f, midtonesLevel / 100.0f,
                    0.75f, highlightsLevel / 100.0f,
                    1f, whitesLevel / 100.0f,
                    1.001f, whitesLevel / 100.0f
            };

            ArrayList<Float> dataPoints = new ArrayList<>(100);
            ArrayList<Float> interpolatedPoints = new ArrayList<>(100);

            interpolatedPoints.add(points[0]);
            interpolatedPoints.add(points[1]);

            for (int index = 1; index < points.length / 2 - 2; index++) {
                float point0x = points[(index - 1) * 2];
                float point0y = points[(index - 1) * 2 + 1];
                float point1x = points[(index) * 2];
                float point1y = points[(index) * 2 + 1];
                float point2x = points[(index + 1) * 2];
                float point2y = points[(index + 1) * 2 + 1];
                float point3x = points[(index + 2) * 2];
                float point3y = points[(index + 2) * 2 + 1];


                for (int i = 1; i < curveGranularity; i++) {
                    float t = (float) i * (1.0f / (float) curveGranularity);
                    float tt = t * t;
                    float ttt = tt * t;

                    float pix = 0.5f * (2 * point1x + (point2x - point0x) * t + (2 * point0x - 5 * point1x + 4 * point2x - point3x) * tt + (3 * point1x - point0x - 3 * point2x + point3x) * ttt);
                    float piy = 0.5f * (2 * point1y + (point2y - point0y) * t + (2 * point0y - 5 * point1y + 4 * point2y - point3y) * tt + (3 * point1y - point0y - 3 * point2y + point3y) * ttt);

                    piy = Math.max(0, Math.min(1, piy));

                    if (pix > point0x) {
                        interpolatedPoints.add(pix);
                        interpolatedPoints.add(piy);
                    }

                    if ((i - 1) % curveDataStep == 0) {
                        dataPoints.add(piy);
                    }
                }
                interpolatedPoints.add(point2x);
                interpolatedPoints.add(point2y);
            }
            interpolatedPoints.add(points[12]);
            interpolatedPoints.add(points[13]);

            cachedDataPoints = new float[dataPoints.size()];
            for (int a = 0; a < cachedDataPoints.length; a++) {
                cachedDataPoints[a] = dataPoints.get(a);
            }
            float[] retValue = new float[interpolatedPoints.size()];
            for (int a = 0; a < retValue.length; a++) {
                retValue[a] = interpolatedPoints.get(a);
            }
            return retValue;
        }

        public boolean isDefault() {
            return Math.abs(blacksLevel - 0) < 0.00001 && Math.abs(shadowsLevel - 25) < 0.00001 && Math.abs(midtonesLevel - 50) < 0.00001 && Math.abs(highlightsLevel - 75) < 0.00001 && Math.abs(whitesLevel - 100) < 0.00001;
        }
    }


    public static class CurvesToolValue {

        public CurvesValue luminanceCurve = new CurvesValue();
        public CurvesValue redCurve = new CurvesValue();
        public CurvesValue greenCurve = new CurvesValue();
        public CurvesValue blueCurve = new CurvesValue();
        public ByteBuffer curveBuffer = null;

        public static int activeType;

        public final static int CurvesTypeLuminance = 0;
        public final static int CurvesTypeRed = 1;
        public final static int CurvesTypeGreen = 2;
        public final static int CurvesTypeBlue = 3;

        public CurvesToolValue() {
            curveBuffer = ByteBuffer.allocateDirect(200 * 4);
            curveBuffer.order(ByteOrder.LITTLE_ENDIAN);
        }

        public void fillBuffer() {
            curveBuffer.position(0);
            float[] luminanceCurveData = luminanceCurve.getDataPoints();
            float[] redCurveData = redCurve.getDataPoints();
            float[] greenCurveData = greenCurve.getDataPoints();
            float[] blueCurveData = blueCurve.getDataPoints();
            for (int a = 0; a < 200; a++) {
                curveBuffer.put((byte) (redCurveData[a] * 255));
                curveBuffer.put((byte) (greenCurveData[a] * 255));
                curveBuffer.put((byte) (blueCurveData[a] * 255));
                curveBuffer.put((byte) (luminanceCurveData[a] * 255));
            }
            curveBuffer.position(0);
        }

        public boolean shouldBeSkipped() {
            return luminanceCurve.isDefault() && redCurve.isDefault() && greenCurve.isDefault() && blueCurve.isDefault();
        }
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_red:
                if (checked)

                    CurvesToolValue.activeType=1;

                break;
            case R.id.radio_green:
                if (checked)
                    CurvesToolValue.activeType=2;

                break;
            case R.id.radio_blue:
                if (checked)
                    CurvesToolValue.activeType=3;

                break;
            case R.id.radio_all:
                if (checked)
                    CurvesToolValue.activeType=0;

                break;
        }
    }
}
