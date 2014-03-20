package com.nineoldandroids.animation;

/**
 * This internal subclass is used for all types which are not int or float.
 */
class ObjectKeyframe extends Keyframe {

    /**
     * The value of the animation at the time mFraction.
     */
    Object mValue;

    ObjectKeyframe(float fraction, Object value) {
        mFraction = fraction;
        mValue = value;
        mHasValue = (value != null);
        mValueType = mHasValue ? value.getClass() : Object.class;
    }

    public Object getValue() {
        return mValue;
    }

    public void setValue(Object value) {
        mValue = value;
        mHasValue = (value != null);
    }

    @Override
    public ObjectKeyframe clone() {
        ObjectKeyframe kfClone = new ObjectKeyframe(getFraction(), mValue);
        kfClone.setInterpolator(getInterpolator());
        return kfClone;
    }
}