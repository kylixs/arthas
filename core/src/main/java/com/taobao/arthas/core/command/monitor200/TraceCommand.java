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
import com.taobao.arthas.core.view.TreeView;
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
    private AdviceListener listener;
    private boolean skipJDKTrace;
    protected int maxTraceDepth = 1;
    private int additionalTraceDepth = 1;
    //enhance step by step
    private boolean bStep;
    //当前trace次数
    private int traceStep;
    private MethodMatcher<String> additionalMethodMatcher;
    private CollectionMatcher firstLevelEnhanceMethodNameMatcher;
    private int lock;
    private Instrumentation inst;
    private EnhancerAffect effect;
    protected MethodCollector globalEnhancedMethodCollector = new MethodCollector();

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
    @Description("set trace depth, default is 1")
    public void setMaxTraceDepth(int maxTraceDepth) {
        if(maxTraceDepth > 0 ) {
            this.maxTraceDepth = Math.min(maxTraceDepth, GlobalOptions.traceMaxDepth);
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

    @Option(shortName = "ed", longName = "enhance-depth")
    @Description("set additional enhance classes trace depth, default is 1")
    public void setAdditionalTraceDepth(int traceDepth) {
        if(traceDepth > 0 ) {
            this.additionalTraceDepth = Math.min(traceDepth, GlobalOptions.traceMaxDepth);
        }
    }

    @Option(shortName = "s", longName = "step", flag = true)
    @Description("Auto trace new methods after a trace, step by step.")
    public void setStep(boolean b) {
        this.bStep = b;
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
        this.lock = lock;
        this.inst = inst;
        this.listener = listener;
        this.skipJDKTrace = skipJDKTrace;
        this.effect = effect;

        //get first enhance class-methods for stack displaying
        if(firstLevelEnhanceMethodNameMatcher == null) {
            MethodCollector enhancedMethodCollector = effect.getEnhancedMethodCollector();
            firstLevelEnhanceMethodNameMatcher = enhancedMethodCollector.getMethodNameMatcher(null, null, false);
        }
        globalEnhancedMethodCollector.merge(effect.getEnhancedMethodCollector());

        int depth = 1;
        if(maxTraceDepth > 1 || additionalTraceDepth > 1) {
            process.write(format("Max trace depth:%d, additional enhance depth:%d\n", maxTraceDepth, additionalTraceDepth));
            process.write(format("Trace level:%d, %s\n", depth, effect));
        }
        //如果不是逐步增强，则一次性增强多级
        while(!bStep && ++depth <= maxTraceDepth){
            if (!enhanceMethods(process,lock, inst, listener, skipJDKTrace, effect, globalEnhancedMethodCollector, ignoreMethodsMatcher)) {
                break;
            }
            process.write(format("Trace level:%d, %s\n", depth, effect));
        }

        //enhance additional class methods
        if(additionalMethodMatcher!=null && !bExceedEnhanceMethodLimit){
            depth = 1;
            Enhancer.enhance(inst, lock, listener instanceof InvokeTraceable,
                    skipJDKTrace, additionalMethodMatcher, additionalMethodMatcher, ignoreMethodsMatcher, effect);
            globalEnhancedMethodCollector.merge(effect.getEnhancedMethodCollector());
            process.write(format("Trace additional enhance class methods:%d, %s\n", depth, effect));
            //如果不是逐步增强，则一次性增强多级
            while(!bStep && ++depth <= additionalTraceDepth) {
                if (!enhanceMethods(process, lock, inst, listener, skipJDKTrace, effect, globalEnhancedMethodCollector, ignoreMethodsMatcher)) {
                    break;
                }
                process.write(format("Trace additional class methods level:%d, %s\n", depth, effect));
            }
        }
        if(bStep){
            process.write("Trace methods step by step.\n");
        }
        return effect;
    }

    private boolean enhanceMethods(CommandProcess process, int lock, Instrumentation inst, AdviceListener listener, boolean skipJDKTrace, EnhancerAffect effect, MethodCollector globalEnhancedMethodCollector, MethodMatcher<String> ignoreMethodsMatcher) throws UnmodifiableClassException {

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
                skipJDKTrace, newMethodNameMatcher, newMethodNameMatcher, ignoreMethodsMatcher, effect);

        MethodCollector enhancedMethodCollector = effect.getEnhancedMethodCollector();
        globalEnhancedMethodCollector.merge(enhancedMethodCollector);
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

    protected boolean isFirstLevelEnhanceMethod(String className, String methodName){
        return firstLevelEnhanceMethodNameMatcher!=null && firstLevelEnhanceMethodNameMatcher.matching(className, methodName);
    }

    public void onTraceResult(CommandProcess process, TreeView view) {
        try {
            MethodCollector methodCollector = view.getMethodCollector();

            //get MethodCollector before modify tree view
            view.pretty();
            process.write(view.draw() + "\n");

            if(!bStep){
                return ;
            }
            if(traceStep >= GlobalOptions.traceMaxDepth){
                process.write("Exceed max trace depth:"+traceStep+"\n");
                return;
            }
            traceStep++;

            CollectionMatcher newMethodNameMatcher = methodCollector.getMethodNameMatcher(globalEnhancedMethodCollector, ignoreMethodsMatcher, true);
            if(newMethodNameMatcher == null || newMethodNameMatcher.size() == 0){
                return;
            }
            process.write("Visited new methods: "+newMethodNameMatcher.size()+"\n");
            if(checkEnhanceMethodLimits(process, effect.mCnt() + newMethodNameMatcher.size())){
                return;
            }
            globalEnhancedMethodCollector.merge(methodCollector);
            effect.getVisitedMethodCollector().clear();
            Enhancer.enhance(inst, lock, listener instanceof InvokeTraceable,
                    skipJDKTrace, newMethodNameMatcher, newMethodNameMatcher, ignoreMethodsMatcher, effect);
            globalEnhancedMethodCollector.merge(effect.getEnhancedMethodCollector());

            process.write("Enhance total methods, "+effect+"\n" );
        } catch (UnmodifiableClassException e) {
            process.write("Enhance new methods failed: "+e.toString());
            e.printStackTrace();
        }
    }
}
