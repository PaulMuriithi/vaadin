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

package com.vaadin.server;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vaadin.LegacyApplication;
import com.vaadin.ui.UI;

public abstract class LegacyApplicationUIProvider extends UIProvider {
    /**
     * Ignore initial / and then get everything up to the next /
     */
    private static final Pattern WINDOW_NAME_PATTERN = Pattern
            .compile("^/?([^/]+).*");

    @Override
    public Class<? extends UI> getUIClass(VaadinRequest request) {
        UI uiInstance = getUIInstance(request);
        if (uiInstance != null) {
            return uiInstance.getClass();
        }
        return null;
    }

    @Override
    public UI createInstance(VaadinRequest request, Class<? extends UI> type) {
        return getUIInstance(request);
    }

    @Override
    public String getTheme(VaadinRequest request, Class<? extends UI> uiClass) {
        LegacyApplication application = getApplication();
        if (application != null) {
            return application.getTheme();
        } else {
            return null;
        }
    }

    @Override
    public String getPageTitle(VaadinRequest request,
            Class<? extends UI> uiClass) {
        UI uiInstance = getUIInstance(request);
        if (uiInstance != null) {
            return uiInstance.getCaption();
        } else {
            return super.getPageTitle(request, uiClass);
        }
    }

    private UI getUIInstance(VaadinRequest request) {
        String pathInfo = request.getRequestPathInfo();
        String name = null;
        if (pathInfo != null && pathInfo.length() > 0) {
            Matcher matcher = WINDOW_NAME_PATTERN.matcher(pathInfo);
            if (matcher.matches()) {
                // Skip the initial slash
                name = matcher.group(1);
            }
        }

        LegacyApplication application = getApplication();
        if (application == null) {
            return null;
        }
        UI.LegacyWindow window = application.getWindow(name);
        if (window != null) {
            return window;
        }
        return application.getMainWindow();
    }

    /**
     * This implementation simulates the way of finding a window for a request
     * by extracting a window name from the requested path and passes that name
     * to {@link #getWindow(String)}.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public UI getExistingUI(VaadinRequest request) {
        UI uiInstance = getUIInstance(request);
        if (uiInstance == null || uiInstance.getUIId() == -1) {
            // Not initialized -> Let go through createUIInstance to make it
            // initialized
            return null;
        } else {
            UI.setCurrent(uiInstance);
            return uiInstance;
        }
    }

    private LegacyApplication getApplication() {
        LegacyApplication application = VaadinSession.getCurrent()
                .getAttribute(LegacyApplication.class);
        if (application == null) {
            application = createApplication();
            if (application == null) {
                return null;
            }
            VaadinSession.getCurrent().setAttribute(LegacyApplication.class,
                    application);
            application.doInit();
        }

        if (application != null && !application.isRunning()) {
            VaadinSession.getCurrent().setAttribute(LegacyApplication.class,
                    null);
            // Run again without a current application
            return getApplication();
        }

        return application;
    }

    protected abstract LegacyApplication createApplication();

}
