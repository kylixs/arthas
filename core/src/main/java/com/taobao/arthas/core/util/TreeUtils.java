package com.taobao.arthas.core.util;

/**
 * @author gongdewei 3/28/19 11:17 AM
 */
public class TreeUtils {

    public static String[] parseNodeData(String data){
        String[] methodNames = new String[2];
        int p1 = data.indexOf(":");
        if(p1 != -1){
            methodNames[0] = data.substring(0, p1);
            int p2 = data.indexOf("(", p1+1);
            if(p2 != -1){
                methodNames[1] = data.substring(p1+1, p2);
            }
        }
        return methodNames;
    }
}
