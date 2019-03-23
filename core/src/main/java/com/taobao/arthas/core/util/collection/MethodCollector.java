package com.taobao.arthas.core.util.collection;

import com.taobao.arthas.core.util.matcher.CollectionMatcher;

import java.util.*;

public class MethodCollector {

    private final Map<String, List<String>> classMethodMap = new LinkedHashMap<String, List<String>>();

    public void addMethod(String className, String methodName) {
        List<String> methods = classMethodMap.get(className);
        if(methods == null){
            methods = new ArrayList<String>();
            classMethodMap.put(className, methods);
        }
        if(!methods.contains(methodName)) {
            methods.add(methodName);
        }
    }

    public boolean contains(String className, String methodName) {
        List<String> methods = classMethodMap.get(className);
        if(methods != null){
            return methods.contains(methodName);
        }
        return false;
    }

    public Collection<String> getClassNames(){
        return classMethodMap.keySet();
    }

    public CollectionMatcher getMethodMatcher(MethodCollector filteredCollector, Collection<String> referencedClassNames, boolean skipJdkClass) {
        List<String> methodNames = new ArrayList<String>(16);
        for (Map.Entry<String, List<String>> entry : classMethodMap.entrySet()) {
            String className = entry.getKey();
            if(skipJdkClass && className.startsWith("java/")){
                continue;
            }
            if(filteredCollector!=null) {
                for (String name : entry.getValue()) {
                    if(!filteredCollector.contains(className, name)){
                        methodNames.add(name);
                        referencedClassNames.add(className);
                    }
                }
            }else {
                referencedClassNames.add(className);
                methodNames.addAll(entry.getValue());
            }
        }
        return new CollectionMatcher(methodNames);
    }
}
