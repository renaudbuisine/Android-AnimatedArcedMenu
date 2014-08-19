package com.renaudbuisine.animatedarcedmenu.views;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.renaudbuisine.animatedarcedmenu.R;
import com.renaudbuisine.animatedarcedmenu.callbacks.ArcedMenuCallback;

/**
 * Created by renaudbuisine on 17/07/2014.
 */
public class ArcedMenuLayout extends RelativeLayout{

    public abstract class ArcedMenuState{
        public static final int CLOSED = 0;
        public static final int CLOSING = 1;
        public static final int OPENING = 2;
        public static final int OPENED = 3;
    }
    private abstract class ArcedMenuButtonStyle{
        public static final int NONE = 0;
        public static final int BUTTON_ONLY = 1;
        public static final int ANIMATED_BUTTON = 2;
    }
    /**
     * Private interface to manage the end of animations
     */
    private interface ArcedMenuAnimationCallback{
        void onMenuAnimationCancelled();
        void onMenuAnimationFinished();
    }
    /**
     * Private class to manage arc displaying
     */
    private class ArcedMenuArcParameters{
        public double offset;
        public double amplitude;

        public ArcedMenuArcParameters(double offset, double amplitude){
            this.offset = offset;
            this.amplitude = amplitude;
        }

        @SuppressWarnings("unused")
		public ArcedMenuArcParameters(){
            amplitude = offset = 0;
        }
    }

    private static final String DEBUG_TAG = "ARCED_MENU_DEBUG";
    private static final String ERROR_TAG = "ARCED_MENU_ERROR";

    private static final float MENU_FADE_ALPHA = 0.6f;
    private static final float MENU_BUTTON_PRESSING_ALPHA = 0.7f;
    private static final int MENU_BUTTON_DRAG_VIBRATION_DURATION = 30;

    private ImageButton mImageButton;
    private View mFadeView;
    private ViewGroup mMenuElementsContainer;

    private int mMenuState = ArcedMenuState.CLOSED;

    private int mOpenedMenuRadius;
    private int mAnimationDuration;
    private int mMenuButtonGravity;
    private int[] mMenuButtonMargin;

    private int mChildrenCount;

    private List<int[]> mChildrenFinalPositions = new ArrayList<int[]>();
    private int[] mMenuCenter;

    private AnimatorSet.Builder mAnimationBuilder;
    private AnimatorSet mAnimation;

    private ArcedMenuCallback mCallback;

    public ArcedMenuLayout(Context context) {
        super(context);

        _initMenuView(context);
    }

    public ArcedMenuLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        _initMenuView(context);

        TypedArray a = null;
        android.content.res.Resources.Theme theme = context.getTheme();
        if(theme != null) {
            a = theme.obtainStyledAttributes(attrs,
                    R.styleable.ArcedMenuLayout, 0, 0);
        }

        // get attributes
        int resourceId = -1;
        int w = 0, h = 0;
        int style = 0;

        if(a != null) {
            try {
                resourceId = a.getResourceId(R.styleable.ArcedMenuLayout_src, -1);

                mMenuButtonMargin = new int[2];

                mMenuButtonGravity = a.getInteger(R.styleable.ArcedMenuLayout_menuGravity, 0);
                mMenuButtonMargin[0] = a.getDimensionPixelOffset(R.styleable.ArcedMenuLayout_menuButtonLeftMargin, 0);
                mMenuButtonMargin[1] = a.getDimensionPixelOffset(R.styleable.ArcedMenuLayout_menuButtonTopMargin, 0);
                w = a.getDimensionPixelOffset(R.styleable.ArcedMenuLayout_menuButtonWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
                h = a.getDimensionPixelOffset(R.styleable.ArcedMenuLayout_menuButtonHeight, ViewGroup.LayoutParams.WRAP_CONTENT);

                mOpenedMenuRadius = a.getDimensionPixelOffset(R.styleable.ArcedMenuLayout_openedMenuRadius, 0);

                mAnimationDuration = a.getInteger(R.styleable.ArcedMenuLayout_menuAnimationDuration, 300); // default at 0.3 second

                style = a.getInteger(R.styleable.ArcedMenuLayout_menuButtonStyle, ArcedMenuButtonStyle.NONE);

                a.recycle();
            } catch (Exception e) {
                Log.e(ERROR_TAG, e.getMessage());
            }
        }

        // Set the image in the button
        mImageButton.setImageDrawable(context.getResources().getDrawable(resourceId));
        LayoutParams params = (LayoutParams)mImageButton.getLayoutParams();

        // apply custom height and width if necessary
        params.height = h;
        params.width = w;

        mImageButton.setLayoutParams(params);

        //Button style
        switch(style){
            case ArcedMenuButtonStyle.ANIMATED_BUTTON :
                mImageButton.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));

                mImageButton.setOnTouchListener(new OnTouchListener() {
                    @SuppressLint("ClickableViewAccessibility")
					@Override
                    public boolean onTouch(View v, MotionEvent event) {
                        final int action = event.getAction();

                        switch (action) {
                            case MotionEvent.ACTION_DOWN:
                                mImageButton.setAlpha(MENU_BUTTON_PRESSING_ALPHA);
                                break;

                            case MotionEvent.ACTION_UP:
                                mImageButton.setAlpha(1.0f);
                                break;
                        }
                        return false;
                    }
                });
                break;

            case ArcedMenuButtonStyle.BUTTON_ONLY:
                mImageButton.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
                break;
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // add imageButtonView above other views
        this.addView(mImageButton,this.getChildCount());

        Log.d(DEBUG_TAG, "Menu frame initialized");
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if(changed){
            _setImageButtonPosition(r-l,b-t);
            mImageButton.setWillNotDraw(true);
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    /**
     * Indicate if the menu is closed or not
     * @return boolean
     */
    public boolean isMenuClosed(){
        return mMenuState == ArcedMenuState.CLOSED;
    }

    /**
     * Indicate if the menu is opened or not
     * @return boolean
     */
    public boolean isMenuOpened(){
        return mMenuState == ArcedMenuState.OPENED;
    }

    /**
     * Getter of the duration of the animation
     * @return int
     */
    public int getAnimationDuration() {
        return mAnimationDuration;
    }

    /**
     * Getter of the duration of the animation
     * @param animationDuration the duration of the animation in milliseconds
     */
    public void setAnimationDuration(int animationDuration) {
        this.mAnimationDuration = animationDuration;
    }

    /**
     * Setter fot eh callback object
     * @param callback ArcedMenuCallback
     */
    public void setCallback(ArcedMenuCallback callback){
        mCallback = callback;
    }

    /**
     * Setter of the container of the elements to display with the menu
     * @param menuElementsContainer viewgroup container
     */
    public void setMenuElementsContainer(ViewGroup menuElementsContainer){
        mMenuElementsContainer = menuElementsContainer;

        _setMenuElementsVisibility(INVISIBLE);

        Log.d(DEBUG_TAG, "Menu items count : " + String.valueOf(_getMenuElementsCount()));
    }

    /**
     * Setter of the image of the menu's button
     * @param drawable Drawable image
     */
    public void setMenuImage(Drawable drawable){
        mImageButton.setImageDrawable(drawable);
    }

    /**
     * Setter of the image of the menu's button
     * @param bm bitmap image
     */
    public void setMenuImage(Bitmap bm){
        mImageButton.setImageBitmap(bm);
    }

    /**
     * Getter of the menu button
     * @return ImageButton the button of the menu
     */
    public ImageButton getMenuButton(){
        return mImageButton;
    }

    /**
     * Launch the animation to open the menu and set the parameters of the opened menu
     */
    public void openMenu(){

        mMenuState = ArcedMenuState.OPENING;

        Log.d(DEBUG_TAG, "Menu Opening");

        if(mCallback != null){
            mCallback.onMenuOpening();
        }

        _setMenuElementsVisibility(VISIBLE);

        // get menu center
        boolean centerChanged = _saveMenuCenter();

        int nbrViews = _getMenuElementsCount();

        // position changed or there are other elements
        if(centerChanged || nbrViews != mChildrenCount){
            _calculateFinalPositions();
        }

        for(int i = 0; i < _getMenuElementsCount(); i ++){
            View v = mMenuElementsContainer.getChildAt(i);

            // init position
            _initViewForOpening(v);

            // center on the final position
            int[] finalPosition = mChildrenFinalPositions.get(i).clone();
            _getCenteredPosition(finalPosition,v);

            _pushViewAnimation(v,finalPosition,1.0f);
        }

        _animateFadeOpening();

        // launch the animation
        _launchAnimation(new OvershootInterpolator(), new ArcedMenuAnimationCallback() {
            @Override
            public void onMenuAnimationCancelled() {
                if (mCallback != null) {
                    mCallback.onMenuAnimationCancelled();
                }
            }

            @Override
            public void onMenuAnimationFinished() {
                mMenuState = ArcedMenuState.OPENED;

                Log.d(DEBUG_TAG, "Menu Opened");

                if (mCallback != null) {
                    mCallback.onMenuOpened();
                }
            }
        });
    }

    /**
     * Launch the animation to close the menu and set the parameters of the closed menu
     * Launch the animation to close the menu and set the parameters of the closed menu
     */
    public void closeMenu(){
        mMenuState = ArcedMenuState.CLOSING;

        Log.d(DEBUG_TAG, "Menu Closing");

        if(mCallback != null){
            mCallback.onMenuClosing();
        }

        for(int i = 0; i < _getMenuElementsCount(); i ++){
            View v = mMenuElementsContainer.getChildAt(i);

            int[] finalPosition = mMenuCenter.clone();
            _getCenteredPosition(finalPosition,v);

            _pushViewAnimation(v, finalPosition, 0.0f);
        }

        _animateFadeClosing();

        // launch the animation
        _launchAnimation(new AccelerateInterpolator(), new ArcedMenuAnimationCallback() {
            @Override
            public void onMenuAnimationCancelled() {

                if (mCallback != null) {
                    mCallback.onMenuAnimationCancelled();
                }
            }

            @Override
            public void onMenuAnimationFinished() {
                mMenuState = ArcedMenuState.CLOSED;
                _setMenuElementsVisibility(INVISIBLE);

                Log.d(DEBUG_TAG, "Menu Closed");

                if (mCallback != null) {
                    mCallback.onMenuClosed();
                }
            }
        });
    }

    /**
     * Active or remove drag abilities of the menu
     * @param enabled state of the ability
     */
    public void enableDragAndDropMenu(boolean enabled){
        if(enabled) {
            // add long press listener to detect the drag beginning
            mImageButton.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ClipData data = ClipData.newPlainText("", "");
                    DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                    v.startDrag(data, shadowBuilder, v, 0);
                    return false;
                }
            });

            this.setOnDragListener(new OnDragListener() {
                @Override
                public boolean onDrag(View v, DragEvent event) {
                    View view = (View) event.getLocalState();

                    switch (event.getAction()) {
                        case DragEvent.ACTION_DRAG_STARTED:
                            view.setVisibility(INVISIBLE);
                            // vibrate to indicate the begining of the movement
                            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                            // Vibrate for n milliseconds
                            vibrator.vibrate(MENU_BUTTON_DRAG_VIBRATION_DURATION);
                            break;
                        case DragEvent.ACTION_DROP:
                            float x = event.getX();
                            float y = event.getY();
                            mImageButton.setX(x - mImageButton.getWidth() / 2);
                            mImageButton.setY(y - mImageButton.getHeight() / 2);
                            mImageButton.setVisibility(View.VISIBLE);
                            // set alpha to 1 because the on touch animation
                            mImageButton.setAlpha(1.0f);
                            break;
                        default:
                            break;
                    }
                    return true;
                }
            });

        }
        else{
            mImageButton.setOnDragListener(null);
        }
    }

    /**
     * Initialize the menu view (create layout and button)
     * @param context Context
     */
    private void _initMenuView(Context context){

        mImageButton = new ImageButton(context);
        mImageButton.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        // define action on button click
        mImageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (mMenuState) {
                    case ArcedMenuState.CLOSED:
                        openMenu();
                        break;
                    case ArcedMenuState.OPENED:
                        closeMenu();
                        break;
                }
            }
        });

        mFadeView = new View(context);
        mFadeView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mFadeView.setBackgroundColor(context.getResources().getColor(android.R.color.background_dark));
    }

    /**
     * Set the poition of the menu button regarding the gravity provided
     * @param layoutWidth width of the container
     * @param layoutHeight height of the container
     */
    private void _setImageButtonPosition(int layoutWidth, int layoutHeight){

        int x = 0, y = 0;
        int buttonWidth = mImageButton.getWidth();
        int buttonHeight = mImageButton.getHeight();

        int verticalGravity = mMenuButtonGravity & 240; // 240 = 0b11110000
        int horizontalGravity = mMenuButtonGravity & 15; // 240 = 0b00001111

        if((verticalGravity & 16) != 0) { // & 0b00010000
            switch(verticalGravity){
                case Gravity.TOP :
                    y = 0;
                    break;
                case Gravity.BOTTOM :
                    y = layoutHeight - buttonHeight;
                    break;
                case Gravity.CENTER_VERTICAL :
                    y = (layoutHeight - buttonHeight) / 2;
                    break;
            }
        }
        if((horizontalGravity & 1) != 0){ // & 0b00000001
            switch(horizontalGravity){
                case Gravity.LEFT :
                    x = 0;
                    break;
                case Gravity.RIGHT :
                    x = layoutWidth - buttonWidth;
                    break;
                case Gravity.CENTER_HORIZONTAL :
                    x = (layoutWidth - buttonWidth) / 2;
            }
        }

        mImageButton.setX(x + mMenuButtonMargin[0]);
        mImageButton.setY(y + mMenuButtonMargin[1]);
    }

    /**
     * Count the number of element to display in the menu
     * @return int the number of elements of the menu
     */
    private int _getMenuElementsCount(){
        return mMenuElementsContainer.getChildCount();
    }

    /**
     * Set the visibility of the views of the menu
     * @param visibility the value to give to the attribute visibility of the views
     */
    private void _setMenuElementsVisibility(int visibility){
        mMenuElementsContainer.setVisibility(visibility);
    }

    /**
     * Get the center of the menu (center of the button),and precise if the position has changed
     * @return boolean
     */
    private boolean _saveMenuCenter(){

        int[] center = new int[2];
        int[] mainFramePosition = new int[2];

        // save position
        mImageButton.getLocationOnScreen(center);
        this.getLocationOnScreen(mainFramePosition);
        center[0] += mImageButton.getWidth() / 2 - mainFramePosition[0];
        center[1] += mImageButton.getHeight() / 2 - mainFramePosition[1];

        boolean hasChanged = mMenuCenter == null || center[0] != mMenuCenter[0] || center[1] != mMenuCenter[1];
        if(hasChanged){
            mMenuCenter = center;
        }

        return hasChanged;
    }

    /**
     * Initisalise the view before the animation
     * @param v the view to init for opening
     */
    private void _initViewForOpening(View v){
        v.setVisibility(VISIBLE);
        v.setScaleX(0);
        v.setScaleY(0);

        int[] position = mMenuCenter.clone();
        _getCenteredPosition(position, v);

        v.setX(position[0]);
        v.setY(position[1]);
    }

    /**
     * Animate the fade view for the menu opening
     */
    private void _animateFadeOpening(){
        this.addView(mFadeView);
        mMenuElementsContainer.bringToFront();
        mImageButton.bringToFront();

        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(mFadeView, View.ALPHA, 0.0f, MENU_FADE_ALPHA));

        set.setDuration(mAnimationDuration);
        set.setInterpolator(new LinearInterpolator());
        set.start();
    }

    /**
     * Animate the fade view for the menu closing
     */
    private void _animateFadeClosing(){

        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(mFadeView, View.ALPHA, mFadeView.getAlpha(), 0.0f));

        set.setDuration(mAnimationDuration);
        set.setInterpolator(new AccelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                _removeFade();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                _removeFade();
            }
        });
        set.start();
    }

    /**
     * Remove the fade view
     */
    private void _removeFade(){
        this.removeView(mFadeView);
    }

    /**
     * Prepare the animation by pushing element
     * @param v the v concerned by the pushed animation
     * @param finalPosition the position of the view at the end of the animation
     * @param finalScale the scale of the view at the end of the animation
     */
    private void _pushViewAnimation(View v, int[] finalPosition,float finalScale){

        // construct the animation
        if(mAnimation == null){
            mAnimation = new AnimatorSet();
            mAnimationBuilder = mAnimation.play(ObjectAnimator.ofFloat(v, View.X,
                    v.getX(), finalPosition[0]));

            if(mAnimationBuilder != null) {
                mAnimationBuilder.with(ObjectAnimator.ofFloat(v, View.Y,
                        v.getY(), finalPosition[1]))
                        .with(ObjectAnimator.ofFloat(v, View.SCALE_X,
                                v.getScaleX(), finalScale))
                        .with(ObjectAnimator.ofFloat(v, View.SCALE_Y,
                                v.getScaleY(), finalScale));
            }
        }else{ // push in animation
            mAnimationBuilder = mAnimationBuilder.with(ObjectAnimator.ofFloat(v, View.X,
                    v.getX(), finalPosition[0]))
                    .with(ObjectAnimator.ofFloat(v, View.Y,
                            v.getY(), finalPosition[1]))
                    .with(ObjectAnimator.ofFloat(v, View.SCALE_X,
                            v.getScaleX(), finalScale))
                    .with(ObjectAnimator.ofFloat(v, View.SCALE_Y,
                            v.getScaleY(), finalScale));
        }
    }

    /**
     * Launch the animation
     */
    private void _launchAnimation(TimeInterpolator interpolator, final ArcedMenuAnimationCallback callback){

        mAnimation.setDuration(mAnimationDuration);
        mAnimation.setInterpolator(interpolator);
        mAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimation = null;
                mAnimationBuilder = null;
                callback.onMenuAnimationFinished();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mAnimation = null;
                mAnimationBuilder = null;
                callback.onMenuAnimationCancelled();
            }
        });
        mAnimation.start();
    }

    /**
     * Calculate the final positions of the different element
     */
    private void _calculateFinalPositions(){

        mChildrenCount = _getMenuElementsCount();

        ArcedMenuArcParameters params = _findArcAngles();

        _calculateFinalPositionsInArc(params);
    }

    /**
     * Calculate the final positions of the different element regarding the arc amplitude and origin
     * @param params parameters for the arc (origin and amplitude)
     */
    private void _calculateFinalPositionsInArc(ArcedMenuArcParameters params){

        // clear previous positions
        mChildrenFinalPositions.clear();

        for(float i = 0; i < mChildrenCount ; i++){

            double angle = params.amplitude / mChildrenCount * (i + 0.5f) + params.offset;

            int[] position = new int[2];
            position[0] = (int)Math.round(Math.cos(angle) * mOpenedMenuRadius) + mMenuCenter[0];
            position[1] = (int)Math.round(- Math.sin(angle) * mOpenedMenuRadius) + mMenuCenter[1];

            mChildrenFinalPositions.add(position);
        }
    }

    private ArcedMenuArcParameters _findArcAngles(){

        //calculate radius regarding children's size
        int radius = mOpenedMenuRadius + _getChildrenPadding();

        double[] intersectionsL = _solveYCircleEquationInRectangle(mMenuCenter,radius,0);
        double[] intersectionsT = _solveXCircleEquationInRectangle(mMenuCenter, radius, 0);
        double[] intersectionsR = _solveYCircleEquationInRectangle(mMenuCenter,radius,this.getWidth());
        double[] intersectionsB = _solveXCircleEquationInRectangle(mMenuCenter, radius, this.getHeight());

        List<double[]> intersections = new ArrayList<double[]>();

        _buildIntersectionPoints(intersections,intersectionsL[0],0,true);
        _buildIntersectionPoints(intersections,intersectionsL[1],0,true);
        _buildIntersectionPoints(intersections,intersectionsR[0],this.getWidth(),true);
        _buildIntersectionPoints(intersections,intersectionsR[1],this.getWidth(),true);
        _buildIntersectionPoints(intersections,intersectionsT[0],0,false);
        _buildIntersectionPoints(intersections,intersectionsT[1],0,false);
        _buildIntersectionPoints(intersections,intersectionsB[0],this.getHeight(),false);
        _buildIntersectionPoints(intersections,intersectionsB[1],this.getHeight(),false);


        if(intersections.size() < 2){
            return new ArcedMenuArcParameters(- Math.PI/2,2 * Math.PI);
        }

        // search the closest corner from the menu
        /**
         * situation 0 : Corner to left top
         * situation 1 : Corner to left bottom
         * situation 2 : Corner to right top
         * situation 3 : Corner to right bottom
         */
        int situation = mMenuCenter[0] <= this.getWidth() / 2 ? 0 : 2;
        if(mMenuCenter[1] > this.getHeight() / 2){
            situation ++;
        }

        // we need just 2 points for our arc
        if(intersections.size() > 2){
            int refValues[] = new int[2];
            switch(situation){
                case 0 :
                    refValues[0] = 0;
                    refValues[1] = 0;
                    break;
                case 1 :
                    refValues[0] = 0;
                    refValues[1] = this.getHeight();
                    break;
                case 2 :
                    refValues[0] = this.getWidth();
                    refValues[1] = 0;
                    break;
                case 3 :
                    refValues[0] = this.getWidth();
                    refValues[1] = this.getHeight();
                    break;
            }

            double[] hPoint, vPoint;
            hPoint = vPoint = intersections.get(0);// default points

            double hDist = 0, vDist = 0;

            for(int i = 0; i < intersections.size(); i++){
                double[] p = intersections.get(i);

                // vertical test
                // is valid point ?
                boolean vValidPoint = situation % 2 == 0 && p[1] > mMenuCenter[1] || situation % 2 != 0 && p[1] < mMenuCenter[1];
                // is closer ?
                double vTempDist = Math.abs(refValues[1] - p[1]);
                if(vValidPoint && vDist < vTempDist){
                    vPoint = p;
                    vDist = vTempDist;
                    continue;
                }

                // horizontal test
                // is valid point ?
                boolean hValidPoint = situation < 2 && p[0] > mMenuCenter[0] || situation >= 2 && p[0] < mMenuCenter[0];
                // is closer ?
                double hTempDist = Math.abs(refValues[0] - p[0]);
                if(hValidPoint && hDist < hTempDist){
                    hPoint = p;
                    hDist = hTempDist;
                }
            }
            intersections.clear();
            intersections.add(hPoint);
            intersections.add(vPoint);

        }

        double[] pa = intersections.get(0), pb = intersections.get(1);
        // here we have our 2 points, find the angles now
        double a1, a2;
        a1 = _calculateAngleBetweenPointAndCenter(pa);
        a2 = _calculateAngleBetweenPointAndCenter(pb);


        // check if the middle of the arc is in the rectangle
        double testPointAngle = a1 + (a2 - a1)/2;
        // check angle value to turn in trigonometric sens
        if(testPointAngle < a1){
            testPointAngle += Math.PI;
        }
        double[] testPoint = _getPointFromAngleAndRadius(testPointAngle,radius);

        boolean reverseAngle = false;
        boolean sameHValue = pa[0] == pb[0];
        boolean sameVValue = pa[1] == pb[1];
        switch(situation){
            case 0:
                reverseAngle = testPoint[0] <= 0 && (testPoint[1] <= 0 || sameHValue) || (sameVValue && testPoint[1] <= 0);
                break;
            case 1:
                reverseAngle = testPoint[0] <= 0 && (testPoint[1] >= 0 || sameHValue) || (sameVValue && testPoint[1] >= 0);
                break;
            case 2:
                reverseAngle = testPoint[0] >= 0 && (testPoint[1] <= 0 || sameHValue) || (sameVValue && testPoint[1] <= 0);
                break;
            case 3:
                reverseAngle = testPoint[0] >= 0 && (testPoint[1] >= 0 || sameHValue) || (sameVValue && testPoint[1] >= 0);
                break;
        }

        if(reverseAngle){ // point outside
            double aMemo = a1;
            a1 = a2;
            a2 = aMemo;
        }

        double amplitude = a2 > a1 ? Math.abs(a2 - a1) : 2*Math.PI - a1 + a2;
        return new ArcedMenuArcParameters(a1,amplitude);
    }

    /**
     * Solve the equation between a vertical line and a circle
     * @param circleCenter center of the circle
     * @param radius radius of the circle
     * @param rectangleSideXPosition position x of the line
     * @return intersections points
     */
    private static double[] _solveYCircleEquationInRectangle(int[] circleCenter, int radius, int rectangleSideXPosition){
        double[] results = new double[2];

        double sq = Math.pow(radius,2) - Math.pow(rectangleSideXPosition - circleCenter[0],2);
        if(sq < 0){
            results[0] = -1;
            results[1] = -1;
            return results;
        }
        sq = Math.sqrt(sq);
        results[0] = sq + circleCenter[1];
        results[1] = -sq + circleCenter[1];
        return results;
    }

    /**
     * Solve the equation between a horizontal line and a circle
     * @param circleCenter center of the circle
     * @param radius radius of the circle
     * @param rectangleSideYPosition position y of the line
     * @return intersections points
     */
    private static double[] _solveXCircleEquationInRectangle(int[] circleCenter, int radius, int rectangleSideYPosition){
        double[] results = new double[2];

        double sq = Math.pow(radius,2) - Math.pow(rectangleSideYPosition - circleCenter[1],2);
        if(sq < 0){
            results[0] = -1;
            results[1] = -1;
            return results;
        }
        sq = Math.sqrt(sq);
        results[0] = sq + circleCenter[0];
        results[1] = -sq + circleCenter[0];
        return results;
    }

    /**
     * Build a point
     * @param points all points
     * @param intersection intersection value searched
     * @param secondCoordValue reference value for the previous search
     * @param isSecondCoordX precise if the reference value is x or y
     */
    private void _buildIntersectionPoints(List<double[]> points, double intersection, double secondCoordValue, boolean isSecondCoordX){
        if(intersection < 0){
            return;
        }

        if(isSecondCoordX){
            if(intersection > this.getHeight()){
                return;
            }

            double[] point = new double[2];
            point[0] = secondCoordValue;
            point[1] = intersection;
            points.add(point);
        }
        else{
            if(intersection > this.getWidth()){
                return;
            }

            double[] point = new double[2];
            point[1] = secondCoordValue;
            point[0] = intersection;
            points.add(point);
        }
    }

    private double _calculateAngleBetweenPointAndCenter(double[] p){

        // english 2D repere
        double diffY = mMenuCenter[1] - p[1];
        double diffX = p[0] - mMenuCenter[0];

        if(diffX == 0){
            return diffY > 0 ? Math.PI / 2 : -Math.PI / 2;
        }

        double rel = diffY / diffX;
        double a = Math.atan(rel);

        if(diffX < 0){
            a += Math.PI;
        }

        // only positif angles
        if(a < 0){
            a += Math.PI * 2;
        }

        return a;
    }

    private static double[] _getPointFromAngleAndRadius(double angle, double radius){
        double[] point = new double[2];
        point[0] = Math.cos(angle) * radius;
        point[1] = -Math.sin(angle) * radius;
        return point;
    }

    /**
     * Provide the padding of the children with the window's edges regarding the biggest view
     * @return padding with the window's edges
     */
    private int _getChildrenPadding(){
        int padding = 0;
        for(int i = 0; i < mChildrenCount ; i++){
            View v = mMenuElementsContainer.getChildAt(i);

            int biggestSide = v.getWidth() > v.getHeight() ? v.getWidth() : v.getHeight();

            if(biggestSide > padding){
                padding = biggestSide;
            }
        }

        return padding / 2;
    }

    /**
     * Returns the position of the view in order to make match the center of this view with the provided position
     * @param position to center
     * @param v reference view
     */
    private static void _getCenteredPosition(int[] position, View v){
        position[0] -= v.getWidth()/2;
        position[1] -= v.getHeight()/2;
    }
}