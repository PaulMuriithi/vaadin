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

package com.vaadin.tests.tb3;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.vaadin.testbench.Parameters;
import com.vaadin.testbench.commands.TestBenchCommands;

/**
 * Base class which provides functionality for tests which use the automatic
 * screenshot comparison function.
 * 
 * @author Vaadin Ltd
 */
public abstract class ScreenshotTB3Test extends AbstractTB3Test {

    private String screenshotBaseName;

    @Rule
    public TestRule watcher = new TestWatcher() {

        @Override
        protected void starting(org.junit.runner.Description description) {
            Class<?> testClass = description.getTestClass();
            // Runner adds [BrowserName] which we do not want to use in the
            // screenshot name
            String testMethod = description.getMethodName();
            testMethod = testMethod.replaceAll("\\[.*\\]", "");

            String className = testClass.getSimpleName();
            if (testClass.getEnclosingClass() != null) {
                className = testClass.getEnclosingClass().getSimpleName();
            }

            screenshotBaseName = className + "-" + testMethod;
        };
    };

    /**
     * Contains a list of screenshot identifiers for which
     * {@link #compareScreen(String)} has failed during the test
     */
    private List<String> screenshotFailures = new ArrayList<String>();

    /**
     * Defines TestBench screen comparison parameters before each test run
     */
    @Before
    public void setupScreenComparisonParameters() {
        Parameters.setScreenshotErrorDirectory(getScreenshotErrorDirectory());
        Parameters
                .setScreenshotReferenceDirectory(getScreenshotReferenceDirectory());
    }

    /**
     * Grabs a screenshot and compares with the reference image with the given
     * identifier. Supports alternative references and will succeed if the
     * screenshot matches at least one of the references.
     * 
     * In case of a failed comparison this method stores the grabbed screenshots
     * in the error directory as defined by
     * {@link #getScreenshotErrorDirectory()}. It will also generate a html file
     * in the same directory, comparing the screenshot with the first found
     * reference.
     * 
     * @param identifier
     * @throws IOException
     */
    protected void compareScreen(String identifier) throws IOException {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Empty identifier not supported");
        }

        File mainReference = getScreenshotReferenceFile(identifier);

        List<File> alternativeFiles = findReferenceAlternatives(mainReference);
        List<File> failedReferenceAlternatives = new ArrayList<File>();

        for (File file : alternativeFiles) {
            if (testBench(driver).compareScreen(file)) {
                break;
            } else {
                failedReferenceAlternatives.add(file);
            }
        }

        File referenceToKeep = null;
        if (failedReferenceAlternatives.size() != alternativeFiles.size()) {
            // Matched one comparison but not all, remove all error images +
            // HTML files
        } else {
            // All comparisons failed, keep the main error image + HTML
            screenshotFailures.add(mainReference.getName());
            referenceToKeep = mainReference;
        }

        // Remove all PNG/HTML files we no longer need (failed alternative
        // references or all error files (PNG/HTML) if comparison succeeded)
        for (File failedAlternative : failedReferenceAlternatives) {
            File failurePng = getErrorFileFromReference(failedAlternative);
            if (failedAlternative != referenceToKeep) {
                // Delete png + HTML
                String htmlFileName = failurePng.getName().replace(".png",
                        ".html");
                File failureHtml = new File(failurePng.getParentFile(),
                        htmlFileName);

                failurePng.delete();
                failureHtml.delete();
            }
        }
    }

    /**
     * 
     * @param referenceFile
     *            The reference image file (in the directory defined by
     *            {@link #getScreenshotReferenceDirectory()})
     * @return the file name of the file generated in the directory defined by
     *         {@link #getScreenshotErrorDirectory()} if comparison with the
     *         given reference image fails.
     */
    private File getErrorFileFromReference(File referenceFile) {
        return new File(referenceFile.getAbsolutePath().replace(
                getScreenshotReferenceDirectory(),
                getScreenshotErrorDirectory()));
    }

    /**
     * Finds alternative references for the given files
     * 
     * @param reference
     * @return all references which should be considered when comparing with the
     *         given files, including the given reference
     */
    private List<File> findReferenceAlternatives(File reference) {
        List<File> files = new ArrayList<File>();
        files.add(reference);

        File screenshotDir = reference.getParentFile();
        String name = reference.getName();
        // Remove ".png"
        String nameBase = name.substring(0, name.length() - 4);
        for (int i = 1;; i++) {
            File file = new File(screenshotDir, nameBase + "_" + i + ".png");
            if (file.exists()) {
                files.add(file);
            } else {
                break;
            }
        }

        return files;
    }

    /**
     * @param testName
     * @return the reference file name to use for the given browser, as
     *         described by {@literal capabilities}, and identifier
     */
    private File getScreenshotReferenceFile(String identifier) {
        DesiredCapabilities capabilities = getDesiredCapabilities();

        String originalName = getScreenshotReferenceName(identifier);
        File exactVersionFile = new File(originalName);
        if (exactVersionFile.exists()) {
            return exactVersionFile;
        }

        String browserVersion = capabilities.getVersion();

        if (browserVersion.matches("\\d+")) {
            for (int version = Integer.parseInt(browserVersion); version > 0; version--) {
                String fileName = getScreenshotReferenceName(identifier,
                        version);
                File oldVersionFile = new File(fileName);
                if (oldVersionFile.exists()) {
                    return oldVersionFile;
                }
            }
        }

        return exactVersionFile;
    }

    /**
     * @return the base directory of 'reference' and 'errors' screenshots
     */
    protected abstract String getScreenshotDirectory();

    /**
     * @return the directory where reference images are stored (the 'reference'
     *         folder inside the screenshot directory)
     */
    private String getScreenshotReferenceDirectory() {
        return getScreenshotDirectory() + "/reference";
    }

    /**
     * @return the directory where comparison error images should be created
     *         (the 'errors' folder inside the screenshot directory)
     */
    private String getScreenshotErrorDirectory() {
        return getScreenshotDirectory() + "/errors";
    }

    /**
     * Checks if any screenshot comparisons failures occurred during the test
     * and combines all comparison errors into one exception
     * 
     * @throws IOException
     *             If there were failures during the test
     */
    @After
    public void checkCompareFailures() throws IOException {
        if (!screenshotFailures.isEmpty()) {
            throw new IOException(
                    "The following screenshots did not match the reference: "
                            + screenshotFailures.toString());
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vaadin.tests.tb3.AbstractTB3Test#onUncaughtException(java.lang.Throwable
     * )
     */
    @Override
    public void onUncaughtException(Throwable cause) {
        super.onUncaughtException(cause);
        // Grab a "failure" screenshot and store in the errors folder for later
        // analysis
        try {
            TestBenchCommands testBench = testBench();
            if (testBench != null) {
                testBench.disableWaitForVaadin();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        try {
            if (driver != null) {
                BufferedImage screenshotImage = ImageIO
                        .read(new ByteArrayInputStream(
                                ((TakesScreenshot) driver)
                                        .getScreenshotAs(OutputType.BYTES)));
                ImageIO.write(screenshotImage, "png", new File(
                        getScreenshotFailureName()));
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

    /**
     * @return the name of a "failure" image which is stored in the folder
     *         defined by {@link #getScreenshotErrorDirectory()} when the test
     *         fails
     */
    private String getScreenshotFailureName() {
        return getScreenshotErrorBaseName() + "-failure.png";
    }

    /**
     * @return the base name used for screenshots. This is the first part of the
     *         screenshot file name, typically created as "testclass-testmethod"
     */
    public String getScreenshotBaseName() {
        return screenshotBaseName;
    }

    /**
     * Returns the name of the reference file based on the given parameters.
     * 
     * @param testName
     * @param capabilities
     * @param identifier
     * @return the full path of the reference
     */
    private String getScreenshotReferenceName(String identifier) {
        return getScreenshotReferenceName(identifier, null);
    }

    /**
     * Returns the name of the reference file based on the given parameters. The
     * version given in {@literal capabilities} is used unless it is overridden
     * by the {@literal versionOverride} parameter.
     * 
     * @param testName
     * @param capabilities
     * @param identifier
     * @return the full path of the reference
     */
    private String getScreenshotReferenceName(String identifier,
            Integer versionOverride) {
        String uniqueBrowserIdentifier;
        if (versionOverride == null) {
            uniqueBrowserIdentifier = BrowserUtil
                    .getUniqueIdentifier(getDesiredCapabilities());
        } else {
            uniqueBrowserIdentifier = BrowserUtil.getUniqueIdentifier(
                    getDesiredCapabilities(), "" + versionOverride);
        }

        // WindowMaximizeRestoreTest_Windows_InternetExplorer_8_window-1-moved-maximized-restored.png
        return getScreenshotReferenceDirectory() + "/"
                + getScreenshotBaseName() + "_" + uniqueBrowserIdentifier + "_"
                + identifier + ".png";
    }

    /**
     * Returns the base name of the screenshot in the error directory. This is a
     * name so that all files matching {@link #getScreenshotErrorBaseName()}*
     * are owned by this test instance (taking into account
     * {@link #getDesiredCapabilities()}) and can safely be removed before
     * running this test.
     */
    private String getScreenshotErrorBaseName() {
        return getScreenshotReferenceName("dummy", null).replace(
                getScreenshotReferenceDirectory(),
                getScreenshotErrorDirectory()).replace("_dummy.png", "");
    }

    /**
     * Removes any old screenshots related to this test from the errors
     * directory before running the test
     */
    @Before
    public void cleanErrorDirectory() {
        // Remove any screenshots for this test from the error directory
        // before running it. Leave unrelated files as-is
        File errorDirectory = new File(getScreenshotErrorDirectory());

        // Create errors directory if it does not exist
        if (!errorDirectory.exists()) {
            errorDirectory.mkdirs();
        }

        final String errorBase = getScreenshotErrorBaseName()
                .replace("\\", "/");
        File[] files = errorDirectory.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                String thisFile = pathname.getAbsolutePath().replace("\\", "/");
                if (thisFile.startsWith(errorBase)) {
                    return true;
                }
                return false;
            }
        });
        for (File f : files) {
            f.delete();
        }
    }
}
