package com.powsybl.gse.util.editor.impl.swing;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;

/**
 * Helper to bind event from another event system
 * @param <T> observable value type
 */
public abstract class ObservableValueHelper<T> extends ObservableValueBase<T> {

    void fireChange(){
        Platform.runLater(this::fireValueChangedEvent);
    }
}
