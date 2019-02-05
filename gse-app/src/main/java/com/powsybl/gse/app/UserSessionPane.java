/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.gse.app;

import com.powsybl.afs.ws.client.utils.UserSession;
import com.powsybl.commons.net.UserProfile;
import com.powsybl.gse.spi.GseAuthenticator;
import com.powsybl.gse.spi.GseContext;
import com.powsybl.gse.util.Glyph;
import com.powsybl.gse.util.GseUtil;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import org.controlsfx.dialog.LoginDialog;

import java.util.Objects;
import java.util.ResourceBundle;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
class UserSessionPane extends StackPane {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("lang.GseAppBar");

    private final GseContext context;

    private final GseAuthenticator authenticator;

    private final Button signInButton;

    private final Text userName = new Text();

    private final Button chevronDownButton;

    private final ContextMenu menu;

    private final ObjectProperty<UserSession> sessionProperty = new SimpleObjectProperty<>();

    UserSessionPane(GseContext context, GseAuthenticator authenticator) {
        this.context = Objects.requireNonNull(context);
        this.authenticator = Objects.requireNonNull(authenticator);

        Text signInGlyph = Glyph.createAwesomeFont('\uf090');
        signInGlyph.getStyleClass().add("gse-app-bar-icon");
        signInButton = new Button("", signInGlyph);
        signInButton.getStyleClass().add("gse-app-bar-button");
        signInButton.setOnAction(event -> signIn());

        userName.getStyleClass().add("gse-app-bar-user-name");

        menu = new ContextMenu();
        Text signOutGlyph = Glyph.createAwesomeFont('\uf090');
        MenuItem signOutMenuItem = new MenuItem(RESOURCE_BUNDLE.getString("SignOut"), signOutGlyph);
        signOutMenuItem.setOnAction(event -> signOut());
        menu.getItems().add(signOutMenuItem);

        Text chevronDownGlyph = Glyph.createAwesomeFont('\uf078');
        chevronDownGlyph.getStyleClass().addAll("gse-app-bar-icon", "gse-app-bar-user-menu-icon");
        chevronDownButton = new Button("", chevronDownGlyph);
        chevronDownButton.getStyleClass().add("gse-app-bar-button");
        chevronDownButton.setOnAction(event -> showUserMenu());

        userName.getStyleClass().add("gse-app-bar-user-name");

        setUserSession(null);
        sessionProperty.addListener((observable, oldSession, newSession) -> setUserSession(newSession));
    }

    public ObjectProperty<UserSession> sessionProperty() {
        return sessionProperty;
    }

    private void showUserMenu() {
        menu.show(chevronDownButton, Side.BOTTOM, 0, 0);
    }

    private void setUserSession(UserSession userSession) {
        if (userSession == null) {
            userName.setText(null);
            getChildren().setAll(signInButton);
        } else {
            userName.setText(userSession.getProfile().getFirstName() + " " + userSession.getProfile().getLastName());
            HBox sessionBox = new HBox(userName, chevronDownButton);
            sessionBox.setAlignment(Pos.CENTER_LEFT);
            getChildren().setAll(sessionBox);
        }
    }

    private void signIn() {
        LoginDialog loginDialog = new LoginDialog(null, null);
        loginDialog.initOwner(getScene().getWindow());
        loginDialog.showAndWait().ifPresent(credentials -> {
            sessionProperty.set(new UserSession(new UserProfile("...", ""), null));
            context.getExecutor().execute(() -> {
                try {
                    UserSession userSession = authenticator.signIn(credentials.getKey(), credentials.getValue());
                    Platform.runLater(() -> sessionProperty.set(userSession));
                } catch (Throwable t) {
                    Platform.runLater(() -> {
                        sessionProperty.set(null);
                        GseUtil.showDialogError(t);
                    });
                }
            });
        });
    }

    private void signOut() {
        sessionProperty.set(null);
    }
}
