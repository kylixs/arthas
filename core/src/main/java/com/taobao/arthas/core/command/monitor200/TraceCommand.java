package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.advisor.InvokeTraceable;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.OptionsUtils;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.arthas.core.util.collection.MethodCollector;
import com.taobao.arthas.core.util.matcher.*;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * 调用跟踪命令<br/>
 * 负责输出一个类中的所有方法调用路径
 *
 * @author vlinux on 15/5/27.
 */
@Name("trace")
@Summary("Trace the execution time of specified method invocation.")
@Description(value = Constants.EXPRESS_DESCRIPTION + Constants.EXAMPLE +
        "  trace org.apache.commons.lang.StringUtils isBlank\n" +
        "  trace *StringUtils isBlank\n" +
        "  trace *StringUtils isBlank params[0].length==1\n" +
        "  trace *StringUtils isBlank '#cost>100'\n" +
        "  trace -E org\\\\.apache\\\\.commons\\\\.lang\\\\.StringUtils isBlank\n" +
        "  trace -E com.test.ClassA|org.test.ClassB method1|method2|method3\n" +
        Constants.WIKI + Constants.WIKI_HOME + "trace")
public class TraceCommand extends EnhancerCommand {

    private String classPattern;
    private String methodPattern;
    private String conditionExpress;
    private boolean isRegEx = false;
    private int numberOfLimit = 100;
    private List<String> pathPatterns;
    private boolean skipJDKTrace;
    protected int traceDepth = 1;
    private MethodMatcher<String> additionalMethodMatcher;

    @Argument(argName = "class-pattern", index = 0)
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(argName = "method-pattern", index = 1)
    @Description("Method name pattern")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Argument(argName = "condition-express", index = 2, required = false)
    @Description(Constants.CONDITION_EXPRESS)
    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
    }

    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    @Option(shortName = "n", longName = "limits")
    @Description("Threshold of execution times")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    @Option(shortName = "p", longName = "path", acceptMultipleValues = true)
    @Description("path tracing pattern")
    public void setPathPatterns(List<String> pathPatterns) {
        this.pathPatterns = pathPatterns;
    }

    @Option(shortName = "j", longName = "jdkMethodSkip")
    @Description("skip jdk method trace")
    public void setSkipJDKTrace(boolean skipJDKTrace) {
        this.skipJDKTrace = skipJDKTrace;
    }

    @Option(shortName = "d", longName = "depth")
    @Description("set trace depth")
    public void setTraceDepth(int traceDepth) {
        if(traceDepth > 0 ) {
            this.traceDepth = Math.min(traceDepth, GlobalOptions.traceMaxDepth);
        }
    }

    @Option(shortName = "sp", longName = "stack-pretty")
    @Description("set trace stack pretty params")
    public void setTraceStackPretty(String prettyParams) {
        //Don't change global settings
        OptionsUtils.parseTraceStackOptions(prettyParams);
    }

    @Option(shortName = "ec", longName = "enhance-class")
    @Description("set additional enhance classes list. eg. 'x.y.z.Foo;x.x.MyClass:func1;'")
    public void setAdditionalEnhanceClasses(String classes) {
        additionalMethodMatcher = OptionsUtils.parseIgnoreMethods(classes);
    }

    public String getClassPattern() {
        return classPattern;
    }

    public String getMethodPattern() {
        return methodPattern;
    }

    public String getConditionExpress() {
        return conditionExpress;
    }

    public boolean isSkipJDKTrace() {
        return skipJDKTrace;
    }

    public boolean isRegEx() {
        return isRegEx;
    }

    public int getNumberOfLimit() {
        return numberOfLimit;
    }

    public List<String> getPathPatterns() {
        return pathPatterns;
    }

    @Override
    protected Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            if (pathPatterns == null || pathPatterns.isEmpty()) {
                classNameMatcher = SearchUtils.classNameMatcher(getClassPattern(), isRegEx());
            } else {
                classNameMatcher = getPathTracingClassMatcher();
            }
        }
        return classNameMatcher;
    }

    @Override
    protected MethodMatcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            if (pathPatterns == null || pathPatterns.isEmpty()) {
                methodNameMatcher = SearchUtils.classNameMatcher(getMethodPattern(), isRegEx());
            } else {
                methodNameMatcher = getPathTracingMethodMatcher();
            }
        }
        return methodNameMatcher;
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        if (pathPatterns == null || pathPatterns.isEmpty()) {
            return new TraceAdviceListener(this, process);
        } else {
            return new PathTraceAdviceListener(this, process);
        }
    }

    protected EnhancerAffect onEnhancerResult(CommandProcess process, int lock, Instrumentation inst, AdviceListener listener, boolean skipJDKTrace, EnhancerAffect effect) throws UnmodifiableClassException {
        MethodCollector globalEnhancedMethodCollector = new MethodCollector();
        MethodMatcher<String> ignoreMethodsMatcher = OptionsUtils.parseIgnoreMethods(GlobalOptions.traceIgnoredMethods);
        int depth = 1;
        int maxDepth = Math.min(this.traceDepth, 10);
        process.write(format("Erace level:%d, %s\n", depth, effect));
        while(++depth <= maxDepth){
            if (!enhanceMethods(process,lock, inst, listener, skipJDKTrace, effect, globalEnhancedMethodCollector, ignoreMethodsMatcher)) {
                break;
            }
            process.write(format("Trace level:%d, %s\n", depth, effect));
        }

        //enhance additional class methods
        if(additionalMethodMatcher!=null && !bExceedEnhanceMethodLimit){
            depth = 1;
            Enhancer.enhance(inst, lock, listener instanceof InvokeTraceable,
                    skipJDKTrace, additionalMethodMatcher, additionalMethodMatcher, effect);
            process.write(format("Trace additional enhance class methods:%d, %s\n", depth, effect));
            while(++depth <= maxDepth) {
                if (!enhanceMethods(process, lock, inst, listener, skipJDKTrace, effect, globalEnhancedMethodCollector, ignoreMethodsMatcher)) {
                    break;
                }
                process.write(format("Trace additional class methods level:%d, %s\n", depth, effect));
            }
        }

        return effect;
    }

    private boolean enhanceMethods(CommandProcess process, int lock, Instrumentation inst, AdviceListener listener, boolean skipJDKTrace, EnhancerAffect effect, MethodCollector globalEnhancedMethodCollector, MethodMatcher<String> ignoreMethodsMatcher) throws UnmodifiableClassException {
        MethodCollector enhancedMethodCollector = effect.getEnhancedMethodCollector();
        globalEnhancedMethodCollector.merge(enhancedMethodCollector);
        MethodCollector visitedMethodCollector = effect.getVisitedMethodCollector();
        CollectionMatcher newMethodNameMatcher = visitedMethodCollector.getMethodNameMatcher(globalEnhancedMethodCollector, ignoreMethodsMatcher, true);
        visitedMethodCollector.clear();
        if (newMethodNameMatcher == null){
            return false;
        }
        if(checkEnhanceMethodLimits(process, effect.mCnt() + newMethodNameMatcher.size())){
            return false;
        }

        Enhancer.enhance(inst, lock, listener instanceof InvokeTraceable,
                skipJDKTrace, newMethodNameMatcher, newMethodNameMatcher, effect);
        return true;
    }

    /**
     * 构造追踪路径匹配
     */
    private Matcher<String> getPathTracingClassMatcher() {

        List<Matcher<String>> matcherList = new ArrayList<Matcher<String>>();
        matcherList.add(SearchUtils.classNameMatcher(getClassPattern(), isRegEx()));

        if (null != getPathPatterns()) {
            for (String pathPattern : getPathPatterns()) {
                if (isRegEx()) {
                    matcherList.add(new RegexMatcher(pathPattern));
                } else {
                    matcherList.add(new WildcardMatcher(pathPattern));
                }
            }
        }

        return new GroupMatcher.Or<String>(matcherList);
    }

    private MethodMatcher getPathTracingMethodMatcher() {
        return new TrueMatcher<String>();
    }
}
