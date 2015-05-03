package com.example.montreuxclient.fastcv.FeatureDetection.base;

/**
 * Created by Oleg on 26-Feb-15.
 */

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.montreuxclient.R;
import com.example.montreuxclient.fastcv.FeatureDetection.utils.CameraSurface;
import com.example.montreuxclient.fastcv.FeatureDetection.utils.FastCVSampleRenderer;
import com.example.montreuxclient.fastcv.FeatureDetection.utils.FastCVUtil;
import com.example.montreuxclient.fastcv.SquaredWrapper;

import java.util.List;

/** The main activity for the FastCV FAST Corner sample app. */
public abstract class GraphicalActivity extends Activity {

    /** Logging tag */
    protected static final String         TAG_LOG               = "Montreux_FastCV";

    /** Width to try and configure camera preview */
    protected static int                  mDesiredWidth     = 800;

    /** Height to try and configure camera preview */
    protected static int                  mDesiredHeight    = 480;

    /** Actual width of camera preview image, can be different
     *  than desired if camera doesn't support desired width. */
    protected int                         mPreviewWidth;

    /** Actual height of camera preview image, can be different
     *  than desired if camera doesn't support desired width. */
    protected int                         mPreviewHeight;

    /** Filtered processing time   */
    protected float                       mProcessTime      = 10;

    protected long startTime;

    /** Main layout */
    protected int                         mMainLayout = R.layout.activity_corner;

    protected FastCVUtil util = new FastCVUtil();

    /** Android camera object */
    protected Camera                      mCamera;

    /** GL Surface view to render camera and corners via GPU */
    protected GLSurfaceView mGLSurface = null;

    /** Surface Holder for camera, Camera API requirement */
    protected CameraSurface mCamSurface = null;

    /** Root view group for all views associated with this
     *  activity */
    protected ViewGroup mRootViewGroup = null;

    /** Java class associated with the camera and corner
     *  renderering */
    protected FastCVSampleRenderer mRenderer;

    /** Text boxes for on screen display of information */
    protected TextView[]                  mText = new TextView[3];

    /** Android UI button to switch between start and stop processing */
    protected ToggleButton tbtnStartStop;

    /** Text color for text boxes */
    protected final int                   mTextColor        = 0xFFFFFFFF; // ARGB

    /** Buffer for camera driver to store preview data, created
     *  statically so that it doesn't need to be re-allocated
     *  each frame. */
    protected byte[]                      mPreviewBuffer    = null;

    /** Flag indicates whether or not preview is running. */
    protected boolean                     mPreviewRunning   = false;

    /** How long to wait between profiling updates. */
    protected long                        mProfilePeriod = 300;

    protected static String title = "Montreux_FastCV";

    /** Handler for when to update profiling information. */
    protected Handler mProfileHandler = new Handler();

    static {
            //System.loadLibrary("squared");
            System.loadLibrary("fastcvUtils");

    }

    /**
     * Camera callback to retrieve camera frames.
     *
     */
    protected final Camera.PreviewCallback mCameraCallback =
            new Camera.PreviewCallback(){
                public void onPreviewFrame( byte[] data, Camera c ){
                    // Increment FPS counter for camera.
                    util.cameraFrameTick();

                    // Perform processing on the camera preview data.
                    update( data, mDesiredWidth, mDesiredHeight );

                    // Simple IIR filter on time.
                    mProcessTime = util.getFastCVProcessTime();
                }
            };

    /**
     * Handler for FPS display. Want to do this periodically to not
     * waste CPU.
     */
    protected Runnable mUpdateTimeTask = new Runnable() {
        /**
         * Overridden run function to handle callback This updates
         * the fps and number of corners detected for the user
         */
        public void run() {
            float camFPS = util.getCameraFPS();
        }
    };
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        startTime = System.currentTimeMillis();
    }
    /**
     * Resumes camera preview
     */
    @Override
    protected void onResume() {

        Log.v(TAG_LOG, "GraphicalActivity: onResume()");
        super.onResume();

        // Initialize UI
        setContentView( mMainLayout );

        initTitle();
        setTitle( title );
        setTitleColor( 0xFFFF5555 );

        util.init();

        int screenw = ((WindowManager)this.getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();

        mText[0] = (TextView) findViewById(R.id.text1);
        mText[1] = (TextView) findViewById(R.id.text2);
        mText[2] = (TextView) findViewById(R.id.text3);

        mRootViewGroup = (ViewGroup)mText[0].getParent();

        int width = (screenw * 1 / 2 );

        for( int i=0; i<mText.length; i++ )
        {
            mText[i].setTextColor( mTextColor );
            mText[i].setWidth( width );
            mText[i].setText( "" );
        }

        tbtnStartStop = (ToggleButton) findViewById(R.id.startStopSwitch);
        tbtnStartStop.setOnClickListener
                (
                        new View.OnClickListener()
                        {
                            public void onClick( View view )
                            {
                                if (tbtnStartStop.isChecked())
                                {
                                    startPreview();
                                }
                                else
                                {
                                    stopPreview();
                                }
                            }
                        }
                );

        mCamera = Camera.open();

        // Camera surface is required for Camera to send
        // preview frame callback on some devices.
        // The order this is added to the rootViewGroup matters to ensure it is
        // below GLSurface. GLSurface is actually used for rendering.

        // Further note, when splash screen is not used this should be added
        // after GLSurfaceView. Really tricky to get ordering correct.
        mCamSurface = new CameraSurface( this, mCamera );
        mRootViewGroup.addView( mCamSurface );

        // Initialize the renderer
        mRenderer = new FastCVSampleRenderer( this );
        mGLSurface = mRenderer.getGLSurfaceView();
        mGLSurface.setZOrderMediaOverlay(true);
        mGLSurface.setVisibility( View.VISIBLE );
        mGLSurface.setRenderMode( GLSurfaceView.RENDERMODE_WHEN_DIRTY );
        mRootViewGroup.addView( mGLSurface );

        bringControlsToFront();

        startPreview();

        System.gc();
    }

    /**
     * Brings the controls to the foreground
     */
    protected void bringControlsToFront()
    {
        for( int i=0; i<mText.length; i++ )
        {
            mText[i].bringToFront();
        }

        tbtnStartStop.bringToFront();
    }

    /**
     * Starts camera preview
     */
    protected void startPreview()
    {
        Log.v(TAG_LOG, "GraphicalActivity: startPreview()");
        if( mCamera != null )
        {
            setupCamera();
            setCallback();
            mCamera.startPreview();
            mPreviewRunning = true;
        }
        else
        {
            mPreviewRunning = false;
        }

        mProfileHandler.removeCallbacks( mUpdateTimeTask );
        mProfileHandler.postDelayed( mUpdateTimeTask, mProfilePeriod );
    }

    /**
     * Stops camera preview
     */
    protected void stopPreview()
    {
        Log.v(TAG_LOG, "GraphicalActivity: stopPreview()");
        if ((mCamera != null) && (mPreviewRunning == true))
        {
            mCamera.stopPreview();
            mCamera.setPreviewCallback( null );
        }

        mProfileHandler.removeCallbacks( mUpdateTimeTask );

        mPreviewRunning = false;
        System.gc();
    }
    /**
     * Sets up  camera configuration parameters
     */
    protected void setupCamera()
    {
        Log.v( TAG_LOG, "GraphicalActivity: setupCamera()");
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();
        Camera.Size list = sizes.get(0);
        mDesiredWidth = list.width;
        mDesiredHeight = list.height;
        for (Camera.Size s: sizes)
        {   if (s.height == 480)
        {
            mDesiredWidth = s.width;
            mDesiredHeight = s.height;
            break;
        }
        }

        parameters.setPreviewFrameRate( 30 );
        parameters.setPreviewSize( mDesiredWidth, mDesiredHeight );
        //
        //  Set Focus mode depending on what is supported. MODE_INFINITY is
        //  preferred mode.
        //
        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        if( supportedFocusModes!= null )
        {
            if( supportedFocusModes.contains
                    (
                            Camera.Parameters.FOCUS_MODE_INFINITY
                    ) )
            {
                parameters.setFocusMode( Camera.Parameters.FOCUS_MODE_INFINITY );
                Log.v( TAG_LOG, "Set focus mode INFINITY" );
            }
            else if( supportedFocusModes.contains
                    (
                            Camera.Parameters.FOCUS_MODE_FIXED
                    ) )
            {
                parameters.setFocusMode( Camera.Parameters.FOCUS_MODE_FIXED );
                Log.v( TAG_LOG, "Set focus mode FIXED" );
            }
            else if( supportedFocusModes.contains
                    (
                            Camera.Parameters.FOCUS_MODE_AUTO
                    ) )
            {
                parameters.setFocusMode( Camera.Parameters.FOCUS_MODE_AUTO);
                Log.v( TAG_LOG, "Set focus mode AUTO" );
            }
        }
        //
        // Set White Balance to Auto if supported.
        //
        List<String> supportedWhiteBalance =
                parameters.getSupportedWhiteBalance();
        if( supportedWhiteBalance != null &&
                supportedWhiteBalance.contains
                        (
                                Camera.Parameters.WHITE_BALANCE_AUTO
                        ) )
        {
            parameters.setWhiteBalance( Camera.Parameters.WHITE_BALANCE_AUTO );
            Log.v( TAG_LOG, "Set white balance AUTO" );
        }

        try
        {
            mCamera.setParameters( parameters );
        }
        catch( RuntimeException re )
        {
            //
            // NOTE that we shouldn't be here as we check where our specified
            // parameters are supported or not.
            //
            re.printStackTrace();
            Log.e( TAG_LOG,"Unable to set Camera Parameters" );
            Log.i( TAG_LOG,"Falling back to setting just the camera preview" );
            parameters = mCamera.getParameters();
            parameters.setPreviewSize( mDesiredWidth, mDesiredHeight );
            try
            {
                mCamera.setParameters( parameters );
            }
            catch( RuntimeException re2 )
            {
                re2.printStackTrace();
                Log.e( TAG_LOG, "Problem with camera configuration, unable to set "+
                        "Camera Parameters. Camera not available." );

            }
        }

        mPreviewWidth = mCamera.getParameters().getPreviewSize().width;
        mPreviewHeight = mCamera.getParameters().getPreviewSize().height;

    }

    /**
     * Sets the Camera preview callback
     *
     */
    public void setCallback()
    {
        int bufferSize = 0;
        int pformat;
        int bitsPerPixel;

        pformat = mCamera.getParameters().getPreviewFormat();

        // Get pixel format information to compute buffer size.
        PixelFormat info = new PixelFormat();
        PixelFormat.getPixelFormatInfo( pformat, info );
        bitsPerPixel = info.bitsPerPixel;

        bufferSize = mPreviewWidth * mPreviewHeight * bitsPerPixel / 8;

        // Make sure buffer is deleted before creating a new one.
        mPreviewBuffer = null;

        // New preview buffer.
        mPreviewBuffer = new byte[ bufferSize + 4096 ];

        // with buffer requires addbuffer.
        mCamera.addCallbackBuffer( mPreviewBuffer );
        mCamera.setPreviewCallbackWithBuffer( mCameraCallback );
    }

    /**
     * Suspends camera preview, cleans up the renderer
     */
    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * Function to pass camera frame for native, FastCV processing.
     * @param data Byte buffer for data.
     * @param w Width of data
     * @param h Height of data
     */
    abstract public void update( byte[] data, int w, int h );

    abstract protected void initTitle();
}
