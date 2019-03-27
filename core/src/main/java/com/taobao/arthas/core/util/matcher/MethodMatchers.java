package com.taobao.arthas.core.util.matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Method Matcher utils
 * @author gongdewei 3/26/19 10:58 AM
 */
public class MethodMatchers {

    public static <T> MethodMatcher<T> and(Collection<MethodMatcher<T>> matchers){
        return new AndMethodMatcher(matchers);
    }
    public static <T> MethodMatcher<T> and(MethodMatcher<T>... matchers){
        return new AndMethodMatcher(Arrays.asList(matchers));
    }

    public static <T> MethodMatcher<T> or(Collection<MethodMatcher<T>> matchers){
        return new OrMethodMatcher(matchers);
    }
    public static <T> MethodMatcher<T> or(MethodMatcher<T>... matchers){
        return new OrMethodMatcher(Arrays.asList(matchers));
    }

    private static class OrMethodMatcher<T> implements MethodMatcher<T> {
        private List<MethodMatcher> matchers;

        public <T> OrMethodMatcher(Collection<MethodMatcher<T>> matchers) {
            this.matchers = new ArrayList<MethodMatcher>(matchers);
        }

        @Override
        public boolean matching(T className, T methodName) {
            for (int i = 0; i < matchers.size(); i++) {
                MethodMatcher matcher = matchers.get(i);
                if(matcher.matching(className, methodName)){
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean matching(T target) {
            for (int i = 0; i < matchers.size(); i++) {
                MethodMatcher matcher = matchers.get(i);
                if(matcher.matching(target)){
                    return true;
                }
            }
            return false;
        }
    }

    private static class AndMethodMatcher<T> implements MethodMatcher<T> {
        private List<MethodMatcher> matchers;

        public <T> AndMethodMatcher(Collection<MethodMatcher<T>> matchers) {
            this.matchers = new ArrayList<MethodMatcher>(matchers);
        }

        @Override
        public boolean matching(T className, T methodName) {
            for (int i = 0; i < matchers.size(); i++) {
                MethodMatcher matcher = matchers.get(i);
                if(!matcher.matching(className, methodName)){
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean matching(T target) {
            for (int i = 0; i < matchers.size(); i++) {
                MethodMatcher matcher = matchers.get(i);
                if(!matcher.matching(target)){
                    return false;
                }
            }
            return true;
        }
    }


}
