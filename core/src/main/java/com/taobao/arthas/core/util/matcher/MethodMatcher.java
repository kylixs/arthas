package com.taobao.arthas.core.util.matcher;

/**
 * 类方法匹配器，目的是避免单一匹配方法名导致重复增强方法
 * Created by gongdewei on 19/3/24.
 */
public interface MethodMatcher<T> extends Matcher<T>{

    /**
     * match className only
     * @param target 目标字符串
     * @return
     */
    @Override
    boolean matching(T target);

    /**
     * 是否匹配类和方法
     */
    boolean matching(T className, T methodName);

//    boolean matchingClassName(T className);
}
