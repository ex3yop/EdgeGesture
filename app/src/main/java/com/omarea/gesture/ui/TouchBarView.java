package com.omarea.gesture.ui;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.omarea.gesture.AccessibilityServiceGesture;
import com.omarea.gesture.ActionModel;
import com.omarea.gesture.Gesture;
import com.omarea.gesture.R;
import com.omarea.gesture.SpfConfig;
import com.omarea.gesture.util.GlobalState;
import com.omarea.gesture.util.Handlers;

public class TouchBarView extends View {
    static final int RIGHT = 2;
    static final int BOTTOM = 0;
    static final int LEFT = 1;
    static final int THREE_SECTION = 3;
    private int barPosition = 0;

    private float touchStartX = 0F; // 触摸开始位置
    private float touchStartRawX = 0F; // 触摸开始位置
    private float touchStartY = 0F; // 触摸开始位置
    private float touchStartRawY = 0F; // 触摸开始位置
    private long gestureStartTime = 0L; // 手势开始时间（是指滑动到一定距离，认定触摸手势生效的时间）
    private boolean isLongTimeGesture = false;
    private Context context = getContext();
    private int FLIP_DISTANCE = dp2px(context, 35f); // 触摸灵敏度（滑动多长距离认为是手势）
    private boolean isTouchDown = false;
    private boolean isGestureCompleted = false;
    private boolean vibratorRun = false;
    private float flingValue = dp2px(context, 3f); // 小于此值认为是点击而非滑动

    private ActionModel eventTouch;
    private ActionModel eventHover;
    private AccessibilityServiceGesture accessibilityService;
    private boolean isLandscapf = false;
    private boolean gameOptimization = false;

    private long lastEventTime = 0L;
    private int lastEvent = -1;

    private float touchRawX;
    private float touchRawY;

    private Path touchPath = new Path();
    private ReTouchHelper reTouchHelper;

    public TouchBarView(Context context) {
        super(context);
        init();
    }

    public TouchBarView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public TouchBarView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
        init();
    }

    private void init() {
    }

    private void performGlobalAction(ActionModel event) {
        if (accessibilityService != null) {
            if (gameOptimization && isLandscapf && ((gestureStartTime - lastEventTime) > 3000 || lastEvent != event.actionCode)) {
                lastEvent = event.actionCode;
                lastEventTime = System.currentTimeMillis();
                Gesture.toast(this.getContext().getString(R.string.please_repeat), Toast.LENGTH_SHORT);
            } else {
                Handlers.executeVirtualAction(accessibilityService, event, touchStartRawX, touchStartRawY);
            }
        }
    }

    private void onShortTouch() {
        if (accessibilityService != null) {
            performGlobalAction(eventTouch);
        }
    }

    private void onTouchHover() {
        if (accessibilityService != null) {
            performGlobalAction(eventHover);
        }
    }

    void setBarPosition(int barPosition, boolean isLandscapf, boolean gameOptimization, int width, int height) {
        this.barPosition = barPosition;
        this.isLandscapf = isLandscapf;
        this.gameOptimization = gameOptimization;

        setSize(width, height);
    }

    private void setSize(int width, int height) {
        ViewGroup.LayoutParams lp = this.getLayoutParams();
        int h = height;
        int w = width;
        if (h < 1) {
            h = 1;
        }
        if (w < 1) {
            w = 1;
        }
        lp.width = w;
        lp.height = h;
        this.setLayoutParams(lp);
    }

    void setEventHandler(ActionModel shortTouch, ActionModel touchHover, final AccessibilityServiceGesture context) {
        this.eventTouch = shortTouch;
        this.eventHover = touchHover;
        this.accessibilityService = context;
    }

    void setReTouchHelper(ReTouchHelper reTouchHelper) {
        this.reTouchHelper = reTouchHelper;
    }

    private void buildGesture() {
        if (reTouchHelper != null) {
            reTouchHelper.dispatchGesture(touchPath);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event != null) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    touchPath.reset();
                    touchPath.moveTo(event.getRawX(), event.getRawY());
                    return onTouchDown(event);
                }
                case MotionEvent.ACTION_MOVE: {
                    touchPath.lineTo(event.getRawX(), event.getRawY());
                    return onTouchMove(event);
                }
                case MotionEvent.ACTION_UP: {
                    return onTouchUp(event);
                }
                case MotionEvent.ACTION_CANCEL:
                    cleartEffect();
                    return true;
                case MotionEvent.ACTION_OUTSIDE: {
                    cleartEffect();
                    return false;
                }
                default: {
                }
            }
        } else {
            cleartEffect();
        }
        return true;
    }

    private boolean onTouchDown(MotionEvent event) {
        isTouchDown = true;
        isGestureCompleted = false;
        touchStartX = event.getX();
        touchStartRawX = event.getRawX();
        touchStartY = event.getY();
        touchStartRawY = event.getRawY();
        GlobalState.startEdgeFeedback(event.getRawX(), event.getRawY(), barPosition);
        gestureStartTime = 0;
        isLongTimeGesture = false;
        vibratorRun = true;

        return true;
    }

    private boolean onTouchMove(MotionEvent event) {
        if (isGestureCompleted || !isTouchDown) {
            return true;
        }

        touchRawX = event.getRawX();
        touchRawY = event.getRawY();

        // 当前触摸位置
        float touchCurrentX = event.getX();
        // 当前触摸位置
        float touchCurrentY = event.getY();
        float a = -1f;
        float b = -1f;
        if (barPosition == LEFT) {
            a = touchCurrentX;
            b = touchStartX;
        } else if (barPosition == RIGHT) {
            a = touchStartX;
            b = touchCurrentX;
        } else if (barPosition == BOTTOM) {
            a = touchStartY;
            b = touchCurrentY;
        }

        if (a - b > FLIP_DISTANCE) {
            if (gestureStartTime < 1) {
                GlobalState.updateEdgeFeedbackIcon(TouchIconCache.getIcon(eventTouch.actionCode), false);

                final long currentTime = System.currentTimeMillis();
                gestureStartTime = currentTime;
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isTouchDown && !isGestureCompleted && currentTime == gestureStartTime) {
                            isLongTimeGesture = true;
                            if (vibratorRun) {
                                Gesture.vibrate(Gesture.VibrateMode.VIBRATE_SLIDE_HOVER, getRootView());

                                vibratorRun = false;
                            }
                            GlobalState.updateEdgeFeedbackIcon(TouchIconCache.getIcon(eventHover.actionCode), true);
                            if (barPosition == BOTTOM) {
                                onTouchHover();
                                isGestureCompleted = true;
                                cleartEffect();
                            } else {
                                GlobalState.updateEdgeFeedback(touchRawX, touchRawY);
                            }
                        }
                    }
                }, Gesture.config.getInt(SpfConfig.CONFIG_HOVER_TIME, SpfConfig.CONFIG_HOVER_TIME_DEFAULT));
                Gesture.vibrate(Gesture.VibrateMode.VIBRATE_SLIDE, getRootView());
            }
        } else {
            GlobalState.updateEdgeFeedbackIcon(null, false);
            vibratorRun = true;
            gestureStartTime = 0;
        }
        GlobalState.updateEdgeFeedback(touchRawX, touchRawY);
        return true;
    }

    private boolean onTouchUp(MotionEvent event) {
        if (!isTouchDown || isGestureCompleted) {
            return true;
        }

        isTouchDown = false;
        isGestureCompleted = true;

        float moveX = event.getX() - touchStartX;
        float moveY = touchStartY - event.getY();

        if (barPosition == BOTTOM && Math.abs(moveY) > flingValue) { //  > flingValue
            if (moveY > FLIP_DISTANCE) {
                if (isLongTimeGesture)
                    onTouchHover();
                else
                    onShortTouch();
            }
        } else if ((barPosition == LEFT || barPosition == RIGHT) && Math.abs(moveX) > flingValue) {
            if (barPosition == LEFT) {
                if (moveX > FLIP_DISTANCE) {
                    // 向屏幕内侧滑动 - 停顿250ms 打开最近任务，不停顿则“返回”
                    if (isLongTimeGesture)
                        onTouchHover();
                    else
                        onShortTouch();
                }
            } else if (barPosition == RIGHT) {
                if (-moveX > FLIP_DISTANCE) {
                    // 向屏幕内侧滑动 - 停顿250ms 打开最近任务，不停顿则“返回”
                    if (isLongTimeGesture)
                        onTouchHover();
                    else
                        onShortTouch();
                }
            }
        } else {
            buildGesture();
        }
        cleartEffect();

        return true;
    }

    /**
     * dp转换成px
     */
    private int dp2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 清除手势效果
     */
    private void cleartEffect() {
        GlobalState.clearEdgeFeedback();
        isTouchDown = false;
    }

    @Override
    @SuppressLint("DrawAllocation")
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}
