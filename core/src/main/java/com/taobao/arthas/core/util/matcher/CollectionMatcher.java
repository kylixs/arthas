package com.taobao.arthas.core.util.matcher;

import java.util.Collection;
import java.util.HashSet;

public class CollectionMatcher implements Matcher<String>, MethodMatcher<String> {

    //full method names: x.y.z.MyClass:func1
    private Collection<String> fullMethodNames = new HashSet<String>();

    //normal class names: x.y.z.MyClass
    private Collection<String> classNames = new HashSet<String>();

    public CollectionMatcher(Collection<String> classNames, Collection<String> fullMethodNames) {
        for (String className : classNames) {
            this.classNames.add(toNormalClassName(className));
        }
        for (String fullMethodName : fullMethodNames) {
            this.fullMethodNames.add(toNormalClassName(fullMethodName));
        }
    }

    public CollectionMatcher() {
        fullMethodNames = new HashSet<String>();
    }

    @Override
    public boolean matching(String target) {
        return classNames.contains(target);
    }

    public boolean isEmpty(){
        return fullMethodNames==null && fullMethodNames.isEmpty();
    }

    public int size(){
        return fullMethodNames.size();
    }

    @Override
    public boolean matching(String className, String methodName) {
        return  fullMethodNames.contains(toNormalClassName(className+":"+methodName));
    }

    private String toNormalClassName(String className) {
        return className.replace('/','.');
    }
}
