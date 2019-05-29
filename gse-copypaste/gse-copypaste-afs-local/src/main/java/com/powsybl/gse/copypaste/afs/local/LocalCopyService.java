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
import com.powsybl.gse.copypaste.afs.CopyServiceConstants;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;


/**
 * @author Nassirou Nambiema <nassirou.nambiena at rte-france.com>
 */
public class LocalCopyService implements CopyService {

    @Override
    public void copy(List<? extends AbstractNodeBase> nodes) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();

    /*    if (nodes.size() == 1) {
            AbstractNodeBase node = nodes.get(0);
            String nodeId = node.getId();
            String archiveDirectory = nodeArchiveDirectory(node);
            archiveNode(node, nodeId, archiveDirectory);
            content.putString(archiveDirectory + CopyServiceConstants.PATH_SEPARATOR + nodeId);
        } else {*/
            StringBuilder localPaths = new StringBuilder();
            for (AbstractNodeBase node : nodes) {
                String nodeId = node.getId();
                String archiveDirectory = nodeArchiveDirectory(node);
                archiveNode(node, nodeId, archiveDirectory);
                localPaths.append(archiveDirectory).append(CopyServiceConstants.PATH_SEPARATOR).append(nodeId).append(CopyServiceConstants.PATH_LIST_SEPARATOR);
            }
            content.putString(localPaths.toString());
      //  }
        clipboard.setContent(content);
    }

    private static void archiveNode(AbstractNodeBase node, String nodeId, String parentPath) {
        File archiveRootFolder = new File(parentPath);
        if (!archiveRootFolder.exists()) {
            archiveRootFolder.mkdir();
        }
        File archiveDestinationFolder = new File(parentPath, nodeId);
        if (!archiveDestinationFolder.exists()) {
            if (node instanceof ProjectNode) {
                ((ProjectNode) node).archive(Paths.get(parentPath));
            } else if (node instanceof Node) {
                ((Node) node).archive(Paths.get(parentPath));
            }
        }
    }

    private static String nodeArchiveDirectory(AbstractNodeBase node) {
        return CopyServiceConstants.LOCAL_DIR + node.getName() + node.getId();
    }
}
