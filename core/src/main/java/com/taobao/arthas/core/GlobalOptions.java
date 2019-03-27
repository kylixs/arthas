package com.taobao.arthas.core;

/**
 * 全局开关
 * Created by vlinux on 15/6/4.
 */
public class GlobalOptions {

    /**
     * 是否支持系统类<br/>
     * 这个开关打开之后将能代理到来自JVM的部分类，由于有非常强的安全风险可能会引起系统崩溃<br/>
     * 所以这个开关默认是关闭的，除非你非常了解你要做什么，否则请不要打开
     */
    @Option(level = 0,
            name = "unsafe",
            summary = "Option to support system-level class",
            description  =
                    "This option enables to proxy functionality of JVM classes."
                            +  " Due to serious security risk a JVM crash is possibly be introduced."
                            +  " Do not activate it unless you are able to manage."
    )
    public static volatile boolean isUnsafe = false;

    /**
     * 是否支持dump被增强的类<br/>
     * 这个开关打开这后，每次增强类的时候都将会将增强的类dump到文件中，以便于进行反编译分析
     */
    @Option(level = 1,
            name = "dump",
            summary = "Option to dump the enhanced classes",
            description =
                    "This option enables the enhanced classes to be dumped to external file " +
                            "for further de-compilation and analysis."
    )
    public static volatile boolean isDump = false;

    /**
     * 是否支持批量增强<br/>
     * 这个开关打开后，每次均是批量增强类
     */
    @Option(level = 1,
            name = "batch-re-transform",
            summary = "Option to support batch reTransform Class",
            description = "This options enables to reTransform classes with batch mode."
    )
    public static volatile boolean isBatchReTransform = true;

    /**
     * 是否支持json格式化输出<br/>
     * 这个开关打开后，使用json格式输出目标对象，配合-x参数使用
     */
    @Option(level = 2,
            name = "json-format",
            summary = "Option to support JSON format of object output",
            description = "This option enables to format object output with JSON when -x option selected."
    )
    public static volatile boolean isUsingJson = false;

    /**
     * 是否关闭子类
     */
    @Option(
            level = 1,
            name = "disable-sub-class",
            summary = "Option to control include sub class when class matching",
            description = "This option disable to include sub class when matching class."
    )
    public static volatile boolean isDisableSubClass = false;

    /**
     * 是否在asm中输出
     */
    @Option(level = 1,
            name = "debug-for-asm",
            summary = "Option to print DEBUG message if ASM is involved",
            description = "This option enables to print DEBUG message of ASM for each method invocation."
    )
    public static volatile boolean isDebugForAsm = false;

    /**
     * 是否日志中保存命令执行结果
     */
    @Option(level = 1,
            name = "save-result",
            summary = "Option to print command's result to log file",
            description = "This option enables to save each command's result to log file, " +
                    "which path is ${user.home}/logs/arthas-cache/result.log."
    )
    public static volatile boolean isSaveResult = false;

    /**
     * job的超时时间
     */
    @Option(level = 2,
            name = "job-timeout",
            summary = "Option to job timeout",
            description = "This option setting job timeout,The unit can be d, h, m, s for day, hour, minute, second. "
                    + "1d is one day in default"
    )
    public static volatile String jobTimeout = "1d";

    /**
     * trace方法深度
     */
    @Option(level = 2,
            name = "enhance-method-limits",
            summary = "Option to setting enhance method limits",
            description = "This option setting enhance method limits, use to prevent enhance large number of class methods, making Java process slowly."
    )
    public static volatile int enhanceMethodLimits = 100;

    /**
     * 是否美化Trace调用树（合并重复调用节点，动态Proxy类显示为接口类）
     */
    @Option(level = 2,
            name = "trace-stack-pretty",
            summary = "Option to prettify trace command output call stack",
            description = "This option enables to prettify trace command output call stack. " +
                    "Using a semicolon ';' to split multiple values, eg. merge-nodes=true;decorate-proxy=true;min-cost=1;top-size=5 .\n" +
                    "merge-nodes=true: merge two node of the same invoking. \n" +
                    "decorate-proxy=true: change the dynamic proxy classname to interface name. \n"+
                    "top-size=5: filter sub nodes by top size. \n"+
                    "min-cost=1: filter nodes less than min-cost value (ms). \n"+
                    "You can set partial attribute value by 'options trace-stack-pretty min-cost=0.5'. \n"
    )
    public static volatile String traceStackPretty = "merge-nodes=true;decorate-proxy=true;top-size=5;min-cost=1;";

    /**
     * trace方法深度
     */
    @Option(level = 2,
            name = "trace-max-depth",
            summary = "Option to trace method depth",
            description = "This option setting trace command max cascade method depth. The value range is [1-5]."
    )
    public static volatile int traceMaxDepth = 3;

    /**
     * 忽略增强的方法列表
     */
    @Option(level = 2,
            name = "trace-ignored-methods",
            summary = "Option to set ignore methods of all enhance command",
            description = "This option setting ignore methods of all enhance command (eg. trace, watch, monitor)." +
                    "Using a semicolon ';' to split multiple values. eg. *StringUtils;*FileUtils;*FooClass:methodName;"
    )
    public static volatile String traceIgnoredMethods = "*StringUtils;org.springframework.util.*;";

}
