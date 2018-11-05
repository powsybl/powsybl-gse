/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.util;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.gse.spi.Savable;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.paint.Color;
import org.controlsfx.control.CheckListView;
import org.controlsfx.dialog.ExceptionDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class GseUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(GseUtil.class);

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang/GseUtil");

    private GseUtil() {
    }

    public static void setWaitingText(Labeled labeled) {
        labeled.setText(RESOURCE_BUNDLE.getString("Waiting") + "...");
        labeled.setTextFill(Color.GREY);
    }

    private static <T> void updateItem(Labeled labeled, T item, boolean empty, T waitingValue, Function<T, String> itemToString) {
        if (!empty) {
            if (waitingValue.equals(item)) {
                setWaitingText(labeled);
            } else {
                if (itemToString != null) {
                    labeled.setText(itemToString.apply(item));
                    labeled.setTextFill(Color.BLACK);
                }
            }
        }
    }

    public static <T> void setWaitingCellFactory(ListView<T> listView, T waitingValue) {
        setWaitingCellFactory(listView, waitingValue, null);
    }

    public static <T> void setWaitingCellFactory(ListView<T> listView, T waitingValue, Function<T, String> itemToString) {
        Objects.requireNonNull(listView);
        Objects.requireNonNull(waitingValue);
        listView.setCellFactory(param -> new TextFieldListCell<T>() {
            @Override
            public void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                GseUtil.updateItem(this, item, empty, waitingValue, itemToString);
            }
        });
    }

    public static <T> void setWaitingCellFactory(CheckListView<T> listView, T waitingValue) {
        setWaitingCellFactory(listView, waitingValue, null);
    }

    public static <T> void setWaitingCellFactory(CheckListView<T> listView, T waitingValue, Function<T, String> itemToString) {
        Objects.requireNonNull(listView);
        Objects.requireNonNull(waitingValue);
        listView.setCellFactory(l -> new CheckBoxListCell<T>(listView::getItemBooleanProperty) {
            @Override
            public void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                GseUtil.updateItem(this, item, empty, waitingValue, itemToString);
            }
        });
    }

    public static <T> void setWaitingCellFactory(TreeView<T> treeView, T waitingValue, Function<T, String> itemToString) {
        Objects.requireNonNull(treeView);
        Objects.requireNonNull(waitingValue);
        treeView.setCellFactory(param -> new TextFieldTreeCell<T>() {
            @Override
            public void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty) {
                    if (waitingValue.equals(item)) {
                        setWaitingText(this);
                    } else {
                        setText(itemToString.apply(item));
                        setTextFill(Color.BLACK);
                        setGraphic(getTreeItem().getGraphic());
                    }
                }
            }
        });
    }

    public static void execute(Executor executor, Runnable task) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(task);
        executor.execute(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                Platform.runLater(() -> GseUtil.showDialogError(t));
            }
        });
    }

    public static void showDialogError(Throwable t) {
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error(t.toString(), t);
        }
        Platform.runLater(
            () -> {
                ExceptionDialog exceptionDialog = new ExceptionDialog(t);
                exceptionDialog.setTitle(RESOURCE_BUNDLE.getString("Error"));
                exceptionDialog.showAndWait();
            });
    }

    public static <T> Service<T> createService(Task<T> task, Executor executor) {
        Service<T> service = new Service<T>() {
            @Override
            protected Task<T> createTask() {
                return task;
            }
        };
        service.setExecutor(executor);
        service.setOnFailed(event -> {
            Throwable exception = event.getSource().getException();
            if (exception != null) {
                LOGGER.error(exception.toString(), exception);
                showDialogError(exception);
            }
        });
        return service;
    }

    public static <T> void registerAccelerator(Scene scene, KeyCodeCombination key, Class<T> aClass, Consumer<T> a) {
        scene.getAccelerators().put(key, () -> {
            Node node = scene.getFocusOwner();
            while (node != null) {
                if (aClass.isAssignableFrom(node.getClass())) {
                    a.accept((T) node);
                    break;
                }
                node = node.getParent();
            }
        });
    }

    public static void registerAccelerators(Scene scene) {
        // save accelerator
        registerAccelerator(scene, new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), Savable.class, Savable::save);
        registerAccelerator(scene, new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN), Runnable.class, Runnable::run);
    }

    public static void setProxy() {
        try {
            URLConnection connection = new URL("http://www.google.com").openConnection();
            connection.setConnectTimeout(1000);
            connection.connect();
        } catch (IOException e) {
            // get proxy config
            ModuleConfig proxyConfig = PlatformConfig.defaultConfig().getModuleConfigIfExists("proxy");
            if (proxyConfig != null) {
                String host = proxyConfig.getStringProperty("host");
                int port = proxyConfig.getIntProperty("port");
                String user = proxyConfig.getStringProperty("user");
                String password = proxyConfig.getStringProperty("password");
                LOGGER.info("Set proxy host={}, port={}, user={}", host, port, user);

                System.getProperties().put("http.proxyHost", host);
                System.getProperties().put("http.proxyPort", Integer.toString(port));
                Authenticator.setDefault(
                        new Authenticator() {
                            @Override
                            public PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(user, password.toCharArray());
                            }
                        }
                );
            }
        }
    }
}
