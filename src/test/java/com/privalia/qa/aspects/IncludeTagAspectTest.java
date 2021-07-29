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

package com.privalia.qa.aspects;


import com.privalia.qa.exceptions.IncludeException;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;


public class IncludeTagAspectTest {
    public LoopIncludeTagAspect inctag = new LoopIncludeTagAspect();

    @Test
    public void testGetFeature() {
        assertThat("test.feature").as("Test feature name is extracted correctly").isEqualTo(inctag.getFeatureName("@include(feature: test.feature,scenario: To copy)"));
    }

    @Test
    public void testGetScenario() {
        assertThat("To copy").as("Test scenario name is extracted correctly").isEqualTo(inctag.getScenName("@include(feature: test.feature,scenario: To copy)"));
    }

    @Test
    public void testGetParams() {
        assertThat(4).as("Test that the number of keys and values are correctly calculated for params").isEqualTo(inctag.getParams("@include(feature: test.feature,scenario: To copy,params: [time1:9, time2:9])").length);
    }

    @Test
    public void testDoReplaceKeys() throws IncludeException {
        String keysNotReplaced = "Given that <time1> is not equal to <time2> into a step";
        String[] keys = {"<time1>", "9", "<time2>", "8"};
        assertThat("Given that 9 is not equal to 8 into a step").as("Test that keys are correctly replaced at scenario outlines").isEqualTo(inctag.doReplaceKeys(keysNotReplaced, keys));
    }

    @Test
    public void testCheckParams() throws IncludeException {
        String lineOfParams = "| hey | ho |";
        String[] keys = {"<time1>", "9", "<time2>", "8"};
        String[] tonsOfKeys = {"<time1>", "9", "<time2>", "23", "33", "32", "10"};
        assertThat(inctag.checkParams(lineOfParams, keys)).as("Test that include parameters match the number of them at the scenario outline included").isTrue();
        assertThat(inctag.checkParams(lineOfParams, tonsOfKeys)).as("Test that include parameters match the number of them at the scenario outline included").isFalse();
    }

    @Test
    public void testTagIterationSkip() throws Exception {
        String path = "";
        List<String> lines = new ArrayList<>();
        lines.add("@include(testCheckParams)");

        assertThatExceptionOfType(Exception.class).isThrownBy(() -> inctag.parseLines(lines, path));
    }
}