package com.taobao.arthas.core.util.matcher;

import com.taobao.arthas.core.util.StringUtils;

/**
 * wildcard matcher for className:methodName
 * @author gongdewei 3/26/19 10:43 AM
 */
public class WildcardMethodMatcher implements MethodMatcher<String> {

    private final String classNamePattern;
    private final String methodNamePattern;

    public WildcardMethodMatcher(String classNamePattern, String methodNamePattern) {
        this.classNamePattern = toNormalClassName(classNamePattern);
        this.methodNamePattern = methodNamePattern;
    }

    @Override
    public boolean matching(String className, String methodName) {
        className = toNormalClassName(className);
        if(!WildcardMatcherUtils.match(className, classNamePattern,0,0)){
            return false;
        }
        if(StringUtils.isBlank(methodNamePattern) || StringUtils.isBlank(methodName) ||
                WildcardMatcherUtils.match(methodName, methodNamePattern, 0, 0)){
            return true;
        }
        return false;
    }

    @Override
    public boolean matching(String className) {
        //only match className
        return WildcardMatcherUtils.match(className, classNamePattern, 0, 0);
    }

    private String toNormalClassName(String className) {
        return className.replace('/','.');
    }
}
