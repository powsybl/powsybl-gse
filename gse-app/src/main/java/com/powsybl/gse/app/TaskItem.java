/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.app;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TaskItem {

    private final UUID id;

    private final String name;

    private final StringProperty message = new SimpleStringProperty("");

    private final BooleanProperty cancellable = new SimpleBooleanProperty(false);

    public TaskItem(UUID id, String name, String message) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.message.set(message);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public StringProperty getMessage() {
        return message;
    }

    public boolean getCancellable() {
        return cancellable.get();
    }

    public BooleanProperty cancellableProperty() {
        return cancellable;
    }

    @Override
    public String toString() {
        return name;
    }
}
