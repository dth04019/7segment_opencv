
package com.example.opencvexample;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

public class camera_recognize extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private JavaCameraView mOpenCvCameraView;	// camera preview view
    //private CameraBridgeViewBase mOpenCvCameraView;

    private Mat mIntermediateMat = null;
    private Mat mRgba = null;

    private boolean bDetecting = false;
    private boolean bIsUserView = false;

    private double iBoxWidthRatio = 0.5;
    private final double iBoxAspectRatio = 3.0;
//    private double iBoxWidthRatio = 0.6;
//    private final double iBoxAspectRatio = 3.0;

    private int iBoxTopLeftX;	// the x coordinate of the top left point of the box
    private int iBoxTopLeftY;	// the y coordinate of the top left point of the box
    private int iBoxWidth;		// the width of the box
    private int iBoxHeight;		// the height of the box

    private final int iDigitalDiffThre = 2;	// the threshold for the difference of project value between digital areas and non-digitals areas

    private final double dDigitalRatioMin = 0.1;
    private final double dDigitalRatioMax = 0.8;

    private final double dDigitalWidthVarThre = 0.1;

    private final double dAspectRatioOne = 0.4;

    private final double dDigitalSlope = 16.0;

    private ArrayList<Integer> recog_results = null;

    private Size sSize5;				// will be used for Gaussian blur

    private static final String TAG = "opencv";

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.setCameraPermissionGranted();             //이거 없으면 카메라 동작이 안됨.
                    mOpenCvCameraView.enableView();
                }
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResume :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }


    private ImageView ivScanLine;

    private String sRecogResults = "";
    private android.widget.RelativeLayout.LayoutParams mRelativeParams;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.navigationBars());
                insetsController.hide(WindowInsets.Type.statusBars());
            }
        } else {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_camera_recognize);

        mOpenCvCameraView = (JavaCameraView) findViewById(R.id.OpenCV_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(0);

//        ivScanLine = (ImageView) findViewById(R.id.scanner_green_line);
//        ivScanLine.setVisibility(View.INVISIBLE);

        //
//        mRelativeParams = new android.widget.RelativeLayout.LayoutParams(convertDpToPixel(300), ViewGroup.LayoutParams.WRAP_CONTENT);
//        mRelativeParams.setMargins(convertDpToPixel(15), 0, 0, 0);
//        mRelativeParams.addRule(RelativeLayout.CENTER_VERTICAL);
//        mTextOcrResult.setLayoutParams(mRelativeParams);

        ImageButton mRecogBtn = (ImageButton) findViewById(R.id.ibtn_start);


    }

    public void recognize(View v){
        Scalar color_red = new Scalar( 255, 0, 0 );
        Scalar color_green = new Scalar( 0, 255, 0 );

        Mat ShownImage = mRgba;
        // draw the slope line

        int digital_start = -1;
        int digital_end = -1;
        ArrayList<Integer> digital_points = new ArrayList<Integer>();
        int vert_sum = 0;
        int pre_vert_sum = iBoxHeight;
        for (int i = iBoxTopLeftX; i < iBoxTopLeftX+iBoxWidth; i++) {
            vert_sum = 0;
            for (int j = iBoxTopLeftY; j < iBoxTopLeftY+iBoxHeight; j++) {
                int next_x = (int) (i - (j - iBoxTopLeftY) / dDigitalSlope);
                int next_y = j;

                if (isWhitePoint(mIntermediateMat, next_x, next_y)) {
                    vert_sum++;
                }
            }

            if (!bIsUserView) { // draw the project value
                Imgproc.line(ShownImage, new Point(i-iBoxHeight/dDigitalSlope, iBoxTopLeftY+iBoxHeight), new Point(i-iBoxHeight/dDigitalSlope, iBoxTopLeftY+iBoxHeight+vert_sum*10), color_red);
            }


            if (digital_start < 0) {
                int diff = vert_sum - pre_vert_sum;
                if (diff >= iDigitalDiffThre) {
                    digital_start = i-1;
                }  else {
                    pre_vert_sum = vert_sum;
                }
            } else {
                if (vert_sum <= pre_vert_sum) {
                    digital_end = i;

                    double ratio = (digital_end - digital_start)/((double) iBoxHeight);
                    if (ratio > dDigitalRatioMin/2) {
                        digital_points.add(digital_start);		// record the left and right positions of the digital
                        digital_points.add(digital_end);
                    }

                    digital_start = -1;
                    digital_end = -1;
                }
            }
        }

        // the vertical position of the line of the digitals
        int digitals_line_start = iBoxTopLeftY ;
        int line_height = iBoxHeight;

        //the thresholds for the three traversal directions
        final double vert_mid_thre = 0.5;		// direction a
        final double hori_upp_thre = 0.35;    	// direction b
        final double hori_low_thre = 0.7;     	// direction c

        final double vert_upp_seg_thre = 0.25;	// threshold for check if a segment is located upper when traversing from direction a
        final double hori_left_seg_thre = 0.5;  // threshold for check if a segment is located left when traversing from direction b and c

        if (recog_results == null) {
            recog_results = new ArrayList<Integer>(); // array to record the recognition results
        }

        // draw bounding rectangles
        for (int i = 0; i < digital_points.size(); i += 2) {
            Imgproc.rectangle(ShownImage, new Point(digital_points.get(i), iBoxTopLeftY), new Point(digital_points.get(i+1), iBoxTopLeftY+iBoxHeight), color_green, 2, 8, 0 );
        }

        ArrayList<Integer> digitals_widths = new ArrayList<Integer>(); // widths of the digitals
        int widths_sum = 0;
        if (digital_points.size() > 0) {
            recog_results.clear();

            int recog_code = 0;    // recognition code
            for (int i = 0; i < digital_points.size(); i += 2) {
                recog_code = 0;

                int width = digital_points.get(i + 1) - digital_points.get(i); // the width of a "digital area" (there might be a digital in it)

                int digital_hori_start = digitals_line_start;
                int hori_sum;
                int start_x = digital_points.get(i);
                int digital_hori_prj_cnt = 0;
                int tmp;
                for (tmp = digital_hori_start; tmp < digitals_line_start + line_height / 2; tmp++) {
                    hori_sum = 0;
                    int next_x = (int) (start_x - (tmp - digitals_line_start) / dDigitalSlope);
                    for (int k = 0; k < width; k++) {
                        if (isWhitePoint(mIntermediateMat, next_x + k, tmp)) {
                            hori_sum++;
                        }
                    }

                    if (hori_sum > 0) {
                        digital_hori_prj_cnt++;
                        if (digital_hori_prj_cnt == 5) {
                            digital_hori_start = tmp - 6;
                            break;
                        }
                    } else {
                        digital_hori_prj_cnt = 0;
                    }
                }

                if (tmp >= digitals_line_start + line_height / 2) {
                    continue; // not a digital
                }

                int digital_hori_end = digitals_line_start + line_height;
                digital_hori_prj_cnt = 0;
                for (tmp = digital_hori_end; tmp > digitals_line_start + line_height / 2; tmp--) {
                    hori_sum = 0;
                    int next_x = (int) (start_x - (tmp - digitals_line_start) / dDigitalSlope);
                    for (int k = 0; k < width; k++) {
                        if (isWhitePoint(mIntermediateMat, next_x + k, tmp)) {
                            hori_sum++;
                        }
                    }

                    if (hori_sum > 0) {
                        digital_hori_prj_cnt++;
                        if (digital_hori_prj_cnt == 5) {
                            digital_hori_end = tmp + 6;
                            break;
                        }
                    } else {
                        digital_hori_prj_cnt = 0;
                    }
                }

                if (tmp <= digitals_line_start + line_height / 2) {
                    continue; // not a digital
                }

                int digital_height = digital_hori_end - digital_hori_start + 1;

                if (digital_height < iBoxHeight * 0.5) {
                    continue; // the digital should not be too short
                }

                // we use aspect ratio to validate the digital area
                double digital_ratio = width / ((double) digital_height);

                if (digital_ratio > dDigitalRatioMax
                        || digital_ratio < dDigitalRatioMin) {
                    continue;
                }

                if (digital_ratio < dAspectRatioOne) { // it should be digital "1" for the low aspect ratio
                    if (i > 0 && digital_points.get(i) - 2 * width <= digital_points.get(i - 1)) {
                        continue;    // if an "1" is too close to the previous digital, it should not be a wrong area
                    }
                    recog_results.add(1);
                    continue;
                }

                int vert_line_x = (int) (start_x - (digital_hori_start - digitals_line_start) / dDigitalSlope + (width) * vert_mid_thre);
                int hori_upp_y = (int) (digital_hori_start + digital_height * hori_upp_thre);
                int hori_low_y = (int) (digital_hori_start + digital_height * hori_low_thre);

                // traverse from direction a
                ArrayList<Integer> vertical_results = traverseRect(mIntermediateMat, vert_line_x, digital_hori_start, 0, digital_height);
                if (vertical_results.size() == 1) { // "4" or "7"
                    if ((vertical_results.get(0) / ((double) digital_height)) < vert_upp_seg_thre) {
                        recog_results.add(7);
                        digitals_widths.add(width);
                        widths_sum += width;
                    } else {
                        recog_results.add(4);
                        digitals_widths.add(width);
                        widths_sum += width;
                    }
                    continue;
                }

                if (vertical_results.size() == 2) { // normally, only "0"'s vertical code is 2
                    if ((vertical_results.get(1) - vertical_results.get(0)) / ((double) digital_height) < 0.6) {
                        recog_results.add(4);    // sometimes we got vertical code 2 for "4"
                        digitals_widths.add(width);
                        widths_sum += width;
                        continue;
                    }
                }

                int hori_upp_x = (int) (start_x - (hori_upp_y - digitals_line_start) / dDigitalSlope);
                int hori_low_x = (int) (start_x - (hori_low_y - digitals_line_start) / dDigitalSlope);

                // traverse from direction b
                ArrayList<Integer> horizontal_results_upp = traverseRect(mIntermediateMat, hori_upp_x, hori_upp_y, 1, width);

                // traverse from direction c
                ArrayList<Integer> horizontal_results_low = traverseRect(mIntermediateMat, hori_low_x, hori_low_y, 1, width);

                // calculate the recognition code
                recog_code = vertical_results.size() * 100 + horizontal_results_upp.size() * 10 + horizontal_results_low.size();
                switch (recog_code) {
                    case 322:
                        recog_results.add(8);
                        digitals_widths.add(width);
                        widths_sum += width;
                        break;
                    case 321:
                        recog_results.add(9);
                        digitals_widths.add(width);
                        widths_sum += width;
                        break;
                    case 312:
                        recog_results.add(6);
                        digitals_widths.add(width);
                        widths_sum += width;
                        break;
                    case 311:
                        if ((horizontal_results_upp.get(0) / ((double) width)) < hori_left_seg_thre) {
                            recog_results.add(5);
                            digitals_widths.add(width);
                            widths_sum += width;
                        } else if ((horizontal_results_low.get(0) / ((double) width)) < hori_left_seg_thre) {
                            recog_results.add(2);
                            digitals_widths.add(width);
                            widths_sum += width;
                        } else {
                            recog_results.add(3);
                            digitals_widths.add(width);
                            widths_sum += width;
                        }
                        break;
                    case 222:
                        recog_results.add(0);
                        digitals_widths.add(width);
                        widths_sum += width;
                        break;
                    case 221:    // sometimes, we got the wrong vertical code 2 for "7". in this case, we have to check the full code
                        recog_results.add(7);
                        digitals_widths.add(width);
                        widths_sum += width;
                        break;
                    default:
                        recog_results.add(-1);    // wrong recognition result :(
                        break;
                }
            }

            if (isValidDigtals(digitals_widths, widths_sum)) {
                bDetecting = false;
            }

            sRecogResults = "";
            // print the final results on the screen
            for (int i = 0; i < recog_results.size(); i++) {
                int digital = recog_results.get(i);
                if (digital >= 0) {
                    sRecogResults += String.format("%d", digital);
                } else {
                    sRecogResults += "X";
                }
            }
            sRecogResults += "g";
        }

        //다이얼로그.
        Log.d("results", sRecogResults);

        showResultDialog(sRecogResults);
    }

    public void showResultDialog(String sRecogResults){
        Dialog resultDialog = new Dialog(camera_recognize.this);
        resultDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        resultDialog.setContentView(R.layout.dialogue);

        TextView tv_result_show = resultDialog.findViewById(R.id.result_show);
        Button mBtnAgain = resultDialog.findViewById(R.id.again_btn);
        Button mBtnOther = resultDialog.findViewById(R.id.other_btn);
        Button mBtnCorrect = resultDialog.findViewById(R.id.correct_btn);

        tv_result_show.setText(sRecogResults);
        resultDialog.show();

        mBtnAgain.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                resultDialog.dismiss();
            }
        });
        mBtnOther.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
//                Toast.makeText(getApplicationContext(), "Needs other way", Toast.LENGTH_LONG).show();
                Intent second_method_activity = new Intent(camera_recognize.this, second_method.class);
//                second_method_activity.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                Log.d("Button", "Other button clicked");
                startActivity(second_method_activity);
            }
        });
        mBtnCorrect.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent result_activity = new Intent(camera_recognize.this, ResultActivity.class);
                result_activity.putExtra("weight", sRecogResults);
//                result_activity.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                Log.d("Button", "result button clicked");
                startActivity(result_activity);
            }
        });
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mIntermediateMat = new Mat();
        mRgba = new Mat();
        sSize5 = new Size(5, 5);

        int tmp;

        DisplayMetrics dm = this.getResources().getDisplayMetrics();
        // work out the size of locating box
        iBoxWidth = (int) (width * iBoxWidthRatio);
        iBoxHeight = (int) (iBoxWidth / iBoxAspectRatio);
        iBoxTopLeftX = (int) ((width - iBoxWidth) / 2.0);
        iBoxTopLeftY = (int) ((height - iBoxHeight) / 2.0);

        RelativeLayout cover_center = (RelativeLayout) findViewById(R.id.cover_center);
        ViewGroup.LayoutParams lp = cover_center.getLayoutParams();
        lp.width = (int) (dm.widthPixels * 0.95 * iBoxWidthRatio);      //네비게이션 바 반영.
//        lp.width = iBoxWidth;
//        lp.width = (int) (iBoxWidth * 1.4142121748604112760452131281493);
//        int dp_save = 346;
//        lp.width = 960;
//        lp.width = ConvertDPtoPX(this, dp_save);
//        lp.width = (int)(dm.densityDpi * iBoxWidthRatio);

//        lp.width = 960;

//        lp.width = (int)(dm.xdpi * dm.scaledDensity + 0.5f);


        //제대로 나오는거
//        lp.width = (int)((width * iBoxWidthRatio) * dm.density / 2);
        lp.height = (int) (lp.width / iBoxAspectRatio);


        Log.d("width_compare", width + "   " + height);
        Log.d("width_compare", dm.widthPixels + "   " + dm.densityDpi + "   " + dm.xdpi + "    " + dm.ydpi + "    " + dm.scaledDensity);
        Log.d("width_compare", iBoxWidth + "    " + iBoxHeight + "   " + lp.width + "   " + lp.height);

        cover_center.setLayoutParams(lp);
    }

    public static int ConvertDPtoPX(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    @Override
    public void onCameraViewStopped() {
        releaseMats();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Scalar color_red = new Scalar( 255, 0, 0 );
        Scalar color_green = new Scalar( 0, 255, 0 );

//        Log.d("inputFrame_size", (int)inputFrame.rgba().size().width + "    " + (int)inputFrame.rgba().size().height);
        // get the gray image
        Imgproc.cvtColor(inputFrame.rgba(), mIntermediateMat, Imgproc.COLOR_RGBA2GRAY);

        // do Gaussian blur to prevent getting lots false hits
        Imgproc.GaussianBlur(mIntermediateMat, mIntermediateMat, sSize5, 2, 2);

        // use Canny edge detecting to get the contours in the image
        final int iCannyLowerThre = 25;		// threshold for Canny detection
        final int iCannyUpperThre = 75;		// threshold for Canny detection

        Imgproc.Canny(mIntermediateMat, mIntermediateMat, iCannyLowerThre, iCannyUpperThre);

        Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2BGRA, 4);

        Imgproc.rectangle( mRgba, new Point(iBoxTopLeftX, iBoxTopLeftY), new Point(iBoxTopLeftX+iBoxWidth, iBoxTopLeftY+iBoxHeight), color_red, 2, 8, 0 );
        Imgproc.circle(mRgba, new Point(iBoxTopLeftX, iBoxTopLeftY), 10, color_green, 3);



        return mRgba;
    }


    private void releaseMats () {

        if (mIntermediateMat != null) {
            mIntermediateMat.release();
        }

        if (mRgba != null) {
            mRgba.release();
        }
    }

    private ArrayList<Integer> traverseRect(Mat mat, int start_x, int start_y, int direct, int distance) {
        ArrayList<Integer> results = new ArrayList<Integer>();
        ArrayList<Integer> detected_points = new ArrayList<Integer>();

        // the threshold for the interval between segments
        double seg_inter_thre;
        if (direct == 1) {
            seg_inter_thre = distance * 0.33;
        } else {
            seg_inter_thre = distance * 0.25;
        }

        for (int i = 0; i < distance; i++) {
            int next_x = start_x;
            int next_y = start_y;
            if (direct == 0) { 	// traverse vertically
                next_y += i;
                next_x = (int) (start_x - i / dDigitalSlope);
            } else { 			// traverse horizontally
                next_x += i;
            }

            if (isWhitePoint(mat, next_x, next_y) || i == distance-1) {
                if (detected_points.size() > 0
                        && (i - detected_points.get(detected_points.size()-1) > seg_inter_thre
                        || i == distance-1)) {
                    // should be another segment or we reach the end. So mark the current segment
                    int seg_mid = (int) ((detected_points.get(0) + detected_points.get(detected_points.size()-1)) / 2.0);
                    results.add(seg_mid);

                    detected_points.clear();
                }

                if (i < distance-1)
                    detected_points.add(i);
            }
        }

        return results;
    }

    private boolean isWhitePoint(Mat mat, int x, int y) {
        double white_thre = 100.0;
        double[] tmp = mat.get(y, x);
        if (tmp[0] < white_thre) {
            return false;
        } else {
            return true;
        }
    }


    private boolean isValidDigtals(ArrayList<Integer> widths, int sum) {
        if (widths.size() == 0) // no digital is detected
            return false;

        if (widths.size() == 1) // only one digital is detected
            return true;

        // print the final results on the screen
        sRecogResults = "";

        double avg = ((double) sum) / widths.size();
        double var_sum = 0.0;
        for (int i = 0; i < widths.size(); i++) {

            var_sum += Math.pow(widths.get(i)-avg, 2);

            sRecogResults += String.format("%d, ", widths.get(i));
        }

        double variance = var_sum / widths.size();

        sRecogResults += String.format("%f, ", variance);

//        handler.sendEmptyMessage(iMsgShowResults);

        if (Math.sqrt(variance)/avg < dDigitalWidthVarThre) {
            return true;
        } else {
            return false;
        }
    }
}