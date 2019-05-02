/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.copypaste.afs.local;

import com.powsybl.afs.AbstractNodeBase;
import com.powsybl.afs.Node;
import com.powsybl.afs.ProjectNode;
import com.powsybl.gse.copypaste.afs.CopyService;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.io.File;
import java.nio.file.Paths;


/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public class LocalCopyService implements CopyService {

    @Override
    public void copy(AbstractNodeBase node) {
        String nodeId = node.getId();
        String nodeName = node.getName();
        String parentPath = LOCAL_DIR + nodeName + nodeId;

        File archiveParentFolder = new File(parentPath);
        if (!archiveParentFolder.exists()) {
            archiveParentFolder.mkdir();
        }
        File archiveFolder = new File(parentPath, nodeId);
        if (!archiveFolder.exists()) {
            if (node instanceof ProjectNode) {
                ((ProjectNode) node).archive(Paths.get(parentPath));
            } else if (node instanceof Node) {
                ((Node) node).archive(Paths.get(parentPath));
            }
        }

        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(parentPath + PATH_SEPARATOR + nodeId);
        clipboard.setContent(content);
    }
}
