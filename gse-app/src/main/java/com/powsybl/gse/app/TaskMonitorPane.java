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
import javafx.scene.layout.*;
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
        private final Button hideButton = new Button();
        private final Button closeButton = new Button();
        private final HBox actionBox = new HBox();
        private final Consumer<TaskItem> onCloseAction;
        private final Consumer<TaskItem> onHideAction;

        public TaskListCell(Consumer<TaskItem> onHide, Consumer<TaskItem> onClose) {
            content = createLayout();
            onCloseAction = onClose;
            onHideAction = onHide;
        }

        @Override
        protected void updateItem(TaskItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                LOGGER.info("Updating task item display for {}", item);
                label.setText(item.getName() + System.lineSeparator() + item.getMessage().getValue());
                label.setTextOverrun(OverrunStyle.ELLIPSIS);
                label.maxWidthProperty().bind(taskList.widthProperty().subtract(50.0));
                hideButton.setOnAction(event -> onHideAction.accept(item));
                closeButton.setOnAction(event -> onCloseAction.accept(item));
                if (item.isCancelable() && !actionBox.getChildren().contains(closeButton)) {
                    actionBox.getChildren().add(closeButton);
                } else if (!item.isCancelable()) {
                    actionBox.getChildren().remove(closeButton);
                }
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
            AnchorPane.setRightAnchor(vBox, 45.0);
            AnchorPane.setBottomAnchor(vBox, 0.0);

            actionBox.setPrefWidth(30.0);
            actionBox.setAlignment(Pos.TOP_CENTER);

            Glyph graphic = Glyph.createAwesomeFont('\uf068');
            graphic.color("-fx-mark-color");
            hideButton.setGraphic(graphic);
            hideButton.setPadding(new Insets(2, 5, 2, 5));
            hideButton.setOnMouseEntered(event -> graphic.color("#eee"));
            hideButton.setOnMouseExited(event -> graphic.color("-fx-mark-color"));
            hideButton.getStyleClass().add("transparent-button");
            hideButton.getStyleClass().add("hide-button");
            actionBox.getChildren().add(hideButton);

            Glyph closeGraphic = Glyph.createAwesomeFont('\uf00d');
            closeGraphic.color("-fx-mark-color");
            closeButton.setGraphic(closeGraphic);
            closeButton.setPadding(new Insets(2, 5, 2, 5));
            closeButton.setOnMouseEntered(event -> closeGraphic.color("#eee"));
            closeButton.setOnMouseExited(event -> closeGraphic.color("-fx-mark-color"));
            closeButton.getStyleClass().add("transparent-button");
            closeButton.getStyleClass().add("hide-button");
            actionBox.getChildren().add(closeButton);

            root.getChildren().add(actionBox);
            AnchorPane.setTopAnchor(actionBox, 0.0);
            AnchorPane.setRightAnchor(actionBox, 0.0);

            return root;
        }
    }

    private final ListView<TaskItem> taskList = new ListView<>();

    public TaskMonitorPane(TaskItemList items) {
        taskList.itemsProperty().bind(items.getDisplayItems());
        taskList.setPlaceholder(new Label(RESOURCE_BUNDLE.getString("NoTaskRunning")));
        setCenter(taskList);
        taskList.setCellFactory(param -> new TaskListCell(items::hideTask, items::cancelTask));
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
