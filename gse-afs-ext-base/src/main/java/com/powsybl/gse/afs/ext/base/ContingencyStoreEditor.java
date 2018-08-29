/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.afs.ext.base;

import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.GeneratorContingency;
import com.powsybl.contingency.afs.ContingencyStore;
import com.powsybl.gse.spi.ProjectFileViewer;
import com.powsybl.gse.util.EquipmentInfo;
import com.powsybl.gse.util.Glyph;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ContingencyStoreEditor extends BorderPane implements ProjectFileViewer {

    private final ContingencyStore store;

    private final ListView<Contingency> contingencyList = new ListView<>();

    private final class ContingencyListCell extends ListCell<Contingency> {

        private HBox hbox = new HBox();
        private Label label = new Label("");
        private Pane pane = new Pane();
        private Button button = new Button("", Glyph.createAwesomeFont('\uf1f8').size("1.2em"));

        private ContingencyListCell() {
            button.setStyle("-fx-background-color: transparent;");
            hbox.getChildren().addAll(label, pane, button);
            hbox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(pane, Priority.ALWAYS);
            button.setOnAction(event -> removeContingency(getItem()));
        }

        @Override
        protected void updateItem(Contingency contingency, boolean empty) {
            super.updateItem(contingency, empty);
            setText(null);
            setGraphic(null);
            if (!empty) {
                label.setText(contingency.getId());
                setGraphic(hbox);
            }
        }
    }

    private void addContingency(Contingency contingency) {
        contingencyList.getItems().add(contingency);
        store.write(contingencyList.getItems());
    }

    private void removeContingency(Contingency contingency) {
        contingencyList.getItems().remove(contingency);
        store.write(contingencyList.getItems());
    }

    public ContingencyStoreEditor(ContingencyStore store) {
        this.store = Objects.requireNonNull(store);
        setCenter(contingencyList);

        contingencyList.setCellFactory(param -> new ContingencyListCell());
        contingencyList.setOnDragOver(event -> {
            if (event.getGestureSource() != contingencyList &&
                    event.getDragboard().hasContent(EquipmentInfo.DATA_FORMAT)) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        contingencyList.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasContent(EquipmentInfo.DATA_FORMAT)) {
                EquipmentInfo equipmentInfo = (EquipmentInfo) db.getContent(EquipmentInfo.DATA_FORMAT);
                ContingencyElement element = null;
                switch (equipmentInfo.getType()) {
                    case "LINE":
                    case "TWO_WINDINGS_TRANSFORMER":
                        element = new BranchContingency(equipmentInfo.getIdAndName().getId());
                        break;

                    case "GENERATOR":
                        element = new GeneratorContingency(equipmentInfo.getIdAndName().getId());
                        break;

                    default:
                        break;
                }
                if (element != null) {
                    addContingency(new Contingency(element.getId(), element));
                }
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    @Override
    public Node getContent() {
        return this;
    }

    @Override
    public void view() {
        contingencyList.getItems().setAll(store.read());
    }

    @Override
    public void dispose() {
        // nothing to dispose
    }

}
