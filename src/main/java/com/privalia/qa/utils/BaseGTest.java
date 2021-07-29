/*
 * Copyright (c) 2021, Veepee
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose
 * with or without fee is hereby  granted, provided that the above copyright notice
 * and this permission notice appear in all copies.
 *
 * THE SOFTWARE  IS PROVIDED "AS IS"  AND THE AUTHOR DISCLAIMS  ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE INCLUDING  ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS.  IN NO  EVENT  SHALL THE  AUTHOR  BE LIABLE  FOR  ANY SPECIAL,  DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
 * OF USE, DATA  OR PROFITS, WHETHER IN AN ACTION OF  CONTRACT, NEGLIGENCE OR OTHER
 * TORTIOUS ACTION, ARISING OUT OF OR  IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
*/

package com.privalia.qa.utils;

import com.privalia.qa.cucumber.testng.CucumberOptionsImpl;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * This is a custom implementation of {@link AbstractTestNGCucumberTests} for adding special configuration
 * of GingerSpec to the {@link CucumberOptions} annotation of the class
 *
 * Test classes must extend this class in order to be executed with TestNG and use the Gingerspec steps
 * and other functionality
 *
 * @author Jose Fernandez
 */
abstract public class BaseGTest extends AbstractTestNGCucumberTests {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getCanonicalName());

    @BeforeSuite(alwaysRun = true)
    public void beforeGSuite(ITestContext context) {
    }

    /**
     * Method executed after a suite.
     *
     * @param context the context
     */
    @AfterSuite(alwaysRun = true)
    public void afterGSuite(ITestContext context) {
    }

    /**
     * Overrides the parent method {@link AbstractTestNGCucumberTests#setUpClass()} and executes custom
     * code before the  object is created
     */
    @Override
    @BeforeClass(alwaysRun = true)
    public void setUpClass() {
        try {
            this.modifyCucumberOptions();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        super.setUpClass();
    }

    /**
     * Method executed before a suite.
     *
     * Before the test is executed, the library modifies the {@link CucumberOptions} annotation
     * in the runner class  to include some special configuration for GingerSpec.
     *
     * @throws NoSuchMethodException        NoSuchMethodException
     * @throws InvocationTargetException    InvocationTargetException
     * @throws IllegalAccessException       IllegalAccessException
     * @throws NoSuchFieldException         NoSuchFieldException
     */
    private void modifyCucumberOptions() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {

        CucumberOptionsImpl newAnnotation = new CucumberOptionsImpl(this.getClass());

        Method method = Class.class.getDeclaredMethod("annotationData", null);
        method.setAccessible(true);
        //Since AnnotationData is a private class we cannot create a direct reference to it. We will have to
        //manage with just Object
        Object annotationData = method.invoke(this.getClass());
        //We now look for the map called "annotations" within AnnotationData object.
        Field annotations = annotationData.getClass().getDeclaredField("annotations");
        annotations.setAccessible(true);
        Map<Class<? extends Annotation>, Annotation> map = (Map<Class<? extends Annotation>, Annotation>) annotations.get(annotationData);
        map.put(CucumberOptions.class, newAnnotation);

    }

    /**
     * Method executed before a test class.
     *
     * @param context the context
     */
    @BeforeClass(alwaysRun = true)
    public void beforeGClass(ITestContext context) {
        ThreadProperty.set("class", this.getClass().getCanonicalName());
    }

    /**
     * Method executed after a test method.
     *
     * @param method the method
     */
    @BeforeMethod(alwaysRun = true)
    public void beforeGMethod(Method method) {
    }

    /**
     * Method executed before method.
     *
     * @param method the method
     */
    @AfterMethod(alwaysRun = true)
    public void afterGMethod(Method method) {
    }

    /**
     * Method executed before a class.
     */
    @AfterClass()
    public void afterGClass() {
    }

}
