package com.powsybl.gse.cop_past;

import com.powsybl.afs.ProjectNode;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.nio.file.Paths;

public class LocalCopyService implements CopyService {

    @Override
    public void copy(ProjectNode projectNode) {
        String idName = projectNode.getName() + projectNode.getId();
        String path = "/home/nassnamb/Documents/ArchiveFolder2/" + idName;

        java.io.File f = new java.io.File(path);
        if (!f.exists()) {
            f.mkdir();
        }
        java.io.File file = new java.io.File(path + "/" + projectNode.getId());
        if (!file.exists()) {
            projectNode.archive(Paths.get(path));
        }
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(path + "/" + projectNode.getId());
        clipboard.setContent(content);

    }
}
