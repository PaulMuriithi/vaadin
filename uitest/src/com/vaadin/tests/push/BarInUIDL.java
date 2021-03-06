/*
 * Copyright 2000-2013 Vaadin Ltd.
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

package com.vaadin.tests.push;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.annotations.Push;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.tests.components.AbstractTestUI;
import com.vaadin.tests.tb3.MultiBrowserTest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;

@Push(transport = Transport.STREAMING)
public class BarInUIDL extends AbstractTestUI {

    public static class BarInUIDLTest extends MultiBrowserTest {
        @Test
        public void sendBarInUIDL() {
            getButton().click();
            Assert.assertEquals(
                    "Thank you for clicking | bar",
                    vaadinElement(
                            "/VVerticalLayout[0]/Slot[1]/VVerticalLayout[0]/Slot[1]/VLabel[0]")
                            .getText());
            getButton().click();
            Assert.assertEquals(
                    "Thank you for clicking | bar",
                    vaadinElement(
                            "/VVerticalLayout[0]/Slot[1]/VVerticalLayout[0]/Slot[2]/VLabel[0]")
                            .getText());
        }

        private WebElement getButton() {
            return vaadinElement("/VVerticalLayout[0]/Slot[1]/VVerticalLayout[0]/Slot[0]/VButton[0]");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.tests.components.AbstractTestUI#setup(com.vaadin.server.
     * VaadinRequest)
     */
    @Override
    protected void setup(VaadinRequest request) {
        Button button = new Button("Click Me");
        button.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                addComponent(new Label("Thank you for clicking | bar"));
            }
        });
        addComponent(button);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.tests.components.AbstractTestUI#getTestDescription()
     */
    @Override
    protected String getTestDescription() {
        return "Verify that there is no problem with messages containing | by clicking the button repeatedly";
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.vaadin.tests.components.AbstractTestUI#getTicketNumber()
     */
    @Override
    protected Integer getTicketNumber() {
        return 12404;
    }

}
