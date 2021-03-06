//
//  Copyright (C) 2011 Christian Beier
//  Copyright (C) 2010 Michael A. MacDonald
//  Copyright (C) 2004 Horizon Wimba.  All Rights Reserved.
//  Copyright (C) 2001-2003 HorizonLive.com, Inc.  All Rights Reserved.
//  Copyright (C) 2001,2002 Constantin Kaplinsky.  All Rights Reserved.
//  Copyright (C) 2000 Tridia Corporation.  All Rights Reserved.
//  Copyright (C) 1999 AT&T Laboratories Cambridge.  All Rights Reserved.
//
//  This is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This software is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this software; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
//  USA.
//

//
// VncCanvas is a subclass of android.view.GLSurfaceView which draws a VNC
// desktop on it.
//

package org.minal.minal.vnc;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

import android.content.Context;
import android.graphics.Matrix;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import org.minal.minal.vnc.VncActivity;


public class VncCanvas extends GLSurfaceView {
    static {
        System.loadLibrary("vnccanvas");
    }

    private final static String TAG = "VncCanvas";


    // Runtime control flags
    private final AtomicBoolean showDesktopInfo = new AtomicBoolean(true);
    private boolean repaintsEnabled = true;

    /**
     * Use camera button as meta key for right mouse button
     */
    boolean cameraButtonDown = false;

    public VncActivity activity;

    // VNC protocol connection
    public VNCConn vncConn;

    public Handler handler = new Handler();

    private VNCGLRenderer glRenderer;

    private Matrix pointerTransMatrix;
    float scale = 1;

    private float getScale() {
        return scale;
    }

    private int pointerMask = VNCConn.MOUSE_BUTTON_NONE;


    // framebuffer coordinates of mouse pointer, Available to activity
    int mouseX, mouseY;

    /**
     * Position of the top left portion of the <i>visible</i> part of the screen, in
     * full-frame coordinates
     */
    int absoluteXPosition = 0, absoluteYPosition = 0;


    //	private static native void on_surface_created();
//    private static native void on_surface_changed(int width, int height);
//    private static native void on_draw_frame();
    private static native void prepareTexture(long rfbClient);


    private class VNCGLRenderer implements Renderer {

        int[] textureIDs = new int[1];   // Array for 1 texture-ID
        private int[] mTexCrop = new int[4];


        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {

            Log.d(TAG, "onSurfaceCreated()");


            // Set color's clear-value to black
            gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

            /*
             * By default, OpenGL enables features that improve quality but reduce
             * performance. One might want to tweak that especially on software
             * renderer.
             */
            gl.glDisable(GL10.GL_DITHER); // Disable dithering for better performance
            gl.glDisable(GL10.GL_LIGHTING);
            gl.glDisable(GL10.GL_DEPTH_TEST);

            /*
             * alpha blending has to be enabled manually!
             */
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

            /*
             * setup texture stuff
             */
            // Enable 2d textures
            gl.glEnable(GL10.GL_TEXTURE_2D);
            // Generate texture-ID array
            gl.glDeleteTextures(1, textureIDs, 0);
            gl.glGenTextures(1, textureIDs, 0);
            // this is a 2D texture
            gl.glBindTexture(GL10.GL_TEXTURE_2D, textureIDs[0]);
            // Set up texture filters --> nice smoothing
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        }

        // Call back after onSurfaceCreated() or whenever the window's size changes
        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {

            Log.d(TAG, "onSurfaceChanged()");

            // Set the viewport (display area) to cover the entire window
            gl.glViewport(0, 0, width, height);

            // Setup orthographic projection
            gl.glMatrixMode(GL10.GL_PROJECTION); // Select projection matrix
            gl.glLoadIdentity();                 // Reset projection matrix
            gl.glOrthox(0, width, height, 0, 0, 100);


            gl.glMatrixMode(GL10.GL_MODELVIEW);  // Select model-view matrix
            gl.glLoadIdentity();                 // Reset
        }

        @Override
        public void onDrawFrame(GL10 gl) {

            // TODO optimize: texSUBimage ?
            // pbuffer: http://blog.shayanjaved.com/2011/05/13/android-opengl-es-2-0-render-to-texture/

            try {
                gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

                if (vncConn.getFramebufferWidth() > 0 && vncConn.getFramebufferHeight() > 0) {
                    vncConn.lockFramebuffer();
                    prepareTexture(vncConn.rfbClient);
                    vncConn.unlockFramebuffer();
                }

                /*
                 * The crop rectangle is given as Ucr, Vcr, Wcr, Hcr.
                 * That is, "left"/"bottom"/width/height, although you can
                 * also have negative width and height to flip the image.
                 *
                 * This is the part of the framebuffer we show on-screen.
                 *
                 * If absolute[XY]Position are negative that means the framebuffer
                 * is smaller than our viewer window.
                 *
                 */
//				mTexCrop[0] = Math.max(absoluteXPosition, 0); // don't let this be <0
                mTexCrop[0] = 0;// don't let this be <0
                mTexCrop[1] = vncConn.getFramebufferHeight();
//                mTexCrop[2] = (int) (VncCanvas.this.getWidth() < vncConn.getFramebufferWidth() * getScale() ? VncCanvas.this.getWidth() / getScale() : vncConn.getFramebufferWidth());
//                mTexCrop[3] = (int) -(VncCanvas.this.getHeight() < vncConn.getFramebufferHeight() * getScale() ? VncCanvas.this.getHeight() / getScale() : vncConn.getFramebufferHeight());
                mTexCrop[2] = vncConn.getFramebufferWidth();
                mTexCrop[3] = -vncConn.getFramebufferHeight();

                Log.d(TAG, "cropRect: u:" + mTexCrop[0] + " v:" + mTexCrop[1] + " w:" + mTexCrop[2] + " h:" + mTexCrop[3] + "scale:" + getScale() + "VncHeight:" + VncCanvas.this.getHeight());

                ((GL11) gl).glTexParameteriv(GL10.GL_TEXTURE_2D, GL11Ext.GL_TEXTURE_CROP_RECT_OES, mTexCrop, 0);

                /*
                 * Very fast, but very basic transforming: only transpose, flip and scale.
                 * Uses the GL_OES_draw_texture extension to draw sprites on the screen without
                 * any sort of projection or vertex buffers involved.
                 *
                 * See http://www.khronos.org/registry/gles/extensions/OES/OES_draw_texture.txt
                 *
                 * All parameters in GL screen coordinates!
                 */
                int x = (int) (VncCanvas.this.getWidth() < vncConn.getFramebufferWidth() * getScale() ? 0 : VncCanvas.this.getWidth() / 2 - (vncConn.getFramebufferWidth() * getScale()) / 2);
                int y = (int) (VncCanvas.this.getHeight() < vncConn.getFramebufferHeight() * getScale() ? 0 : VncCanvas.this.getHeight() / 2 - (vncConn.getFramebufferHeight() * getScale()) / 2);
                int w = (int) (VncCanvas.this.getWidth() < vncConn.getFramebufferWidth() * getScale() ? VncCanvas.this.getWidth() : vncConn.getFramebufferWidth() * getScale());
                int h = (int) (VncCanvas.this.getHeight() < vncConn.getFramebufferHeight() * getScale() ? VncCanvas.this.getHeight() : vncConn.getFramebufferHeight() * getScale());
                gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f); // opaque!
                ((GL11Ext) gl).glDrawTexfOES(x, y, 0, w, h);

                Log.d(TAG, "drawing to screen: x " + x + " y " + y + " w " + w + " h " + h);


            } catch (NullPointerException e) {
            }

        }

    }


    /**
     * Constructor used by the inflation apparatus
     *
     * @param context
     */
    public VncCanvas(final Context context, AttributeSet attrs) {
        super(context, attrs);

        setFocusable(true);

        glRenderer = new VNCGLRenderer();
        setRenderer(glRenderer);
        // only render upon request
        setRenderMode(RENDERMODE_WHEN_DIRTY);

        int oldprio = android.os.Process.getThreadPriority(android.os.Process.myTid());
        // GIVE US MAGIC POWER, O GREAT FAIR SCHEDULER!
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
        Log.d(TAG, "Changed prio from " + oldprio + " to " + android.os.Process.getThreadPriority(android.os.Process.myTid()));
    }

    /**
     * Create a view showing a VNC connection
     */
    void initializeVncCanvas(VncActivity a, VNCConn conn, Float scale) {
        activity = a;
        vncConn = conn;
        this.scale = scale;
    }

    /**
     * Apply scroll offset and scaling to convert touch-space coordinates to the corresponding
     * point on the full frame.
     *
     * @param e MotionEvent with the original, touch space coordinates.  This event is altered in place.
     * @return e -- The same event passed in, with the coordinates mapped
     */
    MotionEvent changeTouchCoordinatesToFullFrame(MotionEvent e) {
        // Adjust coordinates for Android notification bar.
        e.offsetLocation(0, -1f * getTop());

//		e.setLocation(absoluteXPosition + e.getX() / scale, absoluteYPosition + e.getY() / scale);
        if (pointerTransMatrix == null) {
            pointerTransMatrix = new Matrix();
            pointerTransMatrix.setScale((float) getWidth() / activity.touchPad.getWidth(), (float) getHeight() / activity.touchPad.getHeight());
        }
        e.transform(pointerTransMatrix);
        return e;
    }


    @Override
    public void onPause() {
        /*
         * this is to avoid a deadlock between GUI thread and GLThread:
         *
         * the GUI thread would call onPause on the GLThread which would never return since
         * the GL thread's GLThreadManager object is waiting on the GLThread.
         */
        try {
            vncConn.unlockFramebuffer();
        } catch (IllegalMonitorStateException e) {
            // thrown when mutex was not locked
        } catch (NullPointerException e) {
            // thrown if we fatal out at the very beginning
        }

        super.onPause();
    }


    void reDraw() {
        Log.d(TAG, "call canvas redraw");
        Log.d(TAG, String.format("reDraw: %d, %d", (repaintsEnabled ? 1 : 0), vncConn.rfbClient));

        if (repaintsEnabled && vncConn.rfbClient != 0) {
            // request a redraw from GL thread
            requestRender();

            // Show a Toast with the desktop info on first frame draw.
            if (showDesktopInfo.get()) {
                showDesktopInfo.set(false);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        showConnectionInfo();
                    }
                });
            }

        }
    }

    public void disableRepaints() {
        repaintsEnabled = false;
    }

    public void enableRepaints() {
        repaintsEnabled = true;
    }

    public void showConnectionInfo() {
        String msg = "";
        try {
            msg = vncConn.getDesktopName();
            int idx = vncConn.getDesktopName().indexOf("(");
            if (idx > -1) {
                // Breakup actual desktop name from IP addresses for improved
                // readability
                String dn = vncConn.getDesktopName().substring(0, idx).trim();
                String ip = vncConn.getDesktopName().substring(idx).trim();
                msg = dn + "\n" + ip;
            }
            msg += "\n" + vncConn.getFramebufferWidth() + "x" + vncConn.getFramebufferHeight();
            String enc = vncConn.getEncoding();
            // Encoding might not be set when we display this message
            if (enc != null && !enc.equals(""))
                msg += ", " + vncConn.getEncoding() + " encoding, " + vncConn.getColorModel().toString();
            else
                msg += ", " + vncConn.getColorModel().toString();
        } catch (NullPointerException e) {
        }
        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
    }

    /**
     * Convert a motion event to a format suitable for sending over the wire
     *
     * @param evt       motion event; x and y must already have been converted from screen coordinates
     *                  to remote frame buffer coordinates.  cameraButton flag is interpreted as second mouse
     *                  button
     * @param downEvent True if "mouse button" (touch or trackball button) is down when this happens
     * @return true if event was actually sent
     */
    public boolean processPointerEvent(MotionEvent evt, boolean downEvent) {
        return processPointerEvent(evt, downEvent, cameraButtonDown);
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

            return vncConn.sendPointerEvent((int) evt.getX(), (int) evt.getY(), evt.getMetaState(), pointerMask);

        } catch (NullPointerException e) {
            return false;
        }
    }

}
