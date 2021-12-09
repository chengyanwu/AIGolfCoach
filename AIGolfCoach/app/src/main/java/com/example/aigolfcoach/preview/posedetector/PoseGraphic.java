/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.aigolfcoach.preview.posedetector;

import static com.google.mlkit.vision.common.PointF3D.from;
import static java.lang.Math.max;
import static java.lang.Math.min;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.util.Log;

import com.google.common.primitives.Ints;
import com.google.mlkit.vision.common.PointF3D;
import com.example.aigolfcoach.GraphicOverlay;
import com.example.aigolfcoach.GraphicOverlay.Graphic;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;
import java.util.List;
import java.util.Locale;

/** Draw the detected pose in preview. */
public class PoseGraphic extends Graphic {

  private static final String TAG = "PoseGraphic";

  private static final float DOT_RADIUS = 8.0f;
  private static final float IN_FRAME_LIKELIHOOD_TEXT_SIZE = 30.0f;
  private static final float STROKE_WIDTH = 10.0f;
  private static final float POSE_CLASSIFICATION_TEXT_SIZE = 60.0f;

  private static final String SPINE_TRACKING = "Spine Tracking";
  private static final String HEAD_TRACKING = "Head Tracking";

  private final Pose pose;
  private final boolean showInFrameLikelihood;
  private final boolean visualizeZ;
  private final boolean rescaleZForVisualization;
  private float zMin = Float.MAX_VALUE;
  private float zMax = Float.MIN_VALUE;

  private final List<String> poseClassification;
  private final Paint classificationTextPaint;
  private final Paint leftPaint;
  private final Paint rightPaint;
  private final Paint whitePaint;
  private final Paint greenPaint;
  private final Paint redPaint;


  private String function;

  PoseGraphic(
      GraphicOverlay overlay,
      String function,
      Pose pose,
      boolean showInFrameLikelihood,
      boolean visualizeZ,
      boolean rescaleZForVisualization,
      List<String> poseClassification) {
    super(overlay);
    this.pose = pose;
    this.function = function;
    this.showInFrameLikelihood = showInFrameLikelihood;
    this.visualizeZ = visualizeZ;
    this.rescaleZForVisualization = rescaleZForVisualization;

    this.poseClassification = poseClassification;
    classificationTextPaint = new Paint();
    classificationTextPaint.setColor(Color.WHITE);
    classificationTextPaint.setTextSize(POSE_CLASSIFICATION_TEXT_SIZE);
    classificationTextPaint.setShadowLayer(5.0f, 0f, 0f, Color.BLACK);

    whitePaint = new Paint();
    whitePaint.setStrokeWidth(STROKE_WIDTH);
    whitePaint.setColor(Color.WHITE);
    whitePaint.setTextSize(IN_FRAME_LIKELIHOOD_TEXT_SIZE);
    leftPaint = new Paint();
    leftPaint.setStrokeWidth(STROKE_WIDTH);
    leftPaint.setColor(Color.GREEN);
    rightPaint = new Paint();
    rightPaint.setStrokeWidth(STROKE_WIDTH);
    rightPaint.setColor(Color.YELLOW);
    redPaint = new Paint();
    redPaint.setStrokeWidth(STROKE_WIDTH);
    redPaint.setColor(Color.RED);
    greenPaint = new Paint();
    greenPaint.setStrokeWidth(STROKE_WIDTH);
    greenPaint.setColor(Color.GREEN);

  }

  @Override
  public void draw(Canvas canvas) {
    List<PoseLandmark> landmarks = pose.getAllPoseLandmarks();
    if (landmarks.isEmpty()) {
      return;
    }

    // Draw pose classification text.
    float classificationX = POSE_CLASSIFICATION_TEXT_SIZE * 0.5f;
    for (int i = 0; i < poseClassification.size(); i++) {
      float classificationY = (canvas.getHeight() - POSE_CLASSIFICATION_TEXT_SIZE * 1.5f
          * (poseClassification.size() - i));
      canvas.drawText(
          poseClassification.get(i),
          classificationX,
          classificationY,
          classificationTextPaint);
    }

    // Draw all the points
//    for (PoseLandmark landmark : landmarks) {
//      drawPoint(canvas, landmark, whitePaint);
//      if (visualizeZ && rescaleZForVisualization) {
//        zMin = min(zMin, landmark.getPosition3D().getZ());
//        zMax = max(zMax, landmark.getPosition3D().getZ());
//      }
//    }

    PoseLandmark nose = pose.getPoseLandmark(PoseLandmark.NOSE);
    PoseLandmark leftEyeInner = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_INNER);
    PoseLandmark leftEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE);
    PoseLandmark leftEyeOuter = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_OUTER);
    PoseLandmark rightEyeInner = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_INNER);
    PoseLandmark rightEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE);
    PoseLandmark rightEyeOuter = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_OUTER);
    PoseLandmark leftEar = pose.getPoseLandmark(PoseLandmark.LEFT_EAR);
    PoseLandmark rightEar = pose.getPoseLandmark(PoseLandmark.RIGHT_EAR);
    PoseLandmark leftMouth = pose.getPoseLandmark(PoseLandmark.LEFT_MOUTH);
    PoseLandmark rightMouth = pose.getPoseLandmark(PoseLandmark.RIGHT_MOUTH);

    PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
    PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
    PoseLandmark leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
    PoseLandmark rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
    PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
    PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
    PoseLandmark leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
    PoseLandmark rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);
    PoseLandmark leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE);
    PoseLandmark rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE);
    PoseLandmark leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE);
    PoseLandmark rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE);

    PoseLandmark leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY);
    PoseLandmark rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY);
    PoseLandmark leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX);
    PoseLandmark rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX);
    PoseLandmark leftThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB);
    PoseLandmark rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB);
    PoseLandmark leftHeel = pose.getPoseLandmark(PoseLandmark.LEFT_HEEL);
    PoseLandmark rightHeel = pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL);
    PoseLandmark leftFootIndex = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX);
    PoseLandmark rightFootIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX);


    // Face
//    drawLine(canvas, nos\\\e, lefyEyeInner, whitePaint);
//    drawLine(canvas, lefyEyeInner, lefyEye, whitePaint);
//    drawLine(canvas, lefyEye, leftEyeOuter, whitePaint);
//    drawLine(canvas, leftEyeOuter, leftEar, whitePaint);
//    drawLine(canvas, nose, rightEyeInner, whitePaint);
//    drawLine(canvas, rightEyeInner, rightEye, whitePaint);
//    drawLine(canvas, rightEye, rightEyeOuter, whitePaint);
//    drawLine(canvas, rightEyeOuter, rightEar, whitePaint);
//    drawLine(canvas, leftMouth, rightMouth, whitePaint);
//
//    drawLine(canvas, leftShoulder, rightShoulder, whitePaint);
//    drawLine(canvas, leftHip, rightHip, whitePaint);

    // Left body
//    drawLine(canvas, leftShoulder, leftElbow, leftPaint);
//    drawLine(canvas, leftElbow, leftWrist, leftPaint);
//    drawLine(canvas, leftShoulder, leftHip, leftPaint);
//    drawLine(canvas, leftHip, leftKnee, leftPaint);
//    drawLine(canvas, leftKnee, leftAnkle, leftPaint);
//    drawLine(canvas, leftWrist, leftThumb, leftPaint);
//    drawLine(canvas, leftWrist, leftPinky, leftPaint);
//    drawLine(canvas, leftWrist, leftIndex, leftPaint);
//    drawLine(canvas, leftIndex, leftPinky, leftPaint);
//    drawLine(canvas, leftAnkle, leftHeel, leftPaint);
//    drawLine(canvas, leftHeel, leftFootIndex, leftPaint);

    // Right body
//    drawLine(canvas, rightShoulder, rightElbow, rightPaint);
//    drawLine(canvas, rightElbow, rightWrist, rightPaint);
//    drawLine(canvas, rightShoulder, rightHip, rightPaint);
//    drawLine(canvas, rightHip, rightKnee, rightPaint);
//    drawLine(canvas, rightKnee, rightAnkle, rightPaint);
//    drawLine(canvas, rightWrist, rightThumb, rightPaint);
//    drawLine(canvas, rightWrist, rightPinky, rightPaint);
//    drawLine(canvas, rightWrist, rightIndex, rightPaint);
//    drawLine(canvas, rightIndex, rightPinky, rightPaint);
//    drawLine(canvas, rightAnkle, rightHeel, rightPaint);
//    drawLine(canvas, rightHeel, rightFootIndex, rightPaint);

    switch(function){
      case SPINE_TRACKING:
//        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        drawSpine(canvas, true, leftShoulder, rightShoulder, leftHip, rightHip);
        drawSpine(canvas, false, leftShoulder, rightShoulder, leftHip, rightHip);
        Log.i(TAG,"Spine Tracking Selected");
        break;
      case HEAD_TRACKING:
        Log.i(TAG,"Head Tracking Selected");
        drawHead(canvas, true, leftEye, rightEye, nose);
//        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        break;
      default:
        ;

    }
    // Draw inFrameLikelihood for all points
    if (showInFrameLikelihood) {
      for (PoseLandmark landmark : landmarks) {
        canvas.drawText(
            String.format(Locale.US, "%.2f", landmark.getInFrameLikelihood()),
            translateX(landmark.getPosition().x),
            translateY(landmark.getPosition().y),
            whitePaint);
      }
    }
  }
  private void drawHead(Canvas canvas, boolean isCoach, PoseLandmark leftEye, PoseLandmark rightEye, PoseLandmark nose) {
    PointF point1 = leftEye.getPosition();
    PointF point2 = rightEye.getPosition();
    PointF point3 = nose.getPosition();
    float pointHeadX = (point1.x + point2.x + point3.x) / 3;
    float pointHeadY = (point1.y + point2.y + point3.y) / 3;

    Paint paint;

//    if(isCoach){
//
//
//      paint = rightPaint;
//    }else {
//      float deltaX = topX - bottomX;
//      float deltaY =topY - bottomY;
//      double rad = - (double)Math.atan2(deltaY, deltaX);
//      if( Math.abs(rad - (Math.PI / 3) ) < 0.034) paint = greenPaint;
//      else paint = redPaint;
//      top = from(topX, topY, topZ);
//      bottom = from(bottomX, bottomY, bottomZ);
//    }

    canvas.drawCircle(translateX(pointHeadX), translateY(pointHeadY), 15, rightPaint);
  }

  void drawPoint(Canvas canvas, PoseLandmark landmark, Paint paint) {
    PointF3D point = landmark.getPosition3D();
    maybeUpdatePaintColor(paint, canvas, point.getZ());
    canvas.drawCircle(translateX(point.getX()), translateY(point.getY()), 12, paint);
  }

  void drawLine(Canvas canvas, PoseLandmark startLandmark, PoseLandmark endLandmark, Paint paint) {
    PointF3D start = startLandmark.getPosition3D();
    PointF3D end = endLandmark.getPosition3D();

    // Gets average z for the current body line
    float avgZInImagePixel = (start.getZ() + end.getZ()) / 2;
    maybeUpdatePaintColor(paint, canvas, avgZInImagePixel);

    canvas.drawLine(
          translateX(start.getX()),
          translateY(start.getY()),
          translateX(end.getX()),
          translateY(end.getY()),
          paint);
  }

  void drawSpine(Canvas canvas, boolean isCoach, PoseLandmark leftShoulder, PoseLandmark rightShoulder, PoseLandmark leftHip, PoseLandmark rightHip){
    float topX, topY, topZ, bottomX, bottomY, bottomZ;
    topX = (leftShoulder.getPosition3D().getX() + rightShoulder.getPosition3D().getX()) / 2;
    topY = (leftShoulder.getPosition3D().getY() + rightShoulder.getPosition3D().getY()) / 2;
    topZ = (leftShoulder.getPosition3D().getZ() + rightShoulder.getPosition3D().getZ()) / 2;

    bottomX = (leftHip.getPosition3D().getX() + rightHip.getPosition3D().getX()) / 2;
    bottomY = (leftHip.getPosition3D().getY() + rightHip.getPosition3D().getY()) / 2;
    bottomZ = (leftHip.getPosition3D().getZ() + rightHip.getPosition3D().getZ()) / 2;

    PointF3D top;
    PointF3D bottom;

    PointF top2D;
    PointF bottom2D;

    Paint paint;

    if(isCoach){
      float r = -(float) Math.sqrt( Math.pow((topX - bottomX),2) + Math.pow((topY - bottomY),2) );
      float new_topX = bottomX - (float) (r * Math.cos(-Math.PI / 3));
      float new_topY = bottomY - (float) (r * Math.sin(-Math.PI / 3));

      top = from(new_topX, new_topY, topZ);
      bottom = from(bottomX, bottomY, bottomZ);

      paint = rightPaint;
    }else {
      float deltaX = topX - bottomX;
      float deltaY =topY - bottomY;
      double rad = - (double)Math.atan2(deltaY, deltaX);
      if( Math.abs(rad - (Math.PI / 3) ) < 0.034) paint = greenPaint;
      else paint = redPaint;
      top = from(topX, topY, topZ);
      bottom = from(bottomX, bottomY, bottomZ);
    }

    // Gets average z for the current body line
    float avgZInImagePixel = (top.getZ() + bottom.getZ()) / 2;
    maybeUpdatePaintColor(paint, canvas, avgZInImagePixel);

    canvas.drawLine(
            translateX(top.getX()),
            translateY(top.getY()),
            translateX(bottom.getX()),
            translateY(bottom.getY()),
            paint);

  }

  private void maybeUpdatePaintColor(Paint paint, Canvas canvas, float zInImagePixel) {
    if (!visualizeZ) {
      return;
    }

    // When visualizeZ is true, sets up the paint to different colors based on z values.
    // Gets the range of z value.
    float zLowerBoundInScreenPixel;
    float zUpperBoundInScreenPixel;

    if (rescaleZForVisualization) {
      zLowerBoundInScreenPixel = min(-0.001f, scale(zMin));
      zUpperBoundInScreenPixel = max(0.001f, scale(zMax));
    } else {
      // By default, assume the range of z value in screen pixel is [-canvasWidth, canvasWidth].
      float defaultRangeFactor = 1f;
      zLowerBoundInScreenPixel = -defaultRangeFactor * canvas.getWidth();
      zUpperBoundInScreenPixel = defaultRangeFactor * canvas.getWidth();
    }

    float zInScreenPixel = scale(zInImagePixel);

    if (zInScreenPixel < 0) {
      // Sets up the paint to draw the body line in red if it is in front of the z origin.
      // Maps values within [zLowerBoundInScreenPixel, 0) to [255, 0) and use it to control the
      // color. The larger the value is, the more red it will be.
      int v = (int) (zInScreenPixel / zLowerBoundInScreenPixel * 255);
      v = Ints.constrainToRange(v, 0, 255);
      paint.setARGB(255, 255, 255 - v, 255 - v);
    } else {
      // Sets up the paint to draw the body line in blue if it is behind the z origin.
      // Maps values within [0, zUpperBoundInScreenPixel] to [0, 255] and use it to control the
      // color. The larger the value is, the more blue it will be.
      int v = (int) (zInScreenPixel / zUpperBoundInScreenPixel * 255);
      v = Ints.constrainToRange(v, 0, 255);
      paint.setARGB(255, 255 - v, 255 - v, 255);
    }
  }
}
