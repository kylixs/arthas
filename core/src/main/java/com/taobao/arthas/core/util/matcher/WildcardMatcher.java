package com.taobao.arthas.core.util.matcher;

/**
 * wildcard matcher
 * @author ralf0131 2017-01-06 13:17.
 */
public class WildcardMatcher implements Matcher<String>, MethodMatcher<String> {

    private final String pattern;


    public WildcardMatcher(String pattern) {
        this.pattern = pattern;
    }


    @Override
    public boolean matching(String className, String methodName) {
        //ignore className part, compatible with origin usage
        return this.matching(methodName);
    }

    @Override
    public boolean matching(String target) {
        return WildcardMatcherUtils.match(target, pattern, 0, 0);
    }

}
