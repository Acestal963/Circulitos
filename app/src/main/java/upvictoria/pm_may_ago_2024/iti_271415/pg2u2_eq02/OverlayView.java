package upvictoria.pm_may_ago_2024.iti_271415.pg2u2_eq02;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.opencv.core.Point;

import java.util.ArrayList;

public class OverlayView extends View {
    private static final String TAG = "OverlayView";
    private float scaleFactor = 1f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private PointF[] hojaCorners = null;
    private PointF[] previewCorners = null;
    private RectF guideRectangle = null;
    private int imageWidth = 1280;
    private int imageHeight = 720;
    private final Paint paintHoja = new Paint();
    private final Paint paintPreview = new Paint();
    private final Paint paintGuide = new Paint();
    private final Paint paintAlign = new Paint(); // For alignment lines
    private final Paint paintRect = new Paint();
    private final Paint paintCircle = new Paint();
    private final Paint paintText = new Paint();
    private final ArrayList<RectF> zonas = new ArrayList<>();
    private final ArrayList<String> textos = new ArrayList<>();
    private final ArrayList<float[]> circulos = new ArrayList<>();

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paintRect.setColor(Color.RED);
        paintRect.setStyle(Paint.Style.STROKE);
        paintRect.setStrokeWidth(4);

        paintCircle.setColor(Color.GREEN);
        paintCircle.setStyle(Paint.Style.STROKE);
        paintCircle.setStrokeWidth(4);

        paintText.setColor(Color.BLUE);
        paintText.setTextSize(20);

        paintHoja.setColor(Color.BLUE);
        paintHoja.setStyle(Paint.Style.STROKE);
        paintHoja.setStrokeWidth(6);

        paintPreview.setColor(Color.YELLOW);
        paintPreview.setStyle(Paint.Style.STROKE);
        paintPreview.setStrokeWidth(4);

        paintGuide.setColor(Color.WHITE);
        paintGuide.setStyle(Paint.Style.STROKE);
        paintGuide.setStrokeWidth(6);

        paintAlign.setColor(Color.GREEN);
        paintAlign.setStyle(Paint.Style.STROKE);
        paintAlign.setStrokeWidth(2);
        paintAlign.setAlpha(150);
    }

    public void setHojaBorde(Point[] corners) {
        if (corners == null || corners.length != 4) {
            Log.w(TAG, "Invalid hoja corners: " + (corners == null ? "null" : corners.length));
            hojaCorners = null;
        } else {
            hojaCorners = new PointF[4];
            for (int i = 0; i < 4; i++) {
                hojaCorners[i] = new PointF((float) corners[i].x, (float) corners[i].y);
            }
            Log.d(TAG, "Set hoja corners: " + hojaCorners.length);
        }
        previewCorners = null;
        invalidate();
    }

    public void setPreviewCorners(Point[] corners, int imageWidth, int imageHeight) {
        if (corners == null || corners.length < 3) {
            previewCorners = null;
            Log.w(TAG, "Invalid preview corners: " + (corners == null ? "null" : corners.length));
        } else {
            previewCorners = new PointF[corners.length];
            for (int i = 0; i < corners.length; i++) {
                if (corners[i] == null) {
                    Log.w(TAG, "Null point in preview corners at index " + i);
                    previewCorners = null;
                    return;
                }
                previewCorners[i] = new PointF((float) corners[i].x, (float) corners[i].y);
            }
            Log.d(TAG, "Set preview corners: " + previewCorners.length + ", image size: " + imageWidth + "x" + imageHeight);
        }
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        invalidate();
    }

    public void setGuideRectangle(RectF rect) {
        guideRectangle = rect;
        Log.d(TAG, "Set guide rectangle: " + (rect != null ? rect.toString() : "null"));
        invalidate();
    }

    public void clear() {
        zonas.clear();
        textos.clear();
        circulos.clear();
        hojaCorners = null;
        previewCorners = null;
        Log.d(TAG, "Cleared overlay data");
        invalidate();
    }

    public void addZona(RectF zona, String texto) {
        zonas.add(zona);
        textos.add(texto);
        Log.d(TAG, "Added zona: " + zona.toString() + ", text: " + texto);
        invalidate();
    }

    public void addCirculo(float x, float y, float r) {
        circulos.add(new float[]{x, y, r});
        Log.d(TAG, "Added circle: x=" + x + ", y=" + y + ", r=" + r);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scaleFactor, scaleFactor);

        for (int i = 0; i < zonas.size(); i++) {
            RectF z = zonas.get(i);
            canvas.drawRect(z, paintRect);
            canvas.drawText(textos.get(i), z.left + 10, z.top + 50, paintText);
        }

        for (float[] c : circulos) {
            canvas.drawCircle(c[0], c[1], c[2], paintCircle);
        }

        if (hojaCorners != null && hojaCorners.length == 4) {
            for (int i = 0; i < 4; i++) {
                PointF p1 = hojaCorners[i];
                PointF p2 = hojaCorners[(i + 1) % 4];
                canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paintHoja);
            }
            Log.d(TAG, "Drawing hoja contours");
        }

        canvas.restore();

        if (previewCorners != null) {
            float scaleX = getWidth() / (float) imageWidth;
            float scaleY = getHeight() / (float) imageHeight;
            for (int i = 0; i < previewCorners.length; i++) {
                PointF p1 = previewCorners[i];
                PointF p2 = previewCorners[(i + 1) % previewCorners.length];
                canvas.drawLine(p1.x * scaleX, p1.y * scaleY, p2.x * scaleX, p2.y * scaleY, paintPreview);
            }
            Log.d(TAG, "Drawing preview contours: " + previewCorners.length + ", scaleX=" + scaleX + ", scaleY=" + scaleY);

            // Draw dynamic alignment lines
            if (previewCorners.length == 4) {
                PointF center = new PointF(
                        (previewCorners[0].x + previewCorners[2].x) / 2 * scaleX,
                        (previewCorners[0].y + previewCorners[2].y) / 2 * scaleY
                );
                float midX = getWidth() / 2f;
                float midY = getHeight() / 2f;
                canvas.drawLine(center.x, 0, center.x, getHeight(), paintAlign); // Vertical line
                canvas.drawLine(0, center.y, getWidth(), center.y, paintAlign); // Horizontal line
                Log.d(TAG, "Drawing alignment lines at center: " + center.x + ", " + center.y);
            }
        }

        if (guideRectangle != null) {
            canvas.drawRect(guideRectangle, paintGuide);
            float textY = guideRectangle.top - 10 < 20 ? guideRectangle.top + 30 : guideRectangle.top - 10;
            canvas.drawText("Coloque la hoja dentro de este marco", guideRectangle.left + 10, textY, paintText);
            Log.d(TAG, "Drawing guide rectangle");
        }
    }

    public void setTransformParams(float scale, float offsetX, float offsetY) {
        this.scaleFactor = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        Log.d(TAG, "Set transform params: scale=" + scale + ", offsetX=" + offsetX + ", offsetY=" + offsetY);
        invalidate();
    }
}