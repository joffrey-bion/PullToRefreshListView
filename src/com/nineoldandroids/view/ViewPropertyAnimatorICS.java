package com.nineoldandroids.view;

import java.lang.ref.WeakReference;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import android.view.animation.Interpolator;
import com.nineoldandroids.animation.Animator.AnimatorListener;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class ViewPropertyAnimatorICS extends ViewPropertyAnimator {
	/**
	 * A value to be returned when the WeakReference holding the native
	 * implementation returns <code>null</code>
	 */
	private final static long RETURN_WHEN_NULL = -1L;

	/**
	 * A WeakReference holding the native implementation of ViewPropertyAnimator
	 */
	private final WeakReference<android.view.ViewPropertyAnimator> mNative;

	ViewPropertyAnimatorICS(View view) {
		mNative = new WeakReference<android.view.ViewPropertyAnimator>(view.animate());
	}

	@Override
	public ViewPropertyAnimator setDuration(long duration) {
		android.view.ViewPropertyAnimator n = mNative.get();
		if (n != null) {
			n.setDuration(duration);
		}
		return this;
	}

	@Override
	public long getDuration() {
		android.view.ViewPropertyAnimator n = mNative.get();
		if (n != null) {
			return n.getDuration();
		}
		return RETURN_WHEN_NULL;
	}

	@Override
	public long getStartDelay() {
		android.view.ViewPropertyAnimator n = mNative.get();
		if (n != null) {
			return n.getStartDelay();
		}
		return RETURN_WHEN_NULL;
	}

	@Override
	public ViewPropertyAnimator setListener(final AnimatorListener listener) {
		android.view.ViewPropertyAnimator n = mNative.get();
		if (n != null) {
			if (listener == null) {
				n.setListener(null);
			} else {
				n.setListener(new android.animation.Animator.AnimatorListener() {
					@Override
					public void onAnimationStart(android.animation.Animator animation) {
						listener.onAnimationStart(null);
					}

					@Override
					public void onAnimationRepeat(android.animation.Animator animation) {
						listener.onAnimationRepeat(null);
					}

					@Override
					public void onAnimationEnd(android.animation.Animator animation) {
						listener.onAnimationEnd(null);
					}

					@Override
					public void onAnimationCancel(android.animation.Animator animation) {
						listener.onAnimationCancel(null);
					}
				});
			}
		}
		return this;
	}

	@Override
	public ViewPropertyAnimator translationX(float value) {
		android.view.ViewPropertyAnimator n = mNative.get();
		if (n != null) {
			n.translationX(value);
		}
		return this;
	}

	@Override
	public ViewPropertyAnimator alpha(float value) {
		android.view.ViewPropertyAnimator n = mNative.get();
		if (n != null) {
			n.alpha(value);
		}
		return this;
	}

}
