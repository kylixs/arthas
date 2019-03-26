package com.taobao.arthas.core.util;

import com.alibaba.fastjson.JSON;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.util.matcher.MethodMatcher;
import com.taobao.arthas.core.util.matcher.MethodMatchers;
import com.taobao.arthas.core.util.matcher.WildcardMethodMatcher;
import com.taobao.arthas.core.util.reflect.FieldUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * options util methods
 * @author gongdewei 3/25/19 7:34 PM
 */
public class OptionsUtils {

    private static Map<String, Object> defaultOptionValues = getOptionValues();

    public static void saveOptions(File file) {
        OutputStream out = null;
        try {
            Map<String, Object> map = getOptionValues();
            String json = JSON.toJSONString(map, true);
            out = FileUtils.openOutputStream(file, false);
            out.write(json.getBytes("utf-8"));

            parseTraceStackOptions(GlobalOptions.traceStackPretty);
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
            setOptionValues(map);
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

    public static boolean parseTraceStackOptions(String str){
        //com.taobao.arthas.core.GlobalOptions.traceStackPretty
        //merge=true;decorate-proxy=true;min-cost=1ms;top-size=5
        Map<String, String> map = new HashMap<String, String>();
        String[] strings = str.split(";");
        for (String entry : strings) {
            String[] vals = entry.split("=");
            //min-cost => mincost
            if(vals.length > 1) {
                String key = vals[0].toLowerCase().replaceAll("-", "");
                map.put(key, vals[1]);
            }
        }

        Field[] fields = TraceStackOptions.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                String value = map.get(field.getName().toLowerCase());
                FieldUtils.setFieldValue(field, field.getType(), value);
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private static void setOptionValues(Map<String, Object> map) {
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

            parseTraceStackOptions(GlobalOptions.traceStackPretty);
        } catch (Exception e) {
        }
    }

    public static Map<String, Object> getOptionValues() {
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

    public static boolean resetOptionValue(String fieldName){
        try {
            Field field = GlobalOptions.class.getDeclaredField(fieldName);
            if(field != null) {
                field.setAccessible(true);
                Object value = defaultOptionValues.get(fieldName);
                if(value != null) {
                    field.set(null, value);
                    return true;
                }
                field.setAccessible(false);
            }
        } catch (Exception e) {
        }
        return false;
    }

    public static boolean resetAllOptionValues(){
        setOptionValues(defaultOptionValues);
        return true;
    }
}
