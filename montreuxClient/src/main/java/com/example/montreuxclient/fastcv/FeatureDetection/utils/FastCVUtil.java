package com.example.montreuxclient.fastcv.FeatureDetection.utils;

/**
 * Created by Oleg Kleiman on 26-Feb-15.
 */
public class FastCVUtil{
    /** Native function declarations*/
    public native float getCameraFPS();

    /**
     * Initializes native processing
     */
    public native void init();

    /**
     * Increments camera frame counter to track FPS.
     *
     */
    public native void cameraFrameTick();

    /**
     * Retrieves FastCV processing timing from native layer.
     *
     * @return FastCV processing timing, filtered, in ms.
     */
    public native float getFastCVProcessTime();
}
