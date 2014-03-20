package com.nineoldandroids.animation;

/**
 * Internal subclass used when the keyframe value is of type int.
 */
class IntKeyframe extends Keyframe {

    /**
     * The value of the animation at the time mFraction.
     */
    private int mValue;

    IntKeyframe(float fraction, int value) {
        mFraction = fraction;
        mValue = value;
        mValueType = int.class;
        mHasValue = true;
    }

    IntKeyframe(float fraction) {
        mFraction = fraction;
        mValueType = int.class;
    }

    public int getIntValue() {
        return mValue;
    }

    public Object getValue() {
        return mValue;
    }

    public void setValue(Object value) {
        if (value != null && value.getClass() == Integer.class) {
            mValue = ((Integer)value).intValue();
            mHasValue = true;
        }
    }

    @Override
    public IntKeyframe clone() {
        IntKeyframe kfClone = new IntKeyframe(getFraction(), mValue);
        kfClone.setInterpolator(getInterpolator());
        return kfClone;
    }
}