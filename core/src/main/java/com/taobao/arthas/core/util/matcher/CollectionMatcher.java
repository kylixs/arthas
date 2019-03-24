package com.taobao.arthas.core.util.matcher;

import java.util.Collection;
import java.util.HashSet;

public class CollectionMatcher implements Matcher<String>, MethodMatcher<String> {

    private Collection<String> values;

    public CollectionMatcher(Collection<String> values) {
        this.values = new HashSet<String>(values);
    }

    public CollectionMatcher() {
        values = new HashSet<String>();
    }

    @Override
    public boolean matching(String target) {
        return values.contains(target);
    }

    public int size(){
        return values.size();
    }

    @Override
    public boolean matching(String className, String methodName) {
        return matching(className+":"+methodName);
    }
}
