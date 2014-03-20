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

import android.animation.ObjectAnimator;

/**
 * This class holds information about a property and the values that that property
 * should take on during an animation. PropertyValuesHolder objects can be used to
 * create animations with ValueAnimator or ObjectAnimator that operate on several
 * different properties in parallel.
 */
@SuppressWarnings("rawtypes")
class PropertyValuesHolder implements Cloneable {

    /**
     * The name of the property associated with the values. This need not be a real
     * property, unless this object is being used with ObjectAnimator. But this is
     * the name by which aniamted values are looked up with getAnimatedValue(String)
     * in ValueAnimator.
     */
    private String mPropertyName;

    /**
     * The type of values supplied. This information is used both in deriving the
     * setter/getter functions and in deriving the type of TypeEvaluator.
     */
    private Class mValueType;

    /**
     * The set of keyframes (time/value pairs) that define this animation.
     */
    KeyframeSet mKeyframeSet = null;

    // type evaluators for the primitive types handled by this implementation
    private static final TypeEvaluator sIntEvaluator = new IntEvaluator();
    private static final TypeEvaluator sFloatEvaluator = new FloatEvaluator();

    /**
     * The type evaluator used to calculate the animated values. This evaluator is
     * determined automatically based on the type of the start/end objects passed
     * into the constructor, but the system only knows about the primitive types int
     * and float. Any other type will need to set the evaluator to a custom evaluator
     * for that type.
     */
    private TypeEvaluator mEvaluator;

    /**
     * The value most recently calculated by calculateValue(). This is set during
     * that function and might be retrieved later either by
     * ValueAnimator.animatedValue() or by the property-setting logic in
     * ObjectAnimator.animatedValue().
     */
    private Object mAnimatedValue;

    /**
     * Internal utility constructor, used by the factory methods to set the property
     * name.
     * 
     * @param propertyName
     *            The name of the property for this holder.
     */
    protected PropertyValuesHolder(String propertyName) {
        mPropertyName = propertyName;
    }

    /**
     * Constructs and returns a PropertyValuesHolder with a given property name and
     * set of int values.
     * 
     * @param propertyName
     *            The name of the property being animated.
     * @param values
     *            The values that the named property will animate between.
     * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
     */
    public static PropertyValuesHolder ofInt(String propertyName, int... values) {
        return new IntPropertyValuesHolder(propertyName, values);
    }

    /**
     * Constructs and returns a PropertyValuesHolder with a given property name and
     * set of float values.
     * 
     * @param propertyName
     *            The name of the property being animated.
     * @param values
     *            The values that the named property will animate between.
     * @return PropertyValuesHolder The constructed PropertyValuesHolder object.
     */
    public static PropertyValuesHolder ofFloat(String propertyName, float... values) {
        return new FloatPropertyValuesHolder(propertyName, values);
    }

    /**
     * Set the animated values for this object to this set of ints. If there is only
     * one value, it is assumed to be the end value of an animation, and an initial
     * value will be derived, if possible, by calling a getter function on the
     * object. Also, if any value is null, the value will be filled in when the
     * animation starts in the same way. This mechanism of automatically getting null
     * values only works if the PropertyValuesHolder object is used in conjunction
     * {@link ObjectAnimator}, and with a getter function derived automatically from
     * <code>propertyName</code>, since otherwise PropertyValuesHolder has no way of
     * determining what the value should be.
     * 
     * @param values
     *            One or more values that the animation will animate between.
     */
    public void setIntValues(int... values) {
        mValueType = int.class;
        mKeyframeSet = KeyframeSet.ofInt(values);
    }

    /**
     * Set the animated values for this object to this set of floats. If there is
     * only one value, it is assumed to be the end value of an animation, and an
     * initial value will be derived, if possible, by calling a getter function on
     * the object. Also, if any value is null, the value will be filled in when the
     * animation starts in the same way. This mechanism of automatically getting null
     * values only works if the PropertyValuesHolder object is used in conjunction
     * {@link ObjectAnimator}, and with a getter function derived automatically from
     * <code>propertyName</code>, since otherwise PropertyValuesHolder has no way of
     * determining what the value should be.
     * 
     * @param values
     *            One or more values that the animation will animate between.
     */
    public void setFloatValues(float... values) {
        mValueType = float.class;
        mKeyframeSet = KeyframeSet.ofFloat(values);
    }

    /**
     * Set the animated values for this object to this set of Keyframes.
     * 
     * @param values
     *            One or more values that the animation will animate between.
     */
    public void setKeyframes(Keyframe... values) {
        int numKeyframes = values.length;
        Keyframe keyframes[] = new Keyframe[Math.max(numKeyframes, 2)];
        mValueType = values[0].getType();
        for (int i = 0; i < numKeyframes; ++i) {
            keyframes[i] = values[i];
        }
        mKeyframeSet = new KeyframeSet(keyframes);
    }

    /**
     * Set the animated values for this object to this set of Objects. If there is
     * only one value, it is assumed to be the end value of an animation, and an
     * initial value will be derived, if possible, by calling a getter function on
     * the object. Also, if any value is null, the value will be filled in when the
     * animation starts in the same way. This mechanism of automatically getting null
     * values only works if the PropertyValuesHolder object is used in conjunction
     * {@link ObjectAnimator}, and with a getter function derived automatically from
     * <code>propertyName</code>, since otherwise PropertyValuesHolder has no way of
     * determining what the value should be.
     * 
     * @param values
     *            One or more values that the animation will animate between.
     */
    public void setObjectValues(Object... values) {
        mValueType = values[0].getClass();
        mKeyframeSet = KeyframeSet.ofObject(values);
    }

    @Override
    public PropertyValuesHolder clone() {
        try {
            PropertyValuesHolder newPVH = (PropertyValuesHolder) super.clone();
            newPVH.mPropertyName = mPropertyName;
            newPVH.mKeyframeSet = mKeyframeSet.clone();
            newPVH.mEvaluator = mEvaluator;
            return newPVH;
        } catch (CloneNotSupportedException e) {
            // won't reach here
            return null;
        }
    }

    /**
     * Internal function, called by ValueAnimator, to set up the TypeEvaluator that
     * will be used to calculate animated values.
     */
    void init() {
        if (mEvaluator == null) {
            // We already handle int and float automatically, but not their Object
            // equivalents
            mEvaluator = (mValueType == Integer.class) ? sIntEvaluator
                    : (mValueType == Float.class) ? sFloatEvaluator : null;
        }
        if (mEvaluator != null) {
            // KeyframeSet knows how to evaluate the common types - only give it a
            // custom
            // evaluator if one has been set on this class
            mKeyframeSet.setEvaluator(mEvaluator);
        }
    }

    /**
     * The TypeEvaluator will the automatically determined based on the type of
     * values supplied to PropertyValuesHolder. The evaluator can be manually set,
     * however, if so desired. This may be important in cases where either the type
     * of the values supplied do not match the way that they should be interpolated
     * between, or if the values are of a custom type or one not currently understood
     * by the animation system. Currently, only values of type float and int (and
     * their Object equivalents: Float and Integer) are correctly interpolated; all
     * other types require setting a TypeEvaluator.
     * 
     * @param evaluator
     */
    public void setEvaluator(TypeEvaluator evaluator) {
        mEvaluator = evaluator;
        mKeyframeSet.setEvaluator(evaluator);
    }

    /**
     * Function used to calculate the value according to the evaluator set up for
     * this PropertyValuesHolder object. This function is called by
     * ValueAnimator.animateValue().
     * 
     * @param fraction
     *            The elapsed, interpolated fraction of the animation.
     */
    void calculateValue(float fraction) {
        mAnimatedValue = mKeyframeSet.getValue(fraction);
    }

    /**
     * Sets the name of the property that will be animated. This name is used to
     * derive a setter function that will be called to set animated values. For
     * example, a property name of <code>foo</code> will result in a call to the
     * function <code>setFoo()</code> on the target object. If either
     * <code>valueFrom</code> or <code>valueTo</code> is null, then a getter function
     * will also be derived and called.
     * 
     * <p>
     * Note that the setter function derived from this property name must take the
     * same parameter type as the <code>valueFrom</code> and <code>valueTo</code>
     * properties, otherwise the call to the setter function will fail.
     * </p>
     * 
     * @param propertyName
     *            The name of the property being animated.
     */
    public void setPropertyName(String propertyName) {
        mPropertyName = propertyName;
    }

    /**
     * Gets the name of the property that will be animated. This name will be used to
     * derive a setter function that will be called to set animated values. For
     * example, a property name of <code>foo</code> will result in a call to the
     * function <code>setFoo()</code> on the target object. If either
     * <code>valueFrom</code> or <code>valueTo</code> is null, then a getter function
     * will also be derived and called.
     * 
     * @return bababebaebzebgzrbb
     */
    public String getPropertyName() {
        return mPropertyName;
    }

    /**
     * Internal function, called by ValueAnimator and ObjectAnimator, to retrieve the
     * value most recently calculated in calculateValue().
     * 
     * @return sgbgfdsnfsgngfsn
     */
    Object getAnimatedValue() {
        return mAnimatedValue;
    }

    @Override
    public String toString() {
        return mPropertyName + ": " + mKeyframeSet.toString();
    }

    // native static private int nGetIntMethod(Class targetClass, String methodName);
    // native static private int nGetFloatMethod(Class targetClass, String
    // methodName);
    // native static private void nCallIntMethod(Object target, int methodID, int
    // arg);
    // native static private void nCallFloatMethod(Object target, int methodID, float
    // arg);
}
