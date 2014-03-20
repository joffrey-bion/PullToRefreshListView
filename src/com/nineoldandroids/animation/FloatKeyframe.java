package com.nineoldandroids.animation;

/**
 * Internal subclass used when the keyframe value is of type float.
 */
class FloatKeyframe extends Keyframe {
    /**
     * The value of the animation at the time mFraction.
     */
    private float mValue;

    FloatKeyframe(float fraction, float value) {
        mFraction = fraction;
        mValue = value;
        mValueType = float.class;
        mHasValue = true;
    }

    FloatKeyframe(float fraction) {
        mFraction = fraction;
        mValueType = float.class;
    }

    public float getFloatValue() {
        return mValue;
    }

    @Override
    public Object getValue() {
        return mValue;
    }

    @Override
    public void setValue(Object value) {
        if (value != null && value.getClass() == Float.class) {
            mValue = ((Float)value).floatValue();
            mHasValue = true;
        }
    }

    @Override
    public FloatKeyframe clone() {
        FloatKeyframe kfClone = new FloatKeyframe(getFraction(), mValue);
        kfClone.setInterpolator(getInterpolator());
        return kfClone;
    }
}