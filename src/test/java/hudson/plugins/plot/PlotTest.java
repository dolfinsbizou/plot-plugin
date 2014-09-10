/*
 * The MIT License
 *
 * Copyright (c) 2014 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.plot;

import static org.junit.Assert.assertEquals;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.tasks.LogRotator;
import hudson.tasks.Shell;

import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class PlotTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    @Test
    public void discardPlotSamplesForOldBuilds() throws Exception {
        FreeStyleProject p = jobArchivingBuilds(1);

        plotBuilds(p, "2", false);

        j.buildAndAssertSuccess(p);
        assertSampleCount(p, 1);

        j.buildAndAssertSuccess(p);
        assertSampleCount(p, 1); // Truncated to 1

        j.buildAndAssertSuccess(p);
        assertSampleCount(p, 1); // Still 1
    }

    @Test
    public void discardPlotSamplesForDeletedBuilds() throws Exception {
        FreeStyleProject p = jobArchivingBuilds(10);

        plotBuilds(p, "", false);

        j.buildAndAssertSuccess(p);
        assertSampleCount(p, 1);

        j.buildAndAssertSuccess(p);
        assertSampleCount(p, 2);

        j.buildAndAssertSuccess(p);
        assertSampleCount(p, 3);

        p.getLastBuild().delete();
        assertSampleCount(p, 2); // Data should be removed with the build
    }

    @Test
    public void keepPlotSamplesForOldBuilds() throws Exception {
        FreeStyleProject p = jobArchivingBuilds(1);

        plotBuilds(p, "2", true);

        j.buildAndAssertSuccess(p);
        assertSampleCount(p, 1);

        j.buildAndAssertSuccess(p);
        assertSampleCount(p, 2);

        j.buildAndAssertSuccess(p);
        assertSampleCount(p, 2); // Plot 2 builds

        j.buildAndAssertSuccess(p);
        assertSampleCount(p, 2); // Still 2
    }

    @Test
    public void keepPlotSamplesForDeletedBuilds() throws Exception {
        FreeStyleProject p = jobArchivingBuilds(10);

        plotBuilds(p, "", true);

        j.buildAndAssertSuccess(p);
        assertSampleCount(p, 1);

        j.buildAndAssertSuccess(p);
        assertSampleCount(p, 2);

        j.buildAndAssertSuccess(p);
        assertSampleCount(p, 3);

        p.getLastBuild().delete();
        assertSampleCount(p, 3); // Data should be kept
    }

    private FreeStyleProject jobArchivingBuilds(int count) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new Shell("echo YVALUE=$BUILD_NUMBER > src.properties"));
        p.setBuildDiscarder(new LogRotator(-1, count, -1, -1));

        return p;
    }

    private void plotBuilds(FreeStyleProject p, String count, boolean keepRecords) {
        final PlotPublisher publisher = new PlotPublisher();
        final Plot plot = new Plot("Title", "Number", "default", count, null, "line", false, keepRecords);
        p.getPublishersList().add(publisher);
        publisher.addPlot(plot);
        plot.series = Arrays.<Series>asList(new PropertiesSeries("src.properties", null));
    }

    private void assertSampleCount(Job p, int count) throws Exception {
        PlotReport pr = p.getAction(PlotAction.class).getDynamic("default", null, null);
        List<List<String>> table = pr.getTable(0);
        assertEquals("Plot sample count", count, table.size() - 1);
    }
}
