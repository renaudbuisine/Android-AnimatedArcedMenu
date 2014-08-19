package com.renaudbuisine.animatedarcedmenu.samples.solarsystem.activities.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.renaudbuisine.animatedarcedmenu.samples.solarsystem.R;
import com.renaudbuisine.animatedarcedmenu.views.ArcedMenuLayout;

public class PlanetsArcedMenuFragment extends Fragment {

	/**
	 * Link to the arced menu view and behaviour
	 */
    private ArcedMenuLayout mMenuLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

    	// load the content of the menu
        return inflater.inflate(R.layout.fragment_arced_menu,container,false);
    }

    /**
     * Set the last elements to make work the menu (NEED TO BE CALLED TO MAKE THE MENU WORK)
     * @param menuLayout the custom view of the menu
     */
    public void setUp(ArcedMenuLayout menuLayout){
    	//save 
        mMenuLayout = menuLayout;

        // set the container of the elements of the menu (regarding the fragment layout, the menu can be only on part of the main container)
        mMenuLayout.setMenuElementsContainer((ViewGroup)getView());
        // here, we allow the user to move the menu button
        mMenuLayout.enableDragAndDropMenu(true);
    }

    /**
     * Close the menu
     */
    public void closeMenu(){
        mMenuLayout.closeMenu();
    }

}
