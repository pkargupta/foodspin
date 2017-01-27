package com.agnik.priyankakargupta.foodspin;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.gson.Gson;
import com.yelp.clientlib.connection.YelpAPI;
import com.yelp.clientlib.connection.YelpAPIFactory;
import com.yelp.clientlib.entities.SearchResponse;

import org.junit.Before;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;


public class FoodSpinner extends Activity {

    private static Bitmap imageOriginal, imageScaled;
    private static Matrix matrix;
    private Button getnum;
    private ImageView dialer;
    private int dialerHeight, dialerWidth;
    private GestureDetector detector;
    private float totaldegress;
    private boolean[] quadrantTouched;
    private boolean allowRotating;
    private EditText foodtype;
    public static String type;
    public static int spinnernum;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_spinner);
        getnum = (Button) findViewById(R.id.getnum);
        foodtype = (EditText) findViewById(R.id.foodtype);
        getnum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                type = foodtype.getText().toString();
                spinnernum = getSpinnerNum(totaldegress);
                Intent myIntent = new Intent(FoodSpinner.this, RestaurantView.class);
                FoodSpinner.this.startActivity(myIntent);
            }
        });

        // load the image only once
        if (imageOriginal == null) {
            imageOriginal = BitmapFactory.decodeResource(getResources(), R.drawable.graphic_ring);
        }

        // initialize the matrix only once
        if (matrix == null) {
            matrix = new Matrix();
        } else {
            // not needed, you can also post the matrix immediately to restore the old state
            matrix.reset();
        }

        detector = new GestureDetector(this, new MyGestureDetector());

        // the first value will be ignored since there isnt a 0th quadrant
        quadrantTouched = new boolean[] { false, false, false, false, false };

        allowRotating = true;

        dialer = (ImageView) findViewById(R.id.imageView_ring);
        dialer.setOnTouchListener(new MyOnTouchListener());
        dialer.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {

                if (dialerHeight == 0 || dialerWidth == 0) {
                    dialerHeight = dialer.getHeight();
                    dialerWidth = dialer.getWidth();


                    Matrix resize = new Matrix();
                    resize.postScale((float)Math.min(dialerWidth, dialerHeight) / (float)imageOriginal.getWidth(), (float)Math.min(dialerWidth, dialerHeight) / (float)imageOriginal.getHeight());
                    imageScaled = Bitmap.createBitmap(imageOriginal, 0, 0, imageOriginal.getWidth(), imageOriginal.getHeight(), resize, false);

                    // translate to the image view's center
                    float translateX = dialerWidth / 2 - imageScaled.getWidth() / 2;
                    float translateY = dialerHeight / 2 - imageScaled.getHeight() / 2;
                    matrix.postTranslate(translateX, translateY);
                    dialer.setImageBitmap(imageScaled);
                    dialer.setImageMatrix(matrix);

                }

            }
        });

    }

    /**
     * this rotates the dialer based on the num of degrees
     * fyi we are calculating the total degress rotated here to identify what num the spinner is on
     */
    private void rotateDialer(float degrees) {
        matrix.postRotate(degrees, dialerWidth/2, dialerHeight/2);
        dialer.setImageMatrix(matrix);
            if(Math.signum(totaldegress + degrees) == 1 && totaldegress + degrees <= 360)
                totaldegress += degrees;
            else if(Math.signum(totaldegress + degrees) == 1)
                totaldegress = (totaldegress + degrees) - 360;
            else if(Math.signum(totaldegress + degrees) == -1 && totaldegress + degrees >= -360)
                totaldegress += degrees;
            else if(Math.signum(totaldegress + degrees) == -1)
                totaldegress = (totaldegress + degrees) + 360;
        }

    /**
     * @return The angle of the unit circle with the image view's center
     */
    private double getAngle(double xTouch, double yTouch) {
        double x = xTouch - (dialerWidth / 2d);
        double y = dialerHeight - yTouch - (dialerHeight / 2d);

        switch (getQuadrant(x, y)) {
            case 1:
                return Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;

            case 2:
            case 3:
                return 180 - (Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI);

            case 4:
                return 360 + Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;

            default:
                // ignore, does not happen
                return 0;
        }
    }

    private static int getQuadrant(double x, double y) {
        if (x >= 0) {
            return y >= 0 ? 1 : 4;
        } else {
            return y >= 0 ? 2 : 3;
        }
    }

    /**
     * so based on the totaldegree value calculated in the rotatedialer method, we are identifying which range corresponds to
     * which spinner num
     * @param d is the total degrees essentially
     * @return the spinner num
     */
    private int getSpinnerNum(float d){
        if(Math.signum(d) == 1 || Math.signum(d) == 0) {
            if (d <= 30 || d > 328.5)
                return 1;
            else if (d <= 91 && d > 30)
                return 6;
            else if (d <= 152.5 && d > 91)
                return 5;
            else if (d <= 213 && d > 152.5)
                return 4;
            else if (d <= 271.5 && d > 213)
                return 3;
            else if (d <= 328.5 && d > 271.5)
                return 2;
            //doesn't happen so ignore it
            else
                return 0;
        }
        else{
            if (d <= -329 || d > -31)
                return 1;
            else if (d <= -268 && d > -329)
                return 6;
            else if (d <= -206 && d > -268)
                return 5;
            else if (d <= -146 && d > -206)
                return 4;
            else if (d <= -88 && d > -146)
                return 3;
            else if (d <= -31 && d > -88)
                return 2;
            else
                return 0;
        }
    }

    /**
     * this registers the touch events of the spinner and calls the methods corresponding to them
     * ACTION_DOWN is when you first press down on spinner
     * ACTION_MOVE is when the spinner is moving/spinning
     * ACTION_UP is when you releasing your finger from the spinner
     */
    private class MyOnTouchListener implements OnTouchListener {

        private double startAngle;

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    getnum.setEnabled(false);
                    // reset the touched quadrants
                    for (int i = 0; i < quadrantTouched.length; i++) {
                        quadrantTouched[i] = false;
                    }

                    allowRotating = false;
                    startAngle = getAngle(event.getX(), event.getY());
                    break;

                case MotionEvent.ACTION_MOVE:
                    double currentAngle = getAngle(event.getX(), event.getY());
                    rotateDialer((float) (startAngle - currentAngle));
                    startAngle = currentAngle;
                    break;

                case MotionEvent.ACTION_UP:
                    getnum.setEnabled(true);
                    allowRotating = true;
                    break;
            }

            // set the touched quadrant to true
            quadrantTouched[getQuadrant(event.getX() - (dialerWidth / 2), dialerHeight - event.getY() - (dialerHeight / 2))] = true;

            detector.onTouchEvent(event);

            return true;
        }
    }

    /**
     * This adds a fling kind of animation to the spinner with the FlingRunnable class(speeding up and slowing down all that stuff)
     */
    private class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            // get the quadrant of the start and the end of the fling
            int q1 = getQuadrant(e1.getX() - (dialerWidth / 2), dialerHeight - e1.getY() - (dialerHeight / 2));
            int q2 = getQuadrant(e2.getX() - (dialerWidth / 2), dialerHeight - e2.getY() - (dialerHeight / 2));

            // the inversed rotations
            if ((q1 == 2 && q2 == 2 && Math.abs(velocityX) < Math.abs(velocityY))
                    || (q1 == 3 && q2 == 3)
                    || (q1 == 1 && q2 == 3)
                    || (q1 == 4 && q2 == 4 && Math.abs(velocityX) > Math.abs(velocityY))
                    || ((q1 == 2 && q2 == 3) || (q1 == 3 && q2 == 2))
                    || ((q1 == 3 && q2 == 4) || (q1 == 4 && q2 == 3))
                    || (q1 == 2 && q2 == 4 && quadrantTouched[3])
                    || (q1 == 4 && q2 == 2 && quadrantTouched[3])) {

                dialer.post(new FlingRunnable(-1 * (velocityX + velocityY)));
            } else {
                // the normal rotation
                dialer.post(new FlingRunnable(velocityX + velocityY));
            }

            return true;
        }
    }
    private class FlingRunnable implements Runnable {

        private float velocity;

        public FlingRunnable(float velocity) {
            this.velocity = velocity;
        }

        @Override
        public void run() {
            if (Math.abs(velocity) > 5 && allowRotating) {
                rotateDialer(velocity / 75);
                velocity /= 1.0666F;

                // post this instance again
                dialer.post(this);
            }
        }
    }
}