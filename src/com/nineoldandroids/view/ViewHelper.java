package com.nineoldandroids.view;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import static com.nineoldandroids.view.animation.AnimatorProxy.wrap;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public final class ViewHelper {
	private static final boolean OLD_VERSION = Integer.valueOf(Build.VERSION.SDK_INT).intValue() < Build.VERSION_CODES.HONEYCOMB;
	
    private ViewHelper() {}

	public static void setAlpha(View view, float alpha) {
        if (OLD_VERSION) {
            wrap(view).setAlpha(alpha);
        } else {
        	view.setAlpha(alpha);
        }
    }


    public static void setTranslationX(View view, float translationX) {
        if (OLD_VERSION) {
            wrap(view).setTranslationX(translationX);
        } else {
        	view.setTranslationX(translationX);
        }
    }

    public static float getX(View view) {
        return OLD_VERSION ? wrap(view).getX() : view.getX();
    }

}
