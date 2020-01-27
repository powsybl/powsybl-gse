package com.powsybl.gse.app;

import com.google.auto.service.AutoService;
import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ProjectFolder;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.spi.ProjectFileCreator;
import com.powsybl.gse.spi.ProjectFileCreatorExtension;
import com.powsybl.gse.util.Glyph;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.ResourceBundle;
import com.rte_france.imagrid.timeseries.store.afs.TimeSeriesStore;

@AutoService(ProjectFileCreatorExtension.class)
public class GseArchiveDialogExtension implements ProjectFileCreatorExtension {

        private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.GseMenuItem");

        @Override
        public Class<? extends ProjectFile> getProjectFileType() {
            return TimeSeriesStore.class;
        }

        @Override
        public Node getMenuGraphic() {
            return Glyph.createAwesomeFont('\uf0ed').size("1.1em");
        }

        @Override
        public String getMenuText() {
            return RESOURCE_BUNDLE.getString("Archive");
        }

        @Override
        public int getMenuOrder() {
            return 25;
        }

        @Override
        public KeyCodeCombination getMenuKeycode() {
            return new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
        }

        @Override
        public ProjectFileCreator newCreator(ProjectFolder folder, Scene scene, GseContext context) {
            return new GseArchiveDialog<ProjectFolder>(folder, scene);
        }
}

