package com.asld.asld.vnc;

import android.graphics.Matrix;
import android.graphics.Point;
import android.util.Log;
import android.view.GestureDetector;
import android.view.InputDevice;
import android.view.MotionEvent;

/**
 * Handles pointer input from:
 * * touchscreen
 * * stylus
 * * physical mouse
 * and uses this for:
 * * scaling
 * * two-finger fling
 * * pointer movement
 * * click
 * * drag
 * detection and handover of events to VncActivity and VncCanvas.
 */
@SuppressWarnings("FieldCanBeLocal")
public class InputHandler extends GestureDetector.SimpleOnGestureListener {

    private static final String TAG = "PointerInputHandler";

    private final VncActivity vncActivity;
    protected GestureDetector gestures;
    private int pointerMask;
    private Matrix pointerTransMatrix;
    private float absX = 0;
    private float absY = 0;
    private boolean hasDown;
    private boolean twoDown;
    private float touchX;
    private float touchY;
    private float downX;
    private float downY;
    private boolean initPointer = false;
    private float lastAxis = 0;
    private int diffCount = 0;

    InputHandler(VncActivity vncActivity) {
        this.vncActivity = vncActivity;
        gestures = new GestureDetector(vncActivity, this, null, false); // this is a SDK 8+ feature and apparently needed if targetsdk is set
        gestures.setOnDoubleTapListener(this);

        Log.d(TAG, "MightyInputHandler " + this + " created!");
    }

    public void setPointerScale(float x, float y) {
        pointerTransMatrix = new Matrix();
        pointerTransMatrix.setScale(x, y);
    }

    public void init() {
        Log.d(TAG, "MightyInputHandler " + this + " init!");
    }

    public void shutdown() {

        Log.d(TAG, "MightyInputHandler " + this + " shutdown!");
    }


    protected boolean isTouchEvent(MotionEvent event) {
        return event.getSource() == InputDevice.SOURCE_TOUCHSCREEN ||
                event.getSource() == InputDevice.SOURCE_TOUCHPAD;
    }

    private boolean handleTouchEvent(MotionEvent event) {
        if (!initPointer) {
            absX = (float) vncActivity.touchPad.getWidth() / 2;
            absY = (float) vncActivity.touchPad.getHeight() / 2;
            initPointer = true;
        }
        Log.d(TAG, "handleTouchEvent: " + event + " actionType: " + event.getAction());
        Log.d(TAG, "handleTouchEvent: now state: absX: " + absX + " absY: " + absY + " hasDown: " + hasDown + " twoDown: " + twoDown);
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            hasDown = true;
            downX = event.getX();
            downY = event.getY();
            touchX = downX;
            touchY = downY;
            return true;
        }
        if ((action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN) {
            twoDown = true;
            return true;
        }
        if (action == MotionEvent.ACTION_UP || (action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP) {
            if (!hasDown) {
                return true;
            }
            if (event.getPointerId(0) != 0)
                return true;
            Log.d(TAG, "handleTouchEvent: aaa");
            lastAxis = 0;
            diffCount = 0;
            hasDown = false;
            if (event.getEventTime() - event.getDownTime() < 200) {
                if (Math.abs(event.getX(0) - downX) + Math.abs(event.getY(0) - downY) > 30) {
                    twoDown = false;
                    return true;
                }
                event.setLocation(absX, absY);
                event.setAction(MotionEvent.ACTION_DOWN);
                boolean right = twoDown;
                Log.d(TAG, "handleTouchEvent: right: " + right);
                processPointerEvent(event, true, right ? VNCConn.MOUSE_BUTTON_RIGHT : VNCConn.MOUSE_BUTTON_LEFT);
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    event.setLocation(absX, absY);
                    event.setAction(MotionEvent.ACTION_UP);
                    processPointerEvent(event, false, right ? VNCConn.MOUSE_BUTTON_RIGHT : VNCConn.MOUSE_BUTTON_LEFT);

                }).start();
                event.setAction(MotionEvent.ACTION_UP);
            }
            twoDown = false;
            return true;
        }

        if (hasDown && twoDown && action == MotionEvent.ACTION_MOVE) {
            Log.d(TAG, "handleTouchEvent: bbb");
            if (event.getPointerId(0) != 0)
                return true;
            float relativeY = event.getY(0) - touchY;
            touchX = event.getX();
            touchY = event.getY();
            Log.d(TAG, "handleTouchEvent: relativeY: " + relativeY);
            if (relativeY == 0) {
                return true;
            }
            if (lastAxis == 0) {
                lastAxis = relativeY;
                diffCount = 0;
            }
            float absRY = Math.abs(relativeY);
            if (absRY < 20 && lastAxis * relativeY < 0) {
                diffCount++;
                if (diffCount < 5)
                    return true;
                diffCount = 0;
                lastAxis = relativeY;
            }
            if (lastAxis * relativeY < 0) {
                lastAxis = relativeY;
                diffCount = 0;
            }
            if (relativeY > 0) {
                Log.d(TAG, "Input: scroll up, " + relativeY + " mx: " + vncActivity.vncCanvas.mouseX + " my: "
                        + vncActivity.vncCanvas.mouseY + " aX: " + absX + " aY: " + absY);
                vncActivity.vncConn.sendPointerEvent(vncActivity.vncCanvas.mouseX, vncActivity.vncCanvas.mouseY, 0, 0);
                vncActivity.vncConn.sendPointerEvent(vncActivity.vncCanvas.mouseX, vncActivity.vncCanvas.mouseY, 0, VNCConn.MOUSE_BUTTON_SCROLL_UP);
            } else {
                Log.d(TAG, "Input: scroll down, " + relativeY + " mx: " + vncActivity.vncCanvas.mouseX + " my: "
                        + vncActivity.vncCanvas.mouseY + " aX: " + absX + " aY: " + absY);
                vncActivity.vncConn.sendPointerEvent(vncActivity.vncCanvas.mouseX, vncActivity.vncCanvas.mouseY, 0, 0);
                vncActivity.vncConn.sendPointerEvent(vncActivity.vncCanvas.mouseX, vncActivity.vncCanvas.mouseY, 0, VNCConn.MOUSE_BUTTON_SCROLL_DOWN);
            }

        } else if (hasDown && action == MotionEvent.ACTION_MOVE) {
            Log.d(TAG, "handleTouchEvent: ccc");
            if (event.getPointerId(0) != 0)
                return true;
            translateTouch(event.getX(0) - touchX, event.getY(0) - touchY);
            touchX = event.getX();
            touchY = event.getY();
            event.setLocation(absX, absY);
            processPointerEvent(event, false, VNCConn.MOUSE_BUTTON_LEFT);
            return true;
        }
        return false;
    }

    private void translateTouch(float x, float y) {
        float rate = 1.5f;
        absX += x * rate;
        absY += y * rate;
        float w = vncActivity.touchPad.getWidth();
        float h = vncActivity.touchPad.getHeight();
        if (absX > w)
            absX = w;
        if (absX < 0)
            absX = 0;
        if (absY > h)
            absY = h;
        if (absY < 0)
            absY = 0;
    }

    /**
     * 外接设备：鼠标
     *
     * @param e
     * @return
     */
    public boolean onGenericMotionEvent(MotionEvent e) {
        int action = MotionEvent.ACTION_MASK;
        boolean button = false;
        boolean secondary = false;

        if (isTouchEvent(e)) {
            return handleTouchEvent(e);
        }

        // changeTouchCoordinatesToFullFrame
        Log.d(TAG, "Input: touch normal: x:" + e.getX() + " y:" + e.getY() + " action:" + e.getAction() + " event:" + e);
        Log.d(TAG, "onGenericMotionEvent: " + pointerTransMatrix);

        if (e.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER) {
            e.setAction(MotionEvent.ACTION_UP);
        }
        //Translate the event into onTouchEvent type language
        if (e.getButtonState() != 0) {
            if ((e.getButtonState() & MotionEvent.BUTTON_PRIMARY) != 0) {
                button = true;
                secondary = false;
                action = MotionEvent.ACTION_DOWN;
            } else if ((e.getButtonState() & MotionEvent.BUTTON_SECONDARY) != 0 || ((e.getButtonState() & MotionEvent.BUTTON_BACK) != 0) && e.getDeviceId() != -1) {
                // colorOS，修改了鼠标右键为back
                button = true;
                secondary = true;
                action = MotionEvent.ACTION_DOWN;
            }
            //handle touch back
            if ((e.getButtonState() & MotionEvent.BUTTON_BACK) != 0 && e.getDeviceId() == -1) {
                Log.d(TAG, "onGenericMotionEvent: back press");
                vncActivity.changeAppBarVisibility();
                return true;
            }
            if (e.getAction() == MotionEvent.ACTION_MOVE) {
                action = MotionEvent.ACTION_MOVE;
            }
        } else if ((e.getActionMasked() == MotionEvent.ACTION_HOVER_MOVE) ||
                (e.getActionMasked() == MotionEvent.ACTION_UP)) {
            action = MotionEvent.ACTION_UP;
            button = false;
            secondary = false;
        }

        if (action != MotionEvent.ACTION_MASK) {
            e.setAction(action);
            if (!button) {
                processPointerEvent(e, false, VNCConn.MOUSE_BUTTON_LEFT);
            } else {
                Log.d(TAG, "onGenericMotionEvent: " + secondary);
                processPointerEvent(e, true, secondary ? VNCConn.MOUSE_BUTTON_RIGHT : VNCConn.MOUSE_BUTTON_LEFT);
            }
            absX = e.getX();
            absY = e.getY();
        }

        if (e.getAction() == MotionEvent.ACTION_SCROLL) {
            if (e.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) {
                Log.d(TAG, "Input: scroll down");
                vncActivity.vncConn.sendPointerEvent(vncActivity.vncCanvas.mouseX, vncActivity.vncCanvas.mouseY, e.getMetaState(), VNCConn.MOUSE_BUTTON_SCROLL_DOWN);
            } else {
                Log.d(TAG, "Input: scroll up");
                vncActivity.vncConn.sendPointerEvent(vncActivity.vncCanvas.mouseX, vncActivity.vncCanvas.mouseY, e.getMetaState(), VNCConn.MOUSE_BUTTON_SCROLL_UP);
            }
        }


        Log.d(TAG, "Input: touch normal: x:" + e.getX() + " y:" + e.getY() + " action:" + e.getAction() + " event:" + e);

        return true;
    }

    /**
     * Convert a motion event to a format suitable for sending over the wire
     *
     * @param evt         motion event; x and y must already have been converted from screen coordinates
     *                    to remote frame buffer coordinates.
     * @param mouseIsDown True if "mouse button" (touch or trackball button) is down when this happens
     * @param button      button mask
     * @return true if event was actually sent
     */
    public boolean processPointerEvent(MotionEvent evt, boolean mouseIsDown, int button) {
        // map pointer position from touch pad to display screen
        evt.transform(pointerTransMatrix);
        try {
            int action = evt.getAction();
            pointerMask = button;
            if (action == MotionEvent.ACTION_DOWN || (mouseIsDown && action == MotionEvent.ACTION_MOVE)) {
                if (button == VNCConn.MOUSE_BUTTON_RIGHT) {
                    if (action == MotionEvent.ACTION_MOVE)
                        Log.d(TAG, "Input: moving, right mouse button down");
                    else
                        Log.d(TAG, "Input: right mouse button down");
                } else if (button == VNCConn.MOUSE_BUTTON_LEFT) {
                    if (action == MotionEvent.ACTION_MOVE)
                        Log.d(TAG, "Input: moving, left mouse button down");
                    else
                        Log.d(TAG, "Input: left mouse button down");
                }
            } else if (action == MotionEvent.ACTION_UP) {
                Log.d(TAG, "Input: all mouse buttons up");
                pointerMask = 0;
            }

            return vncActivity.vncConn.sendPointerEvent((int) evt.getX(), (int) evt.getY(), evt.getMetaState(), pointerMask);

        } catch (NullPointerException e) {
            return false;
        }
    }

}
