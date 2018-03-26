package com.akistar.curves;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

/**
 * Created by AkiStar on 15.02.2018.
 */

public class PhotoFilterCurvesControl extends View {
    public interface PhotoFilterCurvesControlDelegate {
        void valueChanged();
    }

    private final static int CurvesSegmentNone = 0;
    private final static int CurvesSegmentBlacks = 1;
    private final static int CurvesSegmentShadows = 2;
    private final static int CurvesSegmentMidtones = 3;
    private final static int CurvesSegmentHighlights = 4;
    private final static int CurvesSegmentWhites = 5;

    private final static int GestureStateBegan = 1;
    private final static int GestureStateChanged = 2;
    private final static int GestureStateEnded = 3;
    private final static int GestureStateCancelled = 4;
    private final static int GestureStateFailed = 5;

    private int activeSegment = CurvesSegmentNone;

    private boolean isMoving;
    private boolean checkForMoving = true;

    private float lastX;
    private float lastY;

    public static float density = 1;

    private RectF actualArea = new RectF();

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintDash = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintCurve = new Paint(Paint.ANTI_ALIAS_FLAG);
    private TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private Path path = new Path();

    private PhotoFilterCurvesControlDelegate delegate;

    private MainActivity.CurvesToolValue curveValue;

    public PhotoFilterCurvesControl(Context context, MainActivity.CurvesToolValue value) {
        super(context);
        setWillNotDraw(false);
        density = MainActivity.density;

        curveValue = value;

        paint.setColor(0x99ffffff);
        paint.setStrokeWidth(dp(1));
        paint.setStyle(Paint.Style.STROKE);

        paintDash.setColor(0x99ffffff);
        paintDash.setStrokeWidth(dp(2));
        paintDash.setStyle(Paint.Style.STROKE);

        paintCurve.setColor(0xffffffff);
        paintCurve.setStrokeWidth(dp(2));
        paintCurve.setStyle(Paint.Style.STROKE);

        textPaint.setColor(0xffbfbfbf);
        textPaint.setTextSize(dp(13));
    }

    public void setDelegate(PhotoFilterCurvesControlDelegate photoFilterCurvesControlDelegate) {
        delegate = photoFilterCurvesControlDelegate;
    }

    public void setActualArea(float x, float y, float width, float height) {
        actualArea.left = x;
        actualArea.top = y;
        actualArea.right = width;
        actualArea.bottom = height;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN: {
                if (event.getPointerCount() == 1) {
                    if (checkForMoving && !isMoving) {
                        float locationX = event.getX();
                        float locationY = event.getY();
                        lastX = locationX;
                        lastY = locationY;
                        if (locationX >= actualArea.left && locationX <= actualArea.left + actualArea.right && locationY >= actualArea.top && locationY <= actualArea.top + actualArea.bottom) {
                            isMoving = true;
                        }
                        checkForMoving = false;
                        if (isMoving) {
                            handlePan(GestureStateBegan, event);
                        }
                    }
                } else {
                    if (isMoving) {
                        handlePan(GestureStateEnded, event);
                        checkForMoving = true;
                        isMoving = false;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (isMoving) {
                    handlePan(GestureStateEnded, event);
                    isMoving = false;
                }
                checkForMoving = true;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (isMoving) {
                    handlePan(GestureStateChanged, event);
                }
            }
        }
        return true;
    }

    private void handlePan(int state, MotionEvent event) {
        float locationX = event.getX();
        float locationY = event.getY();

        switch (state) {
            case GestureStateBegan: {
                selectSegmentWithPoint(locationX);
                break;
            }

            case GestureStateChanged: {
                float delta = Math.min(2, (lastY - locationY) / 8.0f);

                MainActivity.CurvesValue curveValue = null;
                switch (this.curveValue.activeType) {
                    case MainActivity.CurvesToolValue.CurvesTypeLuminance:
                        curveValue = this.curveValue.luminanceCurve;
                        break;

                    case MainActivity.CurvesToolValue.CurvesTypeRed:
                        curveValue = this.curveValue.redCurve;
                        break;

                    case MainActivity.CurvesToolValue.CurvesTypeGreen:
                        curveValue = this.curveValue.greenCurve;
                        break;

                    case MainActivity.CurvesToolValue.CurvesTypeBlue:
                        curveValue = this.curveValue.blueCurve;
                        break;

                    default:
                        break;
                }

                switch (activeSegment) {
                    case CurvesSegmentBlacks:
                        curveValue.blacksLevel = Math.max(0, Math.min(100, curveValue.blacksLevel + delta));
                        break;

                    case CurvesSegmentShadows:
                        curveValue.shadowsLevel = Math.max(0, Math.min(100, curveValue.shadowsLevel + delta));
                        break;

                    case CurvesSegmentMidtones:
                        curveValue.midtonesLevel = Math.max(0, Math.min(100, curveValue.midtonesLevel + delta));
                        break;

                    case CurvesSegmentHighlights:
                        curveValue.highlightsLevel = Math.max(0, Math.min(100, curveValue.highlightsLevel + delta));
                        break;

                    case CurvesSegmentWhites:
                        curveValue.whitesLevel = Math.max(0, Math.min(100, curveValue.whitesLevel + delta));
                        break;

                    default:
                        break;
                }

                invalidate();

                if (delegate != null) {
                    delegate.valueChanged();
                }

                lastX = locationX;
                lastY = locationY;
            }
            break;

            case GestureStateEnded:
            case GestureStateCancelled:
            case GestureStateFailed: {
                unselectSegments();
            }
            break;

            default:
                break;
        }
    }

    private void selectSegmentWithPoint(float pointx) {
        if (activeSegment != CurvesSegmentNone) {
            return;
        }
        float segmentWidth = actualArea.right / 5.0f;
        pointx -= actualArea.left;
        activeSegment = (int) Math.floor((pointx / segmentWidth) + 1);
    }

    private void unselectSegments() {
        if (activeSegment == CurvesSegmentNone) {
            return;
        }
        activeSegment = CurvesSegmentNone;
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        float segmentWidth = actualArea.right/ 5.0f;

        for (int i = 0; i < 4; i++) {
            canvas.drawLine(actualArea.left + segmentWidth + i * segmentWidth, actualArea.top, actualArea.left + segmentWidth + i * segmentWidth, actualArea.top + actualArea.bottom, paint);
        }

        canvas.drawLine(actualArea.left, actualArea.top + actualArea.bottom, actualArea.left + actualArea.right, actualArea.top, paintDash);

        MainActivity.CurvesValue curvesValue = null;
        switch (curveValue.activeType) {
            case MainActivity.CurvesToolValue.CurvesTypeLuminance:
                paintCurve.setColor(0xffffffff);
                curvesValue = curveValue.luminanceCurve;
                break;

            case MainActivity.CurvesToolValue.CurvesTypeRed:
                paintCurve.setColor(0xffed3d4c);
                curvesValue = curveValue.redCurve;
                break;

            case MainActivity.CurvesToolValue.CurvesTypeGreen:
                paintCurve.setColor(0xff10ee9d);
                curvesValue = curveValue.greenCurve;
                break;

            case MainActivity.CurvesToolValue.CurvesTypeBlue:
                paintCurve.setColor(0xff3377fb);
                curvesValue = curveValue.blueCurve;
                break;

            default:
                break;
        }

        for (int a = 0; a < 5; a++) {
            String str;
            switch (a) {
                case 0:
                    str = String.format(Locale.US, "%.2f", curvesValue.blacksLevel / 100.0f);
                    break;
                case 1:
                    str = String.format(Locale.US, "%.2f", curvesValue.shadowsLevel / 100.0f);
                    break;
                case 2:
                    str = String.format(Locale.US, "%.2f", curvesValue.midtonesLevel / 100.0f);
                    break;
                case 3:
                    str = String.format(Locale.US, "%.2f", curvesValue.highlightsLevel / 100.0f);
                    break;
                case 4:
                    str = String.format(Locale.US, "%.2f", curvesValue.whitesLevel / 100.0f);
                    break;
                default:
                    str = "";
                    break;
            }
            float width = textPaint.measureText(str);
            canvas.drawText(str, actualArea.left + (segmentWidth - width) / 2 + segmentWidth * a, actualArea.top + actualArea.bottom - dp(4), textPaint);
        }

        float[] points = curvesValue.interpolateCurve();
        invalidate();
        path.reset();
        for (int a = 0; a < points.length / 2; a++) {
            if (a == 0) {
                path.moveTo(actualArea.left + points[a * 2] * actualArea.right, actualArea.top + (1.0f - points[a * 2 + 1]) * actualArea.bottom);
            } else {
                path.lineTo(actualArea.left + points[a * 2] * actualArea.right, actualArea.top + (1.0f - points[a * 2 + 1]) * actualArea.bottom);
            }
        }

        canvas.drawPath(path, paintCurve);

    }
    public static int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }


}


