package com.taobao.arthas.core.util;

import com.alibaba.fastjson.JSON;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.util.matcher.MethodMatcher;
import com.taobao.arthas.core.util.matcher.MethodMatchers;
import com.taobao.arthas.core.util.matcher.WildcardMethodMatcher;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * options util methods
 * @author gongdewei 3/25/19 7:34 PM
 */
public class OptionsUtils {

    public static void saveOptions(File file) {
        OutputStream out = null;
        try {
            Map<String, Object> map = getOptionsMap();
            String json = JSON.toJSONString(map, true);
            out = FileUtils.openOutputStream(file, false);
            out.write(json.getBytes("utf-8"));
        } catch (Exception e) {
            // ignore
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

    public static void loadOptions(File file){
        BufferedReader br = null;
        StringBuilder sbJson = new StringBuilder(128);
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));
            String line;
            while ((line = br.readLine()) != null) {
                sbJson.append(line.trim());
            }
            //convert json string to map
            Map<String, Object> map = JSON.parseObject(sbJson.toString());
            setOptions(map);
        } catch (Exception e) {
            // ignore
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

    /**
     * parse ignore method list, eg. "*StringUtils;*FileUtils;*FooClass:methodName;"
     * @param str
     * @return
     */
    public static MethodMatcher<String> parseIgnoreMethods(String str) {
        List<MethodMatcher<String>> matchers = new ArrayList<MethodMatcher<String>>();
        String[] classMethods = str.split(";");
        for (String classMethod : classMethods) {
            String classNamePattern = classMethod;
            String methodNamePattern = null;
            int p = classMethod.indexOf(":");
            if(p != -1){
                classNamePattern = classMethod.substring(0, p);
                methodNamePattern = classMethod.substring(p+1);
            }
            MethodMatcher<String> matcher = new WildcardMethodMatcher(classNamePattern, methodNamePattern);
            matchers.add(matcher);
        }
        return MethodMatchers.or(matchers);
    }

    private static void setOptions(Map<String, Object> map) {
        try {
            Field[] fields = GlobalOptions.class.getDeclaredFields();
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    Object value = map.get(field.getName());
                    if(value != null) {
                        field.set(null, value);
                    }
                } catch (Exception e) {
                }finally {
                    field.setAccessible(false);
                }
            }
        } catch (Exception e) {
        }
    }

    private static Map<String, Object> getOptionsMap() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        try {
            Field[] fields = GlobalOptions.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                map.put(field.getName(), field.get(null));
                field.setAccessible(false);
            }
        } catch (Exception e) {
        }
        return map;
    }

}
