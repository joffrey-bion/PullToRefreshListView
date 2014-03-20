package com.nineoldandroids.animation;

/**
 * <p>
 * An animation listener receives notifications from an animation. Notifications
 * indicate animation related events, such as the end or the repetition of the
 * animation.
 * </p>
 */
public interface AnimatorListener {
    /**
     * <p>
     * Notifies the start of the animation.
     * </p>
     * 
     * @param animation
     *            The started animation.
     */
    void onAnimationStart(Animator animation);

    /**
     * <p>
     * Notifies the end of the animation. This callback is not invoked for animations
     * with repeat count set to INFINITE.
     * </p>
     * 
     * @param animation
     *            The animation which reached its end.
     */
    void onAnimationEnd(Animator animation);

    /**
     * <p>
     * Notifies the cancellation of the animation. This callback is not invoked for
     * animations with repeat count set to INFINITE.
     * </p>
     * 
     * @param animation
     *            The animation which was canceled.
     */
    void onAnimationCancel(Animator animation);

    /**
     * <p>
     * Notifies the repetition of the animation.
     * </p>
     * 
     * @param animation
     *            The animation which was repeated.
     */
    void onAnimationRepeat(Animator animation);
}