package com.robotique.aevaweb.buddygpt.utilis.tracking;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.google.mediapipe.tasks.components.containers.Connection;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker;
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult;
import com.robotique.aevaweb.buddygpt.R;

import java.util.List;

public class OverlayView extends View {

    private PoseLandmarkerResult results;
    private Paint pointPaint;
    private Paint linePaint;

    private float scaleFactor = 1f;
    private int imageWidth = 1;
    private int imageHeight = 1;
    private TextPaint textPaint;
    private int nbrLandmarks;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        pointPaint = new Paint();
        linePaint = new Paint();
        textPaint = new TextPaint();
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(20f); // Taille du texte à ajuster selon vos besoins
        textPaint.setTextAlign(Paint.Align.CENTER);
        initPaints();
    }

    public void clear() {
        results = null;
        pointPaint.reset();
        linePaint.reset();
        invalidate();
        initPaints();
    }

    private void initPaints() {
        pointPaint.setColor(Color.YELLOW);
        pointPaint.setStrokeWidth(10f);
        pointPaint.setStyle(Paint.Style.FILL);

        linePaint.setColor(ContextCompat.getColor(getContext(), R.color.teal_200));
        linePaint.setStrokeWidth(LANDMARK_STROKE_WIDTH);
        linePaint.setStyle(Paint.Style.FILL);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (results != null) {
            for (List<NormalizedLandmark> landmark : results.landmarks()) {
                for (NormalizedLandmark normalizedLandmark : landmark) {
                    canvas.drawPoint(
                            normalizedLandmark.x() * imageWidth * scaleFactor,
                            normalizedLandmark.y() * imageHeight * scaleFactor,
                            pointPaint
                    );
                }

                for (Connection connection : PoseLandmarker.POSE_LANDMARKS) {
                    canvas.drawLine(
                            results.landmarks().get(0).get(connection.start()).x() * imageWidth * scaleFactor,
                            results.landmarks().get(0).get(connection.start()).y() * imageHeight * scaleFactor,
                            results.landmarks().get(0).get(connection.end()).x() * imageWidth * scaleFactor,
                            results.landmarks().get(0).get(connection.end()).y() * imageHeight * scaleFactor,
                            linePaint
                    );
                }
            }
        }
        // Dessinez le texte avec le nombre de landmarks
        if (nbrLandmarks > 0) {
            canvas.drawText(nbrLandmarks + "/33", getWidth() - 30f, 190f, textPaint);
        }
    }
    public void setResults(PoseLandmarkerResult poseLandmarkerResults, int imageHeight, int imageWidth, RunningMode runningMode) {
        results = poseLandmarkerResults;

        this.imageHeight = imageHeight;
        this.imageWidth = imageWidth;

        scaleFactor = runningMode == RunningMode.IMAGE || runningMode == RunningMode.VIDEO
                ? Math.min(getWidth() * 1f / imageWidth, getHeight() * 1f / imageHeight)
                : Math.max(getWidth() * 1f / imageWidth, getHeight() * 1f / imageHeight);

        invalidate();
    }

    // Méthode pour définir le nombre de landmarks à afficher
    public void setNbrLandmarks(int nbrLandmarks) {
        this.nbrLandmarks = nbrLandmarks;
        invalidate();
    }

    private static final float LANDMARK_STROKE_WIDTH = 6f;
}