package com.powsybl.gse.util.editor;

import javafx.beans.value.ObservableValue;
import org.controlsfx.control.MasterDetailPane;

import java.util.List;

public abstract class AbstractCodeEditor extends MasterDetailPane {

    public abstract void setTabSize(int size);

    public abstract void setCode(String code);

    public abstract String getCode();

    public abstract ObservableValue<String> codeProperty();

    public abstract ObservableValue<Integer> caretPositionProperty();

    public abstract String currentPosition();

    public abstract void setCompletions(List<String> completions);
}
