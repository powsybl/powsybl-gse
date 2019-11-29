/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.app;

import com.powsybl.gse.util.Glyph;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class TaskMonitorPane extends BorderPane {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.TaskMonitorPane");
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskMonitorPane.class);

    private class TaskListCell extends ListCell<TaskItem> {

        private final Label label = new Label("");
        private final Pane content;
        private final Button closeButton = new Button();
        private final Consumer<TaskItem> onCloseAction;

        public TaskListCell(Consumer<TaskItem> onClose) {
            content = createLayout();
            onCloseAction = onClose;
        }

        @Override
        protected void updateItem(TaskItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                label.setText(item.getName() + System.lineSeparator() + item.getMessage().getValue());
                label.setTextOverrun(OverrunStyle.ELLIPSIS);
                label.maxWidthProperty().bind(taskList.widthProperty().subtract(50.0));
                closeButton.setOnAction(event -> onCloseAction.accept(item));
                setGraphic(content);
            }
        }

        private AnchorPane createLayout() {
            AnchorPane root = new AnchorPane();

            ProgressBar progressBar = new ProgressBar();
            progressBar.setMaxWidth(Double.MAX_VALUE);
            VBox vBox = new VBox(label, progressBar);
            root.getChildren().add(vBox);
            AnchorPane.setLeftAnchor(vBox, 0.0);
            AnchorPane.setTopAnchor(vBox, 0.0);
            AnchorPane.setRightAnchor(vBox, 30.0);
            AnchorPane.setBottomAnchor(vBox, 0.0);

            VBox box = new VBox();
            box.setPrefWidth(15.0);
            box.setAlignment(Pos.TOP_CENTER);
            Glyph graphic = Glyph.createAwesomeFont('\uf068');
            graphic.color("-fx-mark-color");
            closeButton.setGraphic(graphic);
            closeButton.setPadding(new Insets(2, 5, 2, 5));
            closeButton.setOnMouseEntered(event -> graphic.color("#eee"));
            closeButton.setOnMouseExited(event -> graphic.color("-fx-mark-color"));
            closeButton.getStyleClass().add("transparent-button");
            closeButton.getStyleClass().add("hide-button");
            box.getChildren().add(closeButton);
            root.getChildren().add(box);
            AnchorPane.setTopAnchor(box, 0.0);
            AnchorPane.setRightAnchor(box, 0.0);

            return root;
        }
    }

    private final ListView<TaskItem> taskList = new ListView<>();

    public TaskMonitorPane(TaskItemList items) {
        taskList.itemsProperty().bind(items.getDisplayItems());
        taskList.setPlaceholder(new Label(RESOURCE_BUNDLE.getString("NoTaskRunning")));
        setCenter(taskList);
        taskList.setCellFactory(param -> new TaskListCell(items::hideTask));
        renderHiddenItemsHint(items);
        items.hiddenTaskCountProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                renderHiddenItemsHint(items);
            }
        });
    }

    private void renderHiddenItemsHint(TaskItemList items) {
        Platform.runLater(() -> {
            if (items.hiddenTaskCountProperty().get() > 0) {
                Text text = new Text(String.format(RESOURCE_BUNDLE.getString("HiddenTaskHint"), items.hiddenTaskCountProperty().get()));
                text.setOnMouseEntered(event -> text.setUnderline(true));
                text.setOnMouseExited(event -> text.setUnderline(false));
                text.setCursor(Cursor.HAND);
                text.setOnMouseClicked(event -> items.showAll());
                setBottom(text);
            } else {
                setBottom(null);
            }
        });
    }

}
