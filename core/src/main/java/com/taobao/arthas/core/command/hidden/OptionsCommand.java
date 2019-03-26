package com.taobao.arthas.core.command.hidden;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.Option;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.OptionsUtils;
import com.taobao.arthas.core.util.matcher.EqualsMatcher;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.matcher.RegexMatcher;
import com.taobao.arthas.core.util.reflect.FieldUtils;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.text.Decoration;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

import static com.taobao.text.ui.Element.label;
import static java.lang.String.format;

/**
 * 选项开关命令
 *
 * @author vlinux on 15/6/6.
 */
@Name("options")
@Summary("View and change various Arthas options")
@Description(Constants.EXAMPLE +
        "options ls \n"+
        "options ls --details \n"+
        "options get unsafe\n" +
        "options set unsafe true\n" +
        "options reset unsafe\n" +
        "options reset --all\n" +
        Constants.WIKI + Constants.WIKI_HOME + "options")
public class OptionsCommand extends AnnotatedCommand {
    private String operate;
    private String optionName;
    private String optionValue;
    private boolean isDetails;
    private boolean isAll;

    @Argument(index = 0, argName = "operate", required = false)
    @Description("Operate: ls, list, get, set, reset")
    public void setOperate(String operate) {
        this.operate = operate;
    }

    @Argument(index = 1, argName = "options-name", required = false)
    @Description("Option name")
    public void setOptionName(String optionName) {
        this.optionName = optionName;
    }

    @Argument(index = 2, argName = "options-value", required = false)
    @Description("Option value")
    public void setOptionValue(String optionValue) {
        this.optionValue = optionValue;
    }

    @com.taobao.middleware.cli.annotations.Option(shortName = "D", longName = "details", flag = true)
    @Description("Show the details of options, include summary and description")
    public void setDetails(boolean b) {
        isDetails = b;
    }

    @com.taobao.middleware.cli.annotations.Option(shortName = "A", longName = "all", flag = true)
    @Description("Reset all options value to default.")
    public void setAll(boolean b) {
        isAll = b;
    }

    @Override
    public void process(CommandProcess process) {
        try {
            if (isShow()) {
                processShow(process);
            } else if (isShowName()) {
                processShowName(process);
            } else if(isSetName()) {
                processChangeNameValue(process);
            } else if(isResetAll()) {
                if(!StringUtils.isBlank(optionName)){
                    process.write(format("Reset all options value don't need specify option name. [%s]\n", optionName));
                    return;
                }
                processResetAllValue(process);
            } else if(isResetName()) {
                processResetNameValue(process);
            } else {
                process.write(format("Options [%s] arguments is invalid, see also the help 'options -h'.\n", operate));
            }
        } catch (Throwable t) {
            // ignore
        } finally {
            process.end();
        }
    }

    private void processShow(CommandProcess process) throws IllegalAccessException {
        Collection<Field> fields = findOptions(new RegexMatcher(".*"));
        process.write(RenderUtil.render(isDetails?drawShowDetailsTable(fields):drawShowTable(fields), process.width()));
    }

    private void processShowName(CommandProcess process) throws IllegalAccessException {
        Collection<Field> fields = findOptions(new EqualsMatcher<String>(optionName));
        if(fields.isEmpty()){
            process.write(format("options[%s] not found.\n", optionName));
        }else {
            if(isDetails){
                process.write(RenderUtil.render(drawShowDetailsTable(fields), process.width()));
            }else {
//                Field field = fields.iterator().next();
//                process.write(field.get(null) + "\n");
                process.write(RenderUtil.render(drawShowTable(fields), process.width()));
            }
        }
    }

    private void processChangeNameValue(CommandProcess process) throws IllegalAccessException {
        Field field = findFieldByOptionName();
        if (field == null) {
            process.write(format("options[%s] not found.\n", optionName));
            return;
        }
        Option optionAnnotation = field.getAnnotation(Option.class);
        Class<?> type = field.getType();
        Object beforeValue = FieldUtils.readStaticField(field);
        Object afterValue;

        try {
            if(optionName.equals("trace.stack-pretty")){
                if(!OptionsUtils.parseTraceStackOptions(optionValue)){
                    process.write(format("Options[%s] value[%s] is invalid. current value [%s].\n", optionName, optionValue, String.valueOf(beforeValue)));
                    return;
                }
            }
            afterValue = FieldUtils.setFieldValue(field, type, optionValue);
            if(afterValue == null){
                process.write(format("Options[%s] type[%s] not supported.\n", optionName, type.getSimpleName()));
                return;
            }

            saveOptions();
        } catch (Throwable t) {
            process.write(format("Cannot cast option value[%s] to type[%s].\n", optionValue, type.getSimpleName()));
            return;
        }

        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("NAME").style(Decoration.bold.bold()),
                label("BEFORE-VALUE").style(Decoration.bold.bold()),
                label("AFTER-VALUE").style(Decoration.bold.bold()));
        table.row(optionAnnotation.name(), StringUtils.objectToString(beforeValue),
                StringUtils.objectToString(afterValue));
        process.write(RenderUtil.render(table, process.width()));
    }

    private void saveOptions() {
        OptionsUtils.saveOptions(new File(com.taobao.arthas.core.util.Constants.OPTIONS_FILE));
    }

    private void processResetAllValue(CommandProcess process) throws IllegalAccessException {
        OptionsUtils.resetAllOptionValues();
        processShow(process);
        saveOptions();
    }

    private void processResetNameValue(CommandProcess process) throws IllegalAccessException {
        Field field = findFieldByOptionName();
        if (field == null) {
            process.write(format("options[%s] not found.\n", optionName));
            return;
        }
        boolean result = OptionsUtils.resetOptionValue(field.getName());
        if(!result){
            process.write(format("Reset option [%s] failed.\n", optionName));
        }else {
            processShowName(process);
        }
        saveOptions();
    }

    private Field findFieldByOptionName() {
        Collection<Field> fields = findOptions(new EqualsMatcher<String>(optionName));
        // name not exists
        if (fields.isEmpty()) {
            return null;
        }
        return fields.iterator().next();
    }

    /*
     * 判断当前动作是否需要展示整个options
     */
    private boolean isShow() {
        return "ls".equals(operate) || "list".equals(operate) || StringUtils.isBlank(operate);
    }

    /*
     * 判断当前动作是否需要展示某个Name的值
     */
    private boolean isShowName() {
        return "get".equals(operate) && !StringUtils.isBlank(optionName);
    }

    private boolean isSetName() {
        return "set".equals(operate) && !StringUtils.isBlank(optionName) && !StringUtils.isBlank(optionValue);
    }

    private boolean isReset() {
        return "reset".equals(operate);
    }

    private boolean isResetName() {
        return isReset() && !isAll && !StringUtils.isBlank(optionName);
    }

    private boolean isResetAll() {
        return isReset() && isAll;
    }

    private Collection<Field> findOptions(Matcher optionNameMatcher) {
        final Collection<Field> matchFields = new ArrayList<Field>();
        for (final Field optionField : FieldUtils.getAllFields(GlobalOptions.class)) {
            if (!optionField.isAnnotationPresent(Option.class)) {
                continue;
            }

            final Option optionAnnotation = optionField.getAnnotation(Option.class);
            if (optionAnnotation != null
                    && !optionNameMatcher.matching(optionAnnotation.name())) {
                continue;
            }
            matchFields.add(optionField);
        }
        return matchFields;
    }

    private Element drawShowTable(Collection<Field> optionFields) throws IllegalAccessException {
        TableElement table = new TableElement( 1, 1, 5)
                .leftCellPadding(1).rightCellPadding(1);
        table.row(true,
                label("TYPE").style(Decoration.bold.bold()),
                label("NAME").style(Decoration.bold.bold()),
                label("VALUE").style(Decoration.bold.bold()));

        for (final Field optionField : optionFields) {
            final Option optionAnnotation = optionField.getAnnotation(Option.class);
            table.row(optionField.getType().getSimpleName(),
                    optionAnnotation.name(),
                    "" + optionField.get(null));
        }
        return table;
    }
    private Element drawShowDetailsTable(Collection<Field> optionFields) throws IllegalAccessException {
        TableElement table = new TableElement(1, 1, 2, 1, 3, 6)
                .leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("LEVEL").style(Decoration.bold.bold()),
                label("TYPE").style(Decoration.bold.bold()),
                label("NAME").style(Decoration.bold.bold()),
                label("VALUE").style(Decoration.bold.bold()),
                label("SUMMARY").style(Decoration.bold.bold()),
                label("DESCRIPTION").style(Decoration.bold.bold()));

        for (final Field optionField : optionFields) {
            final Option optionAnnotation = optionField.getAnnotation(Option.class);
            table.row("" + optionAnnotation.level(),
                    optionField.getType().getSimpleName(),
                    optionAnnotation.name(),
                    "" + optionField.get(null),
                    optionAnnotation.summary(),
                    optionAnnotation.description());
        }
        return table;
    }
}
