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


    InputHandler(VncActivity vncActivity) {
        this.vncActivity = vncActivity;
        gestures = new GestureDetector(vncActivity, this, null, false); // this is a SDK 8+ feature and apparently needed if targetsdk is set
        gestures.setOnDoubleTapListener(this);

        Log.d(TAG, "MightyInputHandler " + this + " created!");
    }

    public void setPointerScale(float x, float y){
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
            return false;
        }

        // changeTouchCoordinatesToFullFrame
        Log.d(TAG, "Input: touch normal: x:" + e.getX() + " y:" + e.getY() + " action:" + e.getAction() + " event:" + e);
        Log.d(TAG, "onGenericMotionEvent: "+pointerTransMatrix);
        e.transform(pointerTransMatrix);

        if (e.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER){
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
            if((e.getButtonState() & MotionEvent.BUTTON_BACK)!=0 && e.getDeviceId() == -1){
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
                processPointerEvent(e, false, false);
            } else {
                Log.d(TAG, "onGenericMotionEvent: " + secondary);
                processPointerEvent(e, true, secondary);
            }
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
     * @param evt            motion event; x and y must already have been converted from screen coordinates
     *                       to remote frame buffer coordinates.
     * @param mouseIsDown    True if "mouse button" (touch or trackball button) is down when this happens
     * @param useRightButton If true, event is interpreted as happening with right mouse button
     * @return true if event was actually sent
     */
    public boolean processPointerEvent(MotionEvent evt, boolean mouseIsDown, boolean useRightButton) {
        // map pointer position from touch pad to display screen
        try {
            int action = evt.getAction();
            if (action == MotionEvent.ACTION_DOWN || (mouseIsDown && action == MotionEvent.ACTION_MOVE)) {
                if (useRightButton) {
                    if (action == MotionEvent.ACTION_MOVE)
                        Log.d(TAG, "Input: moving, right mouse button down");
                    else
                        Log.d(TAG, "Input: right mouse button down");

                    pointerMask = VNCConn.MOUSE_BUTTON_RIGHT;
                } else {
                    if (action == MotionEvent.ACTION_MOVE)
                        Log.d(TAG, "Input: moving, left mouse button down");
                    else
                        Log.d(TAG, "Input: left mouse button down");

                    pointerMask = VNCConn.MOUSE_BUTTON_LEFT;
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
