package dongwei.myapplication;

/* Copyright 2012 ESRI
 *
 * All rights reserved under the copyright laws of the United States
 * and applicable international laws, treaties, and conventions.
 *
 * You may freely redistribute and use this sample code, with or
 * without modification, provided you include the original copyright
 * notice and use restrictions.
 *
 * See the sample code usage restrictions document for further information.
 *
 */
import com.esri.arcgisruntime.mapping.view.MapRotationChangedEvent;
import com.esri.arcgisruntime.mapping.view.MapRotationChangedListener;
import com.esri.arcgisruntime.mapping.view.MapView;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
/**
 * This class defines a custom view that draws an image of a compass. The angle of the compass changes when the
 * setRotationAngle method is called. If a MapView is passed to the constructor, an OnPinchListener is set in order to
 * update the compass rotation angle when the pinch gesture is used on the MapView.
 */
public class Compass extends View {

    float mAngle = 0;

    Paint mPaint;

    Bitmap mBitmap;

    Matrix mMatrix;

    MapView mMapView;

    // Called when the Compass view is inflated from XML. In this case, no attributes are initialized from XML.
    public Compass(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Create a Paint, Matrix and Bitmap that will be re-used together to draw the
        // compass image each time the onDraw method is called.
        mPaint = new Paint();
        mMatrix = new Matrix();


        // Create the bitmap of the compass from a resource.
        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.arrow);
    }

    /** Overloaded constructor that takes a MapView, from which the compass rotation angle will be set. */
    public Compass(Context context, AttributeSet attrs, MapView mapView) {
        this(context, attrs);

        // Save reference to the MapView passed in to this compass.
        mMapView = mapView;
        if (mMapView != null) {

            // Set an OnPinchListener on the map to listen for the pinch gesture which may change the map rotation.
            mMapView.addMapRotationChangedListener(new MapRotationChangedListener() {
                @Override
                public void mapRotationChanged(MapRotationChangedEvent mapRotationChangedEvent) {
                    setRotationAngle(mMapView.getMapRotation());
                }
            } );
        }
    }

    /** Updates the angle, in degrees, at which the compass is draw within this view. */
    public void setRotationAngle(double angle) {
        // Save the new rotation angle.
        mAngle = (float) angle;

        // Force the compass to re-paint itself.
        postInvalidate();
    }

    /** Draws the compass image at the current angle of rotation on the canvas. */
    @Override
    protected void onDraw(Canvas canvas) {

        // Reset the matrix to default values.
        mMatrix.reset();
        mMatrix.setTranslate(25,15);

        // Pass the current rotation angle to the matrix. The center of rotation is set to be the center of the bitmap.
        mMatrix.postRotate(-this.mAngle, mBitmap.getHeight() / 2, mBitmap.getHeight() / 2);


        // Use the matrix to draw the bitmap image of the compass.
        canvas.drawBitmap(mBitmap, mMatrix, mPaint);

        super.onDraw(canvas);

    }

}