package gmrcp.reactnativeescpos.helpers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.os.Handler;

import java.io.ByteArrayOutputStream;

// @ref https://gist.github.com/douglasjunior/dc3b41908514304f694f1b37cadf2df7
public class EscPosHelper {
    /**
     * Collect a slice of 3 bytes with 24 dots for image printing.
     *
     * @param y     row position of the pixel.
     * @param x     column position of the pixel.
     * @param image 2D array of pixels of the image (RGB, row major order).
     * @return 3 byte array with 24 dots (field set).
     */

    public static byte[] collectImageSlice(int y, int x, Bitmap image) {
        byte[] slices = new byte[] { 0, 0, 0 };
        for (int yy = y, i = 0; yy < y + 24 && i < 3; yy += 8, i++) { // repeat for 3 cycles
            byte slice = 0;
            for (int b = 0; b < 8; b++) {
                int yyy = yy + b;
                if (yyy >= image.getHeight()) {
                    continue;
                }
                int color = image.getPixel(x, yyy);
                boolean v = shouldPrintColor(color);
                slice |= (byte) ((v ? 1 : 0) << (7 - b));
            }
            slices[i] = slice;
        }

        return slices;
    }

    /**
     * Resizes a Bitmap image.
     *
     * @param image
     * @param width
     * @return new Bitmap image.
     */
    public static Bitmap resizeImage(Bitmap image, int width) {
        int origHeight = image.getHeight();
        int origWidth = image.getWidth();
        final int destWidth = width;
        float ratio = (float) origWidth / destWidth;
        int destHeight = (int) (origHeight / ratio);

        // we create an scaled bitmap so it reduces the image, not just trim it
        Bitmap newImage = Bitmap.createScaledBitmap(image, destWidth, destHeight, false);

        return newImage;
    }

    public static Bitmap resizeImage(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > ratioBitmap) {
                finalWidth = (int) ((float)maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float)maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
        }
        return image;
    }

    /**
     * Defines if a color should be printed (burned).
     *
     * @param color RGB color.
     * @return true if should be printed/burned (black), false otherwise (white).
     */
    public static boolean shouldPrintColor(int color) {
        final int threshold = 127;
        int a, r, g, b, luminance;
        a = (color >> 24) & 0xff;
        if (a != 0xff) { // ignore pixels with alpha channel
            return false;
        }
        r = (color >> 16) & 0xff;
        g = (color >> 8) & 0xff;
        b = color & 0xff;

        luminance = (int) (0.299 * r + 0.587 * g + 0.114 * b);

        return luminance < threshold;
    }
    

    public static Object setTimeout(Runnable runnable, long delay) {
        TimeoutEvent te =  new TimeoutEvent(runnable, delay);
        return te;
    }

    public static void clearTimeout(Object timeoutEvent) {
        if (timeoutEvent != null && timeoutEvent instanceof TimeoutEvent) {
            ((TimeoutEvent) timeoutEvent).cancelTimeout();
        }
    }

    private static class TimeoutEvent {
        private static Handler handler = new Handler();
        private volatile Runnable runnable;

        private TimeoutEvent(Runnable task, long delay) {
            runnable = task;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (runnable != null) {
                        runnable.run();
                    }
                }
            }, delay);
        }

        private void cancelTimeout() {
            runnable = null;
        }
    }
}
