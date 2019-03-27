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

    public static void saveAllOptions(File file) {
        Map<String, Object> map = getOptionValues();
        saveOptions(map, file);
    }

    public static boolean saveOption(String fieldName, File file) {
        Map<String, Object> currentValues = getOptionValues();
        Object fieldValue = currentValues.get(fieldName);
        if(fieldValue == null){
            return false;
        }

        //read origin option values from file, update this field only
        Map<String, Object> map = readOptions(file);
        map.put(fieldName, fieldValue);
        saveOptions(map, file);
        return true;
    }

    public static void loadAllOptions(File file){
        Map<String, Object> map = readOptions(file);
        setOptionValues(map);
    }

    public static void loadOption(String fieldName, File file) {
        Map<String, Object> map = readOptions(file);
        //only update this field value
        Map<String, Object> newMap = new HashMap<String, Object>();
        newMap.put(fieldName, map.get(fieldName));
        setOptionValues(newMap);
    }

    public static boolean resetOptionValue(String fieldName){
        Object value = defaultOptionValues.get(fieldName);
        if(value == null){
            return false;
        }
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(fieldName, value);
        setOptionValues(defaultOptionValues);
        return true;
    }

    public static boolean resetAllOptionValues(){
        setOptionValues(defaultOptionValues);
        return true;
    }

    private static void saveOptions(Map<String, Object> map, File file) {
        OutputStream out = null;
        try {
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

    private static Map<String, Object> readOptions(File file) {
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
            return map;
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
        return null;
    }

    private static void setOptionValues(Map<String, Object> map) {
        try {
            Field[] fields = GlobalOptions.class.getDeclaredFields();
            for (Field field : fields) {
                try {
                    Object value = map.get(field.getName());
                    if(value != null) {
                        field.setAccessible(true);
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
        return getStaticFieldValues(GlobalOptions.class);
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
        try {
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
                    if(value != null) {
                        FieldUtils.setFieldValue(field, field.getType(), value);
                    }
                } catch (Exception e) {
                    return false;
                }
            }
            return true;
        } finally {
            //refresh traceStackPretty
            String stackOptionValue=StringUtils.join(getStaticFieldValues(TraceStackOptions.class), ";");
            GlobalOptions.traceStackPretty = stackOptionValue;
        }
    }

    private static Map<String, Object> getStaticFieldValues(Class clazz) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        try {
            Field[] fields = clazz.getDeclaredFields();
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
