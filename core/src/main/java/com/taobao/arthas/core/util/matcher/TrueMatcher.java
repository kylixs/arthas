package com.taobao.arthas.core.util.matcher;

/**
 * @author ralf0131 2017-01-06 13:48.
 */
public final class TrueMatcher<T> implements Matcher<T>, MethodMatcher<T>{

    /**
     * always return true
     * @param target
     * @return
     */
    @Override
    public boolean matching(T target) {
        return true;
    }

    @Override
    public boolean matching(T className, T methodName) {
        //ignore className part, compatible with origin usage
        return this.matching(methodName);
    }
}