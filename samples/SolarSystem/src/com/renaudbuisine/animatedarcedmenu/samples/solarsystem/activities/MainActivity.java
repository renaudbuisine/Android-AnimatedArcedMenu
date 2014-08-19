package com.renaudbuisine.animatedarcedmenu.samples.solarsystem.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import com.renaudbuisine.animatedarcedmenu.samples.solarsystem.R;
import com.renaudbuisine.animatedarcedmenu.samples.solarsystem.activities.fragments.PlanetsArcedMenuFragment;
import com.renaudbuisine.animatedarcedmenu.samples.solarsystem.helpers.AssetHelper;
import com.renaudbuisine.animatedarcedmenu.views.ArcedMenuLayout;

public class MainActivity extends Activity {

    private PlanetsArcedMenuFragment mArcedMenuFragment;

    private ImageView mBackground;
    private ImageView mInvisibleBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View contentView = getLayoutInflater().inflate(R.layout.activity_main,null,true);
        setContentView(contentView);

        /**
         * Application behaviour
         */
        mBackground = (ImageView)contentView.findViewById(R.id.imageView_background);
        mInvisibleBackground = (ImageView)contentView.findViewById(R.id.imageView_invisible_background);

        mBackground.setImageDrawable(AssetHelper.getAssetImage(this,"earth.jpg"));

        /**
         * FOR THE ARCED MENU
         */
        // get fragment of the menu
        mArcedMenuFragment = (PlanetsArcedMenuFragment)
                getFragmentManager().findFragmentById(R.id.arced_menu);
        // Set up the menu with the current view (container of the menu)
        mArcedMenuFragment.setUp((ArcedMenuLayout)contentView);
    }

    /**
     * Called after the click on arced menu's item, define in the fragment layout (event defined in layout)
     * @param view th view clicked
     */
    public void onMenuItemClick(final View view)
    {
        switch(view.getId()){
            case R.id.arced_menu_item_mercur :
                _switchImage("mercure.jpg");
                break;
            case R.id.arced_menu_item_venus :
                _switchImage("venus.jpg");
                break;
            case R.id.arced_menu_item_earth :
                _switchImage("earth.jpg");
                break;
            case R.id.arced_menu_item_mars :
                _switchImage("mars.jpg");
                break;
            case R.id.arced_menu_item_jupiter :
                _switchImage("jupiter.jpg");
                break;
            case R.id.arced_menu_item_saturn :
                _switchImage("saturn.jpg");
                break;
            case R.id.arced_menu_item_neptune :
                _switchImage("neptune.jpg");
                break;
            case R.id.arced_menu_item_uranus :
                _switchImage("uranus.jpg");
                break;
        }

        mArcedMenuFragment.closeMenu();
    }

    /**
     * Change the pictures
     */
    private void _switchImage(String fileName){
        mInvisibleBackground.setImageDrawable(mBackground.getDrawable());
        mInvisibleBackground.setVisibility(View.VISIBLE);

        mBackground.setImageDrawable(AssetHelper.getAssetImage(this,fileName));

        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(mInvisibleBackground, View.ALPHA,1.0f,0.0f));
        set.setDuration(1000);
        set.setInterpolator(new LinearInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mInvisibleBackground.setImageDrawable(null);
                mInvisibleBackground.setVisibility(View.GONE);
            }
        });

        set.start();
    }

}
