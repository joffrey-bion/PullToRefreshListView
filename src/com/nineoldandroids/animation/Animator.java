/*
 * Copyright (C) 2010 The Android Open Source Project Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or
 * agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nineoldandroids.animation;

import java.util.ArrayList;

import android.view.animation.Interpolator;

/**
 * This is the superclass for classes which provide basic support for animations
 * which can be started, ended, and have <code>AnimatorListeners</code> added to
 * them.
 */
@SuppressWarnings("javadoc")
public abstract class Animator implements Cloneable {

    /**
     * The set of listeners to be sent events through the life of an animation.
     */
    ArrayList<AnimatorListener> mListeners = null;

    /**
     * Starts this animation. If the animation has a nonzero startDelay, the
     * animation will start running after that delay elapses. A non-delayed animation
     * will have its initial value(s) set immediately, followed by calls to
     * {@link AnimatorListener#onAnimationStart(Animator)} for any listeners of this
     * animator.
     * 
     * <p>
     * The animation started by calling this method will be run on the thread that
     * called this method. This thread should have a Looper on it (a runtime
     * exception will be thrown if this is not the case). Also, if the animation will
     * animate properties of objects in the view hierarchy, then the calling thread
     * should be the UI thread for that view hierarchy.
     * </p>
     * 
     */
    public void start() {}

    /**
     * Cancels the animation. Unlike {@link #end()}, <code>cancel()</code> causes the
     * animation to stop in its tracks, sending an
     * {@link android.animation.Animator.AnimatorListener#onAnimationCancel(Animator)}
     * to its listeners, followed by an
     * {@link android.animation.Animator.AnimatorListener#onAnimationEnd(Animator)}
     * message.
     * 
     * <p>
     * This method must be called on the thread that is running the animation.
     * </p>
     */
    public void cancel() {}

    /**
     * Ends the animation. This causes the animation to assign the end value of the
     * property being animated, then calling the
     * {@link android.animation.Animator.AnimatorListener#onAnimationEnd(Animator)}
     * method on its listeners.
     * 
     * <p>
     * This method must be called on the thread that is running the animation.
     * </p>
     */
    public void end() {}
    /**
     * The amount of time, in milliseconds, to delay starting the animation after
     * {@link #start()} is called.
     * 
     * @param startDelay
     *            The amount of the delay, in milliseconds
     */
    public abstract void setStartDelay(long startDelay);

    /**
     * Sets the length of the animation.
     * 
     * @param duration
     *            The length of the animation, in milliseconds.
     */
    public abstract Animator setDuration(long duration);

    /**
     * The time interpolator used in calculating the elapsed fraction of this
     * animation. The interpolator determines whether the animation runs with linear
     * or non-linear motion, such as acceleration and deceleration. The default value
     * is {@link android.view.animation.AccelerateDecelerateInterpolator}
     * 
     * @param value
     *            the interpolator to be used by this animation
     */
    public abstract void setInterpolator(/* Time */Interpolator value);

    /**
     * Returns whether this Animator is currently running (having been started and
     * gone past any initial startDelay period and not yet ended).
     * 
     * @return Whether the Animator is running.
     */
    public abstract boolean isRunning();

    /**
     * Adds a listener to the set of listeners that are sent events through the life
     * of an animation, such as start, repeat, and end.
     * 
     * @param listener
     *            the listener to be added to the current set of listeners for this
     *            animation.
     */
    public void addListener(AnimatorListener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<AnimatorListener>();
        }
        mListeners.add(listener);
    }

    @Override
    public Animator clone() {
        try {
            final Animator anim = (Animator) super.clone();
            if (mListeners != null) {
                ArrayList<AnimatorListener> oldListeners = mListeners;
                anim.mListeners = new ArrayList<AnimatorListener>();
                int numListeners = oldListeners.size();
                for (int i = 0; i < numListeners; ++i) {
                    anim.mListeners.add(oldListeners.get(i));
                }
            }
            return anim;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
