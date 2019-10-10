package com.powsybl.gse.util.editor;

import javafx.application.Platform;
import javafx.beans.value.ObservableValueBase;

/**
 * Helper to bind event from another event system
 * @param <T> observable value type
 */
public abstract class AbstractObservableValueHelper<T> extends ObservableValueBase<T> {

    public void fireChange() {
        Platform.runLater(this::fireValueChangedEvent);
    }
}
