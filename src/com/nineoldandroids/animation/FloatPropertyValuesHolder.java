package com.nineoldandroids.animation;

class FloatPropertyValuesHolder extends PropertyValuesHolder {

    private FloatKeyframeSet mFloatKeyframeSet;
    private float mFloatAnimatedValue;

    FloatPropertyValuesHolder(String propertyName, float... values) {
        super(propertyName);
        setFloatValues(values);
    }

    @Override
    public void setFloatValues(float... values) {
        super.setFloatValues(values);
        mFloatKeyframeSet = (FloatKeyframeSet) mKeyframeSet;
    }

    @Override
    void calculateValue(float fraction) {
        mFloatAnimatedValue = mFloatKeyframeSet.getFloatValue(fraction);
    }

    @Override
    Object getAnimatedValue() {
        return mFloatAnimatedValue;
    }

    @Override
    public FloatPropertyValuesHolder clone() {
        FloatPropertyValuesHolder newPVH = (FloatPropertyValuesHolder) super.clone();
        newPVH.mFloatKeyframeSet = (FloatKeyframeSet) newPVH.mKeyframeSet;
        return newPVH;
    }

}