package orb.slam2.android;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import orb.slam2.android.nativefunc.OrbNdkHelper;

/**
 * ORB Test Activity For DataSetMode
 *
 * @author buptzhaofang@163.com Mar 24, 2016 4:13:32 PM
 *
 */
public class ORBSLAMForTestActivity extends Activity implements
        Renderer,CvCameraViewListener2, View.OnClickListener, LocationListener, SensorEventListener {

    //maxiaoba
    //add on April 27th ,li
    Button TrackOnly,calibrationStart, distanceStart;
    boolean calibrationIsStart;
    boolean distanceIsStart;
    boolean distanceIsEnd;
    TextView dataTextView, distTextView;
    LocationManager locationManager;
    String provider;
    float displacement;
    public static Location location;
    public static double lng;
    public static double lat;
    boolean checkPermission;
    public static SensorManager mSensorManager;
    public Sensor linearAccelerometer;
    public Sensor gravitySensor;
    public Sensor gyroscope;
    public Sensor magnetometer;
    public Sensor accelerometer;
    public static double[] acce;
    public static double[] gyro;
    public static float[] mGravity;
    public static float[] mGeomagnetic;
    public static float[] RMatrix;
    public static float[] IMatrix;
    public static float[] orientation;
    public static float[] RCaptured;
    public double timestamp;
    public double timestampOld;
    public double timeStep;
    public double[] velocity;
    //public RK4 velocityCalculator;
    public float[] caliStartPos;
    public float[] caliEndPos;
    public float[] distStartPos;
    public float[] distEndPos;
    //public float[] scale;
    public float scale;
    public float[] pos;
    public float[] oldpos;

    //maxiaoba
    private static final String TAG = "OCVSample::Activity";
    ImageView imgDealed;

    LinearLayout linear;

    String vocPath, calibrationPath;

    private static final int INIT_FINISHED=0x00010001;

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;

    private final int CONTEXT_CLIENT_VERSION = 3;
    private GLSurfaceView mGLSurfaceView;

    long addr;
    int w,h;
    boolean isSLAMRunning=true;

    //maxiaoba
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    static {
        System.loadLibrary("ORB_SLAM2_EXCUTOR");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 隐藏标题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_test_orb);


        //maxiaoba
        calibrationIsStart = false;
        displacement = 0;
        distanceIsStart = false;
        distanceIsEnd=false;
        pos = new float[3];
        oldpos = new float[3];
//        scale = new float[3];
//        scale[0] = 1;
//        scale[1] = 1;
//        scale[2] = 1;
        scale = 1;
        caliStartPos = new float[3];
        caliEndPos = new float[3];
        distStartPos = new float[3];
        distEndPos = new float[3];
        //velocityCalculator = new RK4();
        acce = new double[3];
        velocity = new double[3];
        gyro = new double[3];
        RMatrix = new float[9];
        IMatrix = new float[9];
        orientation = new float[3];
        RCaptured = RMatrix;
        dataTextView = (TextView) findViewById(R.id.dataTextView);
        distTextView = (TextView) findViewById(R.id.distTextView);
        TrackOnly=(Button)findViewById(R.id.track_only);
//        dataCollection = (Button) findViewById(R.id.data_collection);
        calibrationStart = (Button) findViewById(R.id.calibrationStart);
        distanceStart = (Button) findViewById(R.id.distanceStart);
//        calibrationEnd = (Button) findViewById(R.id.calibrationEnd);
        TrackOnly.setOnClickListener(this);
//        dataCollection.setOnClickListener(this);
        calibrationStart.setOnClickListener(this);
        distanceStart.setOnClickListener(this);
//        calibrationEnd.setOnClickListener(this);
        //maxiaoba

        imgDealed = (ImageView) findViewById(R.id.img_dealed);

        if (mIsJavaCamera)
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        else
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_native_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mGLSurfaceView = new GLSurfaceView(this);
        linear = (LinearLayout) findViewById(R.id.surfaceLinear);
        //mGLSurfaceView.setEGLContextClientVersion(CONTEXT_CLIENT_VERSION);
        mGLSurfaceView.setRenderer(this);
        linear.addView(mGLSurfaceView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        vocPath = getIntent().getStringExtra("voc");
        calibrationPath = getIntent().getStringExtra("calibration");
        if (TextUtils.isEmpty(vocPath) || TextUtils.isEmpty(calibrationPath)) {
            Toast.makeText(this, "null param,return!", Toast.LENGTH_LONG)
                    .show();
            finish();
        } else {
            Toast.makeText(ORBSLAMForTestActivity.this, "init has been started!",
                    Toast.LENGTH_LONG).show();
            new Thread(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    OrbNdkHelper.initSystemWithParameters(vocPath,
                            calibrationPath);
                    Log.e("information==========>", "init has been finished!");
                    myHandler.sendEmptyMessage(INIT_FINISHED);
                }
            }).start();
        }



        /// Motion Sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        linearAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        dataTextView.setText("No Data");

    }


    //maxiaoba
    //change on April 27th ,li
    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch(v.getId()) {
            case R.id.track_only:
                OrbNdkHelper.trackOnly();
                Toast.makeText(ORBSLAMForTestActivity.this, "Track Only", Toast.LENGTH_LONG).show();
                break;


            case R.id.calibrationStart:
                if (!calibrationIsStart) {
                    calibrationIsStart = true;
                    calibrationStart.setText("End Calibration");
                    for (int i = 0; i < 3; i++) {
                        caliStartPos[i] = pos[i];
                    }
                    Toast.makeText(this, "Calibration Start", Toast.LENGTH_LONG)
                            .show();
                }
                else if (calibrationIsStart) {

                    calibrationIsStart = false;
                    calibrationStart.setText("Start Calibration");
                    for (int i = 0; i < 3; i++) {
                        caliEndPos[i] = pos[i];
                    }
                    displacement = (float) Math.sqrt(Math.pow(caliEndPos[0] - caliStartPos[0], 2) + Math.pow(caliEndPos[1] - caliStartPos[1], 2) + Math.pow(caliEndPos[2] - caliStartPos[2], 2));
                    scale = (float) (2.0 / displacement) * scale;
                    Toast.makeText(this, "Calibration End", Toast.LENGTH_LONG)
                            .show();
                }
                break;
            case R.id.distanceStart:
                if (!distanceIsStart && !distanceIsEnd) {
                    distanceIsStart = true;
                    distanceIsEnd=false;
                    distanceStart.setText("End Distance");
                    for (int i = 0; i < 3; i++) {
                        distStartPos[i] = pos[i];
                    }
                    Toast.makeText(this, "Distance Calculation Start", Toast.LENGTH_LONG)
                            .show();
                }
                else if (distanceIsStart && !distanceIsEnd) {
                    distanceIsStart = false;
                    distanceIsEnd=true;
                    distanceStart.setText("No Distance");
                    for (int i = 0; i < 3; i++) {
                        distEndPos[i] = pos[i];
                    }
                    displacement = (float) Math.sqrt(Math.pow(distEndPos[0] - distStartPos[0], 2) + Math.pow(distEndPos[1] - distStartPos[1], 2) + Math.pow(distEndPos[2] - distStartPos[2], 2));
                    distTextView.setText("Moving Distance: " + String.valueOf(displacement)+"m");

                    Toast.makeText(this, "Distance Calculation End", Toast.LENGTH_LONG)
                            .show();
                }
                else if(!distanceIsStart && distanceIsEnd){
                    distanceIsStart = false;
                    distanceIsEnd=false;
                    distanceStart.setText("Start Distance");
                    OrbNdkHelper.trackOnly();
                    Toast.makeText(ORBSLAMForTestActivity.this, "Track Only, don't calculate distance", Toast.LENGTH_LONG).show();
                    distTextView.setText("");
                }
                break;

        }
    }
    //maxiaoba

    Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {


            switch (msg.what) {
                case INIT_FINISHED:
                    Toast.makeText(ORBSLAMForTestActivity.this,
                            "init has been finished!",
                            Toast.LENGTH_LONG).show();

                    //maxiaoba
                   // velocityCalculator.startFlag = 1;

                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            while(isSLAMRunning){
                                timestampOld = timestamp;
                                timestamp = (double)System.currentTimeMillis()/1000.0;
                                timeStep = timestamp-timestampOld;
                                // TODO Auto-generated method stub
                                RCaptured=RMatrix;
                                // resultfloat = OrbNdkHelper.startCurrentORBForCamera2(timestamp, addr, w, h, RCaptured);
                                 resultfloat = OrbNdkHelper.startCurrentORBForCamera(timestamp, addr, w, h);
  //                              resultImg = Bitmap.createBitmap(w, h,
  //                                      Config.RGB_565);
  //                              resultImg.setPixels(resultInt, 0, w, 0, 0, w, h);




                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // TODO Auto-generated method stub
//
                                        //change on April 27th ,li
                                        if(resultfloat.length==16) {
                                            float[] OCc = {-resultfloat[3], -resultfloat[7], -resultfloat[11]};
                                            float[] OCb = {-OCc[1], -OCc[0], -OCc[2]};
                                            float[] OCe = {RCaptured[0] * OCb[0] + RCaptured[1] * OCb[1] + RCaptured[2] * OCb[2],RCaptured[3] * OCb[0] + RCaptured[4] * OCb[1] + RCaptured[5] * OCb[2],RCaptured[6] * OCb[0] + RCaptured[7] * OCb[1] + RCaptured[8] * OCb[2]};
                                            for(int i=0;i<3;i++){
                                                //pos[i] = scale[i]*OCe[i];
                                                pos[i] = scale*OCe[i];
                                            }
                                            if (Math.abs(pos[0] - oldpos[0])>0.000001 ||Math.abs( pos[1] - oldpos[1])>0.000001 || Math.abs(pos[2] - oldpos[2])>0.000001) {
                                                dataTextView.setText("SLAM on");//("Time Step: "+String.valueOf(timeStep)+"\nX: " + String.valueOf(pos[0]) + "\nY: " + String.valueOf(pos[1]) + "\n" + "Z:" + String.valueOf(pos[2])+"\n"+"Scale: "+String.valueOf(scale));
                                            }
                                            else {
                                                dataTextView.setText("unknown");
                                            }
                                            oldpos[0] = pos[0];
                                            oldpos[1] = pos[1];
                                            oldpos[2] = pos[2];

                                        } else {
                                            dataTextView.setText("unknown");
                                        }

                                        if (distanceIsStart) {
                                            distTextView.setText("Moving Distance: " );
                                        }

                                    }
                                });

                            }
                        }
                    }).start();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private Bitmap tmp, resultImg;
    private float[] resultfloat;
   //maxiaoba private double timestamp;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // TODO Auto-generated method stub
        //OrbNdkHelper.readShaderFile(mAssetMgr);
        OrbNdkHelper.glesInit();
    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // TODO Auto-generated method stub
        OrbNdkHelper.glesResize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // TODO Auto-generated method stub
        OrbNdkHelper.glesRender();
    }


    //change on April 27th ,li
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        mGLSurfaceView.onResume();

        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
       if(!OpenCVLoader.initDebug()){
           //handle initialization error
           OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
       }else{
           Log.d(TAG,"opencv library found inside package");
           mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);


       }


        // mSensorManager.registerListener(this, linearAccelerometer, 200000);
        mSensorManager.registerListener(this, linearAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, gravitySensor, 50000);
        mSensorManager.registerListener(this, gyroscope, 100000);
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);
//        if (checkPermission) {
//            locationManager.requestLocationUpdates(provider, 100, 0, this);
//        }
    }



    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        mGLSurfaceView.onPause();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        mSensorManager.unregisterListener(this);

    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemSwitchCamera = menu.add("stop SLAM");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String toastMesage = new String();
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemSwitchCamera) {
            isSLAMRunning=false;
//
        }

        return true;
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat im=inputFrame.rgba();
        synchronized (im) {
            addr=im.getNativeObjAddr();
        }

        w=im.cols();
        h=im.rows();
        return inputFrame.rgba();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkPermission = true;
                } else {
                    checkPermission = false;
                }
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long time= System.currentTimeMillis();
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            acce[0] = event.values[0];
            acce[1] = event.values[1];
            acce[2] = event.values[2];
           // velocityCalculator.calculateVelocity();
        }
//
        else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyro[0] = event.values[0];
            gyro[1] = event.values[1];
            gyro[2] = event.values[2];
                // gyroTextView.setText("X: " + String.valueOf(event.values[0]) + "\nY: " + String.valueOf(event.values[1]) + "\nZ: " + String.valueOf(event.values[2]));
        }
        else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            boolean success = SensorManager.getRotationMatrix(RMatrix, IMatrix, mGravity, mGeomagnetic);
            if (success) {
                SensorManager.getOrientation(RMatrix, orientation);
                //azimut = orientation[0]; // orientation contains: azimut, pitch and roll
            }
        }
//
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }




}
