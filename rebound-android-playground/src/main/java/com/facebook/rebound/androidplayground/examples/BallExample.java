package com.facebook.rebound.androidplayground.examples;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import com.facebook.rebound.BaseSpringSystem;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringListener;
import com.facebook.rebound.SpringSystem;
import com.facebook.rebound.SpringSystemListener;

import java.util.ArrayList;
import java.util.List;

public class BallExample extends FrameLayout implements SpringListener, SpringSystemListener {

  private final Spring xSpring;
  private final Spring ySpring;
  private final SpringSystem springSystem;
  private final SpringConfig COASTING;
  private float x;
  private float y;
  private Paint ballPaint;
  private boolean dragging;
  private float radius = 100;
  private float downX;
  private float downY;
  private float lastX;
  private float lastY;
  private VelocityTracker velocityTracker;
  private float centerX;
  private float centerY;
  private float attractionThreshold = 300;
  private SpringConfig CONVERGING = SpringConfig.fromOrigamiTensionAndFriction(20, 3);
  private List<PointF> points = new ArrayList<PointF>();

  public BallExample(Context context) {
    this(context, null);
  }

  public BallExample(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public BallExample(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    COASTING = SpringConfig.fromOrigamiTensionAndFriction(0, 0.5);
    COASTING.tension = 0;
    setBackgroundColor(Color.WHITE);

    springSystem = SpringSystem.create();
    springSystem.addListener(this);
    xSpring = springSystem.createSpring();
    ySpring = springSystem.createSpring();
    xSpring.addListener(this);
    ySpring.addListener(this);
    ballPaint = new Paint();
    ballPaint.setStyle(Paint.Style.FILL);
    ballPaint.setColor(Color.RED);

    getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        centerX = getWidth() / 2f;
        centerY = getHeight() / 2f;

        xSpring.setCurrentValue(centerX).setAtRest();
        ySpring.setCurrentValue(centerY).setAtRest();
        getViewTreeObserver().removeOnGlobalLayoutListener(this);

        for (float i = radius; i < getHeight() - radius; i += 400) {
          for (float j = radius; j < getHeight() - radius; j += 400) {
            points.add(new PointF(i, j));
          }
        }
      }
    });
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    ballPaint.setColor(Color.RED);
    ballPaint.setStyle(Paint.Style.FILL);
    canvas.drawCircle(x, y, radius, ballPaint);
    ballPaint.setColor(Color.BLACK);
    ballPaint.setStyle(Paint.Style.STROKE);
    for (PointF point : points) {
      canvas.drawLine(point.x - 10, point.y - 10, point.x + 10, point.y + 10, ballPaint);
      canvas.drawLine(point.x + 10, point.y - 10, point.x - 10, point.y + 10, ballPaint);
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    float touchX = event.getRawX();
    float touchY = event.getRawY();
    boolean ret = false;

    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        downX = touchX;
        downY = touchY;
        lastX = downX;
        lastY = downY;
        velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(event);
        if (downX > x - radius && downX < x + radius && downY > y - radius && downY < y + radius) {
          dragging = true;
          ret = true;
        }
        break;
      case MotionEvent.ACTION_MOVE:
        if (!dragging) {
          break;
        }
        velocityTracker.addMovement(event);
        float offsetX = lastX - touchX;
        float offsetY = lastY - touchY;
        xSpring.setCurrentValue(xSpring.getCurrentValue() - offsetX).setAtRest();
        ySpring.setCurrentValue(ySpring.getCurrentValue() - offsetY).setAtRest();
        checkConstraints();
        ret = true;
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        if (!dragging) {
          break;
        }
        velocityTracker.addMovement(event);
        velocityTracker.computeCurrentVelocity(1000);
        dragging = false;
        ySpring.setSpringConfig(COASTING);
        xSpring.setSpringConfig(COASTING);
        downX = 0;
        downY = 0;
        xSpring.setVelocity(velocityTracker.getXVelocity());
        ySpring.setVelocity(velocityTracker.getYVelocity());
        ret = true;
    }

    lastX = touchX;
    lastY = touchY;
    return ret;
  }

  @Override
  public void onSpringUpdate(Spring spring) {
    x = (float) xSpring.getCurrentValue();
    y = (float) ySpring.getCurrentValue();
    Log.d("WSB", "vel:" + spring.getVelocity());
    invalidate();
  }

  @Override
  public void onSpringAtRest(Spring spring) {

  }

  @Override
  public void onSpringActivate(Spring spring) {

  }

  @Override
  public void onSpringEndStateChange(Spring spring) {

  }

  @Override
  public void onBeforeIntegrate(BaseSpringSystem springSystem) {
  }

  @Override
  public void onAfterIntegrate(BaseSpringSystem springSystem) {
    checkConstraints();
  }

  private void checkConstraints() {
    if (x + radius >= getWidth()) {
      xSpring.setVelocity(-xSpring.getVelocity());
      xSpring.setCurrentValue(xSpring.getCurrentValue() - (x + radius - getWidth()));
    }
    if (x - radius <= 0) {
      xSpring.setVelocity(-xSpring.getVelocity());
      xSpring.setCurrentValue(xSpring.getCurrentValue() - (x - radius));
    }
    if (y + radius >= getHeight()) {
      ySpring.setVelocity(-ySpring.getVelocity());
      ySpring.setCurrentValue(ySpring.getCurrentValue() - (y + radius - getHeight()));
    }
    if (y - radius <= 0) {
      ySpring.setVelocity(-ySpring.getVelocity());
      ySpring.setCurrentValue(ySpring.getCurrentValue() - (y - radius));
    }

    for (PointF point : points) {
      if (dist(x, y, point.x, point.y) < attractionThreshold &&
          Math.abs(xSpring.getVelocity()) < 500 &&
          Math.abs(ySpring.getVelocity()) < 500 &&
          !dragging) {
        xSpring.setSpringConfig(CONVERGING);
        xSpring.setEndValue(point.x);
        ySpring.setSpringConfig(CONVERGING);
        ySpring.setEndValue(point.y);
      }
    }
  }

  private float dist(double posX, double posY, double pos2X, double pos2Y) {
    return (float) Math.sqrt(Math.pow(pos2X - posX, 2) + Math.pow(pos2Y - posY, 2));
  }
}
