package com.nineoldandroids.animation;

class IntPropertyValuesHolder extends PropertyValuesHolder {

    private IntKeyframeSet mIntKeyframeSet;
    int mIntAnimatedValue;

    public IntPropertyValuesHolder(String propertyName, int... values) {
        super(propertyName);
        setIntValues(values);
    }

    @Override
    public void setIntValues(int... values) {
        super.setIntValues(values);
        mIntKeyframeSet = (IntKeyframeSet) mKeyframeSet;
    }

    @Override
    void calculateValue(float fraction) {
        mIntAnimatedValue = mIntKeyframeSet.getIntValue(fraction);
    }

    @Override
    Object getAnimatedValue() {
        return mIntAnimatedValue;
    }

    @Override
    public IntPropertyValuesHolder clone() {
        IntPropertyValuesHolder newPVH = (IntPropertyValuesHolder) super.clone();
        newPVH.mIntKeyframeSet = (IntKeyframeSet) newPVH.mKeyframeSet;
        return newPVH;
    }
}