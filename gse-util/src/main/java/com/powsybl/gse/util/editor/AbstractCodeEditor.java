package com.powsybl.gse.util.editor;

import javafx.beans.value.ObservableValue;
import javafx.util.Pair;
import org.controlsfx.control.MasterDetailPane;

public abstract class AbstractCodeEditor extends MasterDetailPane {

    public abstract void setTabSize(int size);

    public abstract void setCode(String code);

    public abstract String getCode();

    public abstract ObservableValue<String> codeProperty();

    public abstract ObservableValue<Integer> caretPositionProperty();

    public abstract String currentPosition();

    public abstract void moveCaret(int newPosition);

    public abstract void replace(String text, int rangeStart, int rangeEnd);

    /**
     * Caret display position
     * @return x,y
     */
    public abstract Pair<Double, Double> caretDisplayPosition();
}
