package com.renaudbuisine.animatedarcedmenu.callbacks;

/**
 * Created by renaudbuisine on 21/07/2014.
 */
public interface ArcedMenuCallback {
    void onMenuOpening();
    void onMenuOpened();
    void onMenuClosing();
    void onMenuClosed();
    void onMenuAnimationCancelled();
}
