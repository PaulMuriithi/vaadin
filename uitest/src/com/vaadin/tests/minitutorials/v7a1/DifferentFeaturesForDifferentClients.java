/* 
 * Copyright 2011 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.tests.minitutorials.v7a1;

import com.vaadin.server.UIProvider;
import com.vaadin.server.WebBrowser;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

/**
 * Mini tutorial code for
 * https://vaadin.com/wiki/-/wiki/Main/Creating%20an%20application
 * %20with%20different%20features%20for%20different%20clients
 * 
 * @author Vaadin Ltd
 * @since 7.0.0
 */
public class DifferentFeaturesForDifferentClients extends UIProvider {

    @Override
    public Class<? extends UI> getUIClass(VaadinRequest request) {
        // could also use browser version etc.
        if (request.getHeader("user-agent").contains("mobile")) {
            return TouchRoot.class;
        } else {
            return DefaultRoot.class;
        }
    }

    // Must override as default implementation isn't allowed to
    // instantiate our non-public classes
    @Override
    public UI createInstance(VaadinRequest request,
            Class<? extends UI> type) {
        try {
            return type.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

class DefaultRoot extends UI {
    @Override
    protected void init(VaadinRequest request) {
        getContent().addComponent(
                new Label("This browser does not support touch events"));
    }
}

class TouchRoot extends UI {
    @Override
    protected void init(VaadinRequest request) {
        WebBrowser webBrowser = request.getBrowserDetails().getWebBrowser();
        String screenSize = "" + webBrowser.getScreenWidth() + "x"
                + webBrowser.getScreenHeight();
        getContent().addComponent(
                new Label("Using a touch enabled device with screen size"
                        + screenSize));
    }
}
