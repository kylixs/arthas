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
        "options \n"+
        "options --details \n"+
        "options unsafe\n" +
        "options unsafe true\n" +
        "options unsafe true --save\n" +
        "options --save-all\n" +
        "options unsafe --load\n" +
        "options --load-all\n" +
        "options unsafe --reset\n" +
        "options --reset-all\n" +
        Constants.WIKI + Constants.WIKI_HOME + "options")
public class OptionsCommand extends AnnotatedCommand {
    private String optionName;
    private String optionValue;
    private boolean showDetails;
    private boolean bReset;
    private boolean bResetAll;
    private boolean bSave;
    private boolean bSaveAll;
    private boolean bLoad;
    private boolean bLoadAll;

    @Argument(index = 0, argName = "options-name", required = false)
    @Description("Option name")
    public void setOptionName(String optionName) {
        this.optionName = optionName;
    }

    @Argument(index = 1, argName = "options-value", required = false)
    @Description("Option value")
    public void setOptionValue(String optionValue) {
        this.optionValue = optionValue;
    }

    @com.taobao.middleware.cli.annotations.Option(shortName = "d", longName = "details", flag = true)
    @Description("Show the details of options, include summary and description")
    public void setShowDetails(boolean b) {
        showDetails = b;
    }

    @com.taobao.middleware.cli.annotations.Option(shortName = "r", longName = "reset", flag = true)
    @Description("Reset the option value to default.")
    public void setResetOption(boolean b) {
        bReset = b;
    }
    
    @com.taobao.middleware.cli.annotations.Option(shortName = "R", longName = "reset-all", flag = true)
    @Description("Reset all option values to default.")
    public void setResetAll(boolean b) {
        bResetAll = b;
    }

    @com.taobao.middleware.cli.annotations.Option(shortName = "s", longName = "save", flag = true)
    @Description("Save the option value to file.")
    public void setSaveOption(boolean b) {
        bSave = b;
    }

    @com.taobao.middleware.cli.annotations.Option(shortName = "S", longName = "save-all", flag = true)
    @Description("Save all option values to file.")
    public void setSaveAll(boolean b) {
        bSaveAll = b;
    }

    @com.taobao.middleware.cli.annotations.Option(shortName = "l", longName = "load", flag = true)
    @Description("Load the option value from file.")
    public void setLoadOption(boolean b) {
        bLoad = b;
    }

    @com.taobao.middleware.cli.annotations.Option(shortName = "L", longName = "load-all", flag = true)
    @Description("Load all option values from file.")
    public void setLoadAll(boolean b) {
        bLoadAll = b;
    }

    @Override
    public void process(CommandProcess process) {
        try {
            if(bSaveAll){
                saveAllOptions(process);
            }else if(bLoadAll){
                loadAllOptions(process);
            }else if(bResetAll){
                processResetAllValue(process);
            }else if(bLoad){
                loadOption(process);
            }else if(bReset){
                processResetNameValue(process);
            }else  if (isShow()) {
                processShow(process);
            } else if (isShowName()) {
                processShowName(process);
            } else if(isSetName()) {
                processChangeNameValue(process);
            } else {
                process.write("Options arguments is invalid, see also 'options -h'.\n");
            }
        } catch (Throwable t) {
            // ignore
        } finally {
            process.end();
        }
    }

    private void processShow(CommandProcess process) throws IllegalAccessException {
        if(bSave){
            process.write("Missing option name and value, see also 'options -h'.\n");
            return;
        }
        Collection<Field> fields = findOptions(new RegexMatcher(".*"));
        process.write(RenderUtil.render(showDetails ?drawShowDetailsTable(fields):drawShowTable(fields), process.width()));
    }

    private void processShowName(CommandProcess process) throws IllegalAccessException {
        if(bSave){
            process.write(format("Missing option value for [%s], see also 'options -h'.\n", optionName));
            return;
        }
        Collection<Field> fields = findOptions(new EqualsMatcher<String>(optionName));
        if(fields.isEmpty()){
            process.write(format("options[%s] not found.\n", optionName));
        }else {
            if(showDetails){
                process.write(RenderUtil.render(drawShowDetailsTable(fields), process.width()));
            }else {
//                Field field = fields.iterator().next();
//                process.write(field.get(null) + "\n");
                process.write(RenderUtil.render(drawShowTable(fields), process.width()));
            }
        }
    }

    private void processChangeNameValue(CommandProcess process) throws IllegalAccessException {
        Field field = findFieldByOptionName(optionName);
        if (field == null) {
            process.write(format("options[%s] not found.\n", optionName));
            return;
        }
        Option optionAnnotation = field.getAnnotation(Option.class);
        Class<?> type = field.getType();
        Object beforeValue = FieldUtils.readStaticField(field);
        Object afterValue;

        try {
            if(optionName.equals("trace-stack-pretty")){
                if(!OptionsUtils.parseTraceStackOptions(optionValue)) {
                    process.write(format("Options[%s] value[%s] is invalid. current value [%s].\n", optionName, optionValue, String.valueOf(beforeValue)));
                    return;
                }
                afterValue = GlobalOptions.traceStackPretty;
            }else {
                if(optionName.equals("trace-max-depth")){
                    //limit max depth of trace command
                    optionValue = Math.min(Integer.parseInt(optionValue), 20)+"";
                }
                afterValue = FieldUtils.setFieldValue(field, type, optionValue);
            }
            if(afterValue == null){
                process.write(format("Options[%s] type[%s] not supported.\n", optionName, type.getSimpleName()));
                return;
            }

            if(bSave){
                saveOption(process, optionName);
            }
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

    private void saveAllOptions(CommandProcess process) {
        if(!StringUtils.isBlank(optionName)){
            process.write(format("Save all options value don't need specify option name. [%s]\n", optionName));
            return;
        }
        OptionsUtils.saveAllOptions(new File(com.taobao.arthas.core.util.Constants.OPTIONS_FILE));
    }

    private void saveOption(CommandProcess process, String optionName) {
        String fieldName = findFieldNameByOptionName(optionName);
        if (fieldName == null) {
            process.write(format("options[%s] not found.\n", optionName));
            return;
        }
        OptionsUtils.saveOption(fieldName, new File(com.taobao.arthas.core.util.Constants.OPTIONS_FILE));
    }

    private void loadAllOptions(CommandProcess process) {
        if(!StringUtils.isBlank(optionName)){
            process.write(format("Load all options value don't need specify option name. [%s]\n", optionName));
            return;
        }
        OptionsUtils.loadAllOptions(new File(com.taobao.arthas.core.util.Constants.OPTIONS_FILE));
    }

    private void loadOption(CommandProcess process) {
        if(!StringUtils.isBlank(optionValue)){
            process.write(format("Load the option [%s] don't need its value [%s].\n", optionName, optionValue));
            return;
        }
        String fieldName = findFieldNameByOptionName(optionName);
        if (fieldName == null) {
            process.write(format("options[%s] not found.\n", optionName));
            return;
        }
        OptionsUtils.loadOption(fieldName, new File(com.taobao.arthas.core.util.Constants.OPTIONS_FILE));
    }

    private void processResetAllValue(CommandProcess process) throws IllegalAccessException {
        if(!StringUtils.isBlank(optionName)){
            process.write(format("Reset all options value don't need specify option name. [%s]\n", optionName));
            return;
        }
        OptionsUtils.resetAllOptionValues();
        processShow(process);
    }

    private void processResetNameValue(CommandProcess process) throws IllegalAccessException {
        if(!StringUtils.isBlank(optionValue)){
            process.write(format("Reset the option [%s] don't need its value [%s].\n", optionName, optionValue));
            return;
        }

        String fieldName = findFieldNameByOptionName(optionName);
        if (fieldName == null) {
            process.write(format("options[%s] not found.\n", optionName));
            return;
        }
        boolean result = OptionsUtils.resetOptionValue(fieldName);
        if(!result){
            process.write(format("Reset option [%s] failed.\n", optionName));
        }else {
            processShowName(process);
        }
    }

    private Field findFieldByOptionName(String optionName) {
        Collection<Field> fields = findOptions(new EqualsMatcher<String>(optionName));
        // name not exists
        if (fields.isEmpty()) {
            return null;
        }
        return fields.iterator().next();
    }

    private String findFieldNameByOptionName(String optionName) {
        Field field = findFieldByOptionName(optionName);
        if(field != null){
            return field.getName();
        }
        return null;
    }

    /*
     * 判断当前动作是否需要展示整个options
     */
    private boolean isShow() {
        return StringUtils.isBlank(optionName) && StringUtils.isBlank(optionValue);
    }

    /*
     * 判断当前动作是否需要展示某个Name的值
     */
    private boolean isShowName() {
        return !StringUtils.isBlank(optionName) && StringUtils.isBlank(optionValue);
    }

    private boolean isSetName() {
        return !StringUtils.isBlank(optionName) && !StringUtils.isBlank(optionValue);
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
