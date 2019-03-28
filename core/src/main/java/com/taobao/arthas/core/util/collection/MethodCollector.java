package com.taobao.arthas.core.util.collection;

import com.taobao.arthas.core.util.matcher.CollectionMatcher;
import com.taobao.arthas.core.util.matcher.MethodMatcher;

import java.util.*;

public class MethodCollector {

    private final Map<String, List<String>> classMethodMap = new LinkedHashMap<String, List<String>>();

    public void addMethod(String className, String methodName) {
        className = toNormalClassName(className);
        methodName = toNormalClassName(methodName);
        List<String> methods = classMethodMap.get(className);
        if(methods == null){
            methods = new ArrayList<String>();
            classMethodMap.put(className, methods);
        }
        if(!methods.contains(methodName)) {
            methods.add(methodName);
        }
    }

    public void addMethods(String className, List<String> methodNames) {
        for (String methodName : methodNames) {
            this.addMethod(className, methodName);
        }
    }

    public void merge(MethodCollector target) {
        for (Map.Entry<String, List<String>> entry : target.classMethodMap.entrySet()) {
            this.addMethods(entry.getKey(), entry.getValue());
        }
    }

    public boolean contains(String className, String methodName) {
        className = toNormalClassName(className);
        methodName = toNormalClassName(methodName);
        List<String> methods = classMethodMap.get(className);
        if(methods != null){
            return methods.contains(methodName);
        }
        return false;
    }

    public void clear() {
        classMethodMap.clear();
    }

    public Collection<String> getClassNames(){
        return classMethodMap.keySet();
    }

    private boolean shouldSkipClass(boolean skipJdkClass, String className) {
        if (skipJdkClass && (className.startsWith("java.") && !className.equals("java.lang.reflect.InvocationHandler") )) {
            return true;
        }
        return false;
    }

    private String toNormalClassName(String className) {
        return className.replace('/','.');
    }

    public CollectionMatcher getMethodNameMatcher(MethodCollector filteredCollector, MethodMatcher<String> ignoreMethodsMatcher, boolean skipJdkClass) {
        Collection<String> classNames = new HashSet<String>(16);
        Collection<String> fullyMethodNames = new HashSet<String>(16);
        for (Map.Entry<String, List<String>> entry : classMethodMap.entrySet()) {
            String className = entry.getKey();
            if (shouldSkipClass(skipJdkClass, className)) continue;
            for (String methodName : entry.getValue()) {
                if((filteredCollector==null || !filteredCollector.contains(className, methodName)) &&
                        ( ignoreMethodsMatcher == null || !ignoreMethodsMatcher.matching(className, methodName))){
                    fullyMethodNames.add(className+":"+methodName);
                    classNames.add(className);
                }
            }
        }
        if(fullyMethodNames.isEmpty()){
            return null;
        }
        return new CollectionMatcher(classNames, fullyMethodNames);
    }
}
