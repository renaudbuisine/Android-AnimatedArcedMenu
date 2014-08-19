package com.renaudbuisine.animatedarcedmenu.samples.solarsystem.helpers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * Created by renaudbuisine on 25/07/2014.
 */
public abstract class AssetHelper {

    private static final String ERROR_LOG = "ERROR_ASSETHELPER";

    private static final String DRAWABLE_ASSET_FOLDER = "drawable/";

    public static Drawable getAssetImage(Context context, String filename) {
        AssetManager assets = context.getResources().getAssets();

        InputStream buffer = null;
        try {
            buffer = new BufferedInputStream((assets.open(DRAWABLE_ASSET_FOLDER + filename)));
        } catch(IOException e){
            Log.e(ERROR_LOG,"getAssetImage :" + e.getMessage());
        }

        if(buffer == null){
            return null;
        }

        Bitmap bitmap = BitmapFactory.decodeStream(buffer);
        return new BitmapDrawable(context.getResources(), bitmap);
    }
}
