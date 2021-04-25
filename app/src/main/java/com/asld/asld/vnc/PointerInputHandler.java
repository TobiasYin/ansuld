package com.asld.asld.vnc;

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
public class PointerInputHandler extends GestureDetector.SimpleOnGestureListener {

    private static final String TAG = "PointerInputHandler";

    private final VncActivity vncActivity;
    protected GestureDetector gestures;

    PointerInputHandler(VncActivity vncActivity) {
        this.vncActivity = vncActivity;
        gestures = new GestureDetector(vncActivity, this, null, false); // this is a SDK 8+ feature and apparently needed if targetsdk is set
        gestures.setOnDoubleTapListener(this);

       Log.d(TAG, "MightyInputHandler " + this + " created!");
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

        e = vncActivity.vncCanvas.changeTouchCoordinatesToFullFrame(e);

        //Translate the event into onTouchEvent type language
        if (e.getButtonState() != 0) {
            if ((e.getButtonState() & MotionEvent.BUTTON_PRIMARY) != 0) {
                button = true;
                secondary = false;
                action = MotionEvent.ACTION_DOWN;
            } else if ((e.getButtonState() & MotionEvent.BUTTON_SECONDARY) != 0) {
                button = true;
                secondary = true;
                action = MotionEvent.ACTION_DOWN;
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
                vncActivity.vncCanvas.processPointerEvent(e, false);
            } else {
                vncActivity.vncCanvas.processPointerEvent(e, true, secondary);
            }
            vncActivity.vncCanvas.panToMouse();
        }

        if (e.getAction() == MotionEvent.ACTION_SCROLL) {
            if (e.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) {
               Log.d(TAG, "Input: scroll down");
                vncActivity.vncCanvas.vncConn.sendPointerEvent(vncActivity.vncCanvas.mouseX, vncActivity.vncCanvas.mouseY, e.getMetaState(), VNCConn.MOUSE_BUTTON_SCROLL_DOWN);
            } else {
               Log.d(TAG, "Input: scroll up");
                vncActivity.vncCanvas.vncConn.sendPointerEvent(vncActivity.vncCanvas.mouseX, vncActivity.vncCanvas.mouseY, e.getMetaState(), VNCConn.MOUSE_BUTTON_SCROLL_UP);
            }
        }


       Log.d(TAG, "Input: touch normal: x:" + e.getX() + " y:" + e.getY() + " action:" + e.getAction());

        return true;
    }


}