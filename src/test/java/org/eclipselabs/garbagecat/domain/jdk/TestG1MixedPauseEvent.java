/**********************************************************************************************************************
 * garbagecat                                                                                                         *
 *                                                                                                                    *
 * Copyright (c) 2008-2021 Mike Millson                                                                               *
 *                                                                                                                    * 
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse *
 * Public License v1.0 which accompanies this distribution, and is available at                                       *
 * http://www.eclipse.org/legal/epl-v10.html.                                                                         *
 *                                                                                                                    *
 * Contributors:                                                                                                      *
 *    Mike Millson - initial API and implementation                                                                   *
 *********************************************************************************************************************/
package org.eclipselabs.garbagecat.domain.jdk;

import static org.eclipselabs.garbagecat.util.Memory.kilobytes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.eclipselabs.garbagecat.TestUtil;
import org.eclipselabs.garbagecat.domain.JvmRun;
import org.eclipselabs.garbagecat.service.GcManager;
import org.eclipselabs.garbagecat.util.Constants;
import org.eclipselabs.garbagecat.util.jdk.Analysis;
import org.eclipselabs.garbagecat.util.jdk.JdkRegEx;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil.LogEventType;
import org.eclipselabs.garbagecat.util.jdk.Jvm;
import org.junit.jupiter.api.Test;

/**
 * @author James Livingston
 * @author <a href="mailto:mmillson@redhat.com">Mike Millson</a>
 */
class TestG1MixedPauseEvent {

    @Test
    void testIsBlocking() {
        String logLine = "72.598: [GC pause (mixed) 643M->513M(724M), 0.1686650 secs]";
        assertTrue(JdkUtil.isBlocking(JdkUtil.identifyEventType(logLine)),
                JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + " not indentified as blocking.");
    }

    @Test
    void testLogLine() {
        String logLine = "72.598: [GC pause (mixed) 643M->513M(724M), 0.1686650 secs]";
        assertTrue(G1MixedPauseEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + ".");
        G1MixedPauseEvent event = new G1MixedPauseEvent(logLine);
        assertEquals((long) 72598, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(kilobytes(658432), event.getCombinedOccupancyInit(), "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(525312), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(741376), event.getCombinedSpace(), "Combined available size not parsed correctly.");
        assertEquals(168665, event.getDuration(), "Duration not parsed correctly.");
    }

    @Test
    void testLogLineWithTimesData() {
        String logLine = "72.598: [GC pause (mixed) 643M->513M(724M), 0.1686650 secs] "
                + "[Times: user=0.22 sys=0.00, real=0.22 secs]";
        assertTrue(G1MixedPauseEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + ".");
        G1MixedPauseEvent event = new G1MixedPauseEvent(logLine);
        assertEquals((long) 72598, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(kilobytes(658432), event.getCombinedOccupancyInit(), "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(525312), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(741376), event.getCombinedSpace(), "Combined available size not parsed correctly.");
        assertEquals(168665, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(22, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(0, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(22, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(100, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testLogLineSpacesAtEnd() {
        String logLine = "72.598: [GC pause (mixed) 643M->513M(724M), 0.1686650 secs] "
                + "[Times: user=0.22 sys=0.00, real=0.22 secs]   ";
        assertTrue(G1MixedPauseEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + ".");
        G1MixedPauseEvent event = new G1MixedPauseEvent(logLine);
        assertEquals((long) 72598, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(kilobytes(658432), event.getCombinedOccupancyInit(), "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(525312), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(741376), event.getCombinedSpace(), "Combined available size not parsed correctly.");
        assertEquals(168665, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(100, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testTriggerG1EvacuationPause() {
        String logLine = "81.757: [GC pause (G1 Evacuation Pause) (mixed) 1584M->1390M(8192M), 0.1472883 secs]";
        assertTrue(G1MixedPauseEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + ".");
        G1MixedPauseEvent event = new G1MixedPauseEvent(logLine);
        assertEquals((long) 81757, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertTrue(event.getTrigger().matches(JdkRegEx.TRIGGER_G1_EVACUATION_PAUSE), "Trigger not parsed correctly.");
        assertEquals(kilobytes(1584 * 1024), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(1390 * 1024), event.getCombinedOccupancyEnd(),
                "Combined end size not parsed correctly.");
        assertEquals(kilobytes(8192 * 1024), event.getCombinedSpace(), "Combined available size not parsed correctly.");
        assertEquals(147288, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(0, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(0, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(0, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(100, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testTriggerG1EvacuationPauseDashDash() {
        String logLine = "424692.063: [GC pause (G1 Evacuation Pause) (mixed)-- 8129M->7812M(8192M), 0.0890849 secs]";
        assertTrue(G1MixedPauseEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + ".");
        G1MixedPauseEvent event = new G1MixedPauseEvent(logLine);
        assertEquals((long) 424692063, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertTrue(event.getTrigger().matches(JdkRegEx.TRIGGER_G1_EVACUATION_PAUSE), "Trigger not parsed correctly.");
        assertEquals(kilobytes(8129 * 1024), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(7812 * 1024), event.getCombinedOccupancyEnd(),
                "Combined end size not parsed correctly.");
        assertEquals(kilobytes(8192 * 1024), event.getCombinedSpace(), "Combined available size not parsed correctly.");
        assertEquals(89084, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(0, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(0, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(0, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(100, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testLogLinePreprocessedTriggerBeforeG1EvacuationPause() {
        String logLine = "2973.338: [GC pause (G1 Evacuation Pause) (mixed), 0.0457502 secs]"
                + "[Eden: 112.0M(112.0M)->0.0B(112.0M) Survivors: 16.0M->16.0M Heap: 12.9G(30.0G)->11.3G(30.0G)]"
                + " [Times: user=0.19 sys=0.00, real=0.05 secs]";
        assertTrue(G1MixedPauseEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + ".");
        G1MixedPauseEvent event = new G1MixedPauseEvent(logLine);
        assertTrue(event.getTrigger().matches(JdkRegEx.TRIGGER_G1_EVACUATION_PAUSE), "Trigger not parsed correctly.");
        assertEquals((long) 2973338, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(kilobytes(13526630), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(11848909), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(30 * 1024 * 1024), event.getCombinedSpace(),
                "Combined available size not parsed correctly.");
        assertEquals(45750, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(19, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(0, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(5, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(380, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testLogLinePreprocessedNoTrigger() {
        String logLine = "3082.652: [GC pause (mixed), 0.0762060 secs]"
                + "[Eden: 1288.0M(1288.0M)->0.0B(1288.0M) Survivors: 40.0M->40.0M Heap: 11.8G(26.0G)->9058.4M(26.0G)]"
                + " [Times: user=0.30 sys=0.00, real=0.08 secs]";
        assertTrue(G1MixedPauseEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + ".");
        G1MixedPauseEvent event = new G1MixedPauseEvent(logLine);
        assertEquals((long) 3082652, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(kilobytes(12373197), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(9275802), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(26 * 1024 * 1024), event.getCombinedSpace(),
                "Combined available size not parsed correctly.");
        assertEquals(76206, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(30, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(0, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(8, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(375, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testLogLinePreprocessedNoTriggerWholeSizes() {
        String logLine = "449412.888: [GC pause (mixed), 0.06137400 secs][Eden: 2044M(2044M)->0B(1792M) "
                + "Survivors: 4096K->256M Heap: 2653M(12288M)->435M(12288M)] "
                + "[Times: user=0.43 sys=0.00, real=0.06 secs]";
        assertTrue(G1MixedPauseEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + ".");
        G1MixedPauseEvent event = new G1MixedPauseEvent(logLine);
        assertEquals((long) 449412888, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(kilobytes(2653 * 1024), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(435 * 1024), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(12288 * 1024), event.getCombinedSpace(),
                "Combined available size not parsed correctly.");
        assertEquals(61374, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(43, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(0, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(6, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(717, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testLogLineDatestampTimestamp() {
        String logLine = "2018-03-02T07:08:35.683+0000: 47788.145: [GC pause (G1 Evacuation Pause) (mixed) "
                + "1239M->949M(4096M), 0.0245500 secs]";
        assertTrue(G1MixedPauseEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + ".");
        G1MixedPauseEvent event = new G1MixedPauseEvent(logLine);
        assertEquals((long) 47788145, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(kilobytes(1239 * 1024), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(949 * 1024), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(4096 * 1024), event.getCombinedSpace(), "Combined available size not parsed correctly.");
        assertEquals(24550, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(0, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(0, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(0, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(100, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testLogLineDatestamp() {
        String logLine = "2018-03-02T07:08:35.683+0000: [GC pause (G1 Evacuation Pause) (mixed) "
                + "1239M->949M(4096M), 0.0245500 secs]";
        assertTrue(G1MixedPauseEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + ".");
        G1MixedPauseEvent event = new G1MixedPauseEvent(logLine);
        assertEquals(573271715683L, event.getTimestamp(), "Time stamp not parsed correctly.");
    }

    @Test
    void testLogLinePreprocessedhDatestampTimestamp() {
        String logLine = "2016-02-09T23:27:04.149-0500: 3082.652: [GC pause (mixed), 0.0762060 secs]"
                + "[Eden: 1288.0M(1288.0M)->0.0B(1288.0M) Survivors: 40.0M->40.0M Heap: 11.8G(26.0G)->9058.4M(26.0G)] "
                + "[Times: user=0.30 sys=0.00, real=0.08 secs]";
        assertTrue(G1MixedPauseEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + ".");
        G1MixedPauseEvent event = new G1MixedPauseEvent(logLine);
        assertEquals((long) 3082652, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(kilobytes(12373197), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(9275802), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(26 * 1024 * 1024), event.getCombinedSpace(),
                "Combined available size not parsed correctly.");
        assertEquals(76206, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(30, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(0, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(8, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(375, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testLogLinePreprocessedhDatestamp() {
        String logLine = "2016-02-09T23:27:04.149-0500: [GC pause (mixed), 0.0762060 secs]"
                + "[Eden: 1288.0M(1288.0M)->0.0B(1288.0M) Survivors: 40.0M->40.0M Heap: 11.8G(26.0G)->9058.4M(26.0G)] "
                + "[Times: user=0.30 sys=0.00, real=0.08 secs]";
        assertTrue(G1MixedPauseEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + ".");
        G1MixedPauseEvent event = new G1MixedPauseEvent(logLine);
        assertEquals(508375624149L, event.getTimestamp(), "Time stamp not parsed correctly.");
    }

    @Test
    void testNoTriggerToSpaceExhausted() {
        String logLine = "615375.044: [GC pause (mixed) (to-space exhausted), 1.5026320 secs]"
                + "[Eden: 3416.0M(3416.0M)->0.0B(3464.0M) Survivors: 264.0M->216.0M Heap: 17.7G(18.0G)->17.8G(18.0G)] "
                + "[Times: user=11.35 sys=0.00, real=1.50 secs]";
        assertTrue(G1MixedPauseEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + ".");
        G1MixedPauseEvent event = new G1MixedPauseEvent(logLine);
        assertTrue(event.getTrigger().matches(JdkRegEx.TRIGGER_TO_SPACE_EXHAUSTED), "Trigger not parsed correctly.");
        assertEquals((long) 615375044, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(kilobytes(18559795), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(18664653), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(18 * 1024 * 1024), event.getCombinedSpace(),
                "Combined available size not parsed correctly.");
        assertEquals(1502632, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(1135, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(0, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(150, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(757, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testDoubleTriggerToSpaceExhausted() {
        String logLine = "506146.808: [GC pause (G1 Evacuation Pause) (mixed) (to-space exhausted), 8.6429024 secs]"
                + "[Eden: 22.9G(24.3G)->0.0B(24.3G) Survivors: 112.0M->0.0B Heap: 27.7G(28.0G)->23.5G(28.0G)] "
                + "[Times: user=34.39 sys=13.70, real=8.64 secs]";
        assertTrue(G1MixedPauseEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + ".");
        G1MixedPauseEvent event = new G1MixedPauseEvent(logLine);
        assertTrue(event.getTrigger().matches(JdkRegEx.TRIGGER_TO_SPACE_EXHAUSTED), "Trigger not parsed correctly.");
        assertEquals((long) 506146808, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(kilobytes(29045555), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(24641536), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(28 * 1024 * 1024), event.getCombinedSpace(),
                "Combined available size not parsed correctly.");
        assertEquals(8642902, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(3439, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(1370, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(864, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(557, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testTriggerGcLockerInitiatedGc() {
        String logLine = "55.647: [GC pause (GCLocker Initiated GC) (mixed), 0.0210214 secs][Eden: "
                + "44.0M(44.0M)->0.0B(248.0M) Survivors: 31.0M->10.0M Heap: 1141.0M(1500.0M)->1064.5M(1500.0M)] "
                + "[Times: user=0.07 sys=0.00, real=0.02 secs]";
        assertTrue(G1MixedPauseEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + ".");
        G1MixedPauseEvent event = new G1MixedPauseEvent(logLine);
        assertTrue(event.getTrigger().matches(JdkRegEx.TRIGGER_GCLOCKER_INITIATED_GC), "Trigger not parsed correctly.");
        assertEquals((long) 55647, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(kilobytes(1141 * 1024), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(1090048), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(1500 * 1024), event.getCombinedSpace(), "Combined available size not parsed correctly.");
        assertEquals(21021, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(7, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(0, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(2, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(350, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    /**
     * Test preprocessing TRIGGER_TO_SPACE_EXHAUSTED after "mixed".
     * 
     */
    @Test
    void testPreprocessingTriggerToSpaceExhausted() {
        File testFile = TestUtil.getFile("dataset99.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        assertEquals(1, jvmRun.getEventTypes().size(), "Event type count not correct.");
        assertFalse(jvmRun.getEventTypes().contains(LogEventType.UNKNOWN),
                JdkUtil.LogEventType.UNKNOWN.toString() + " collector identified.");
        assertTrue(jvmRun.getEventTypes().contains(LogEventType.G1_MIXED_PAUSE),
                JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + " collector not identified.");
        assertTrue(jvmRun.getAnalysis().contains(Analysis.ERROR_G1_EVACUATION_FAILURE),
                Analysis.ERROR_G1_EVACUATION_FAILURE + " analysis not identified.");
    }

    /**
     * Test preprocessing TRIGGER_G1_EVACUATION_PAUSE before "mixed" and TRIGGER_TO_SPACE_EXHAUSTED after "mixed".
     * 
     */
    @Test
    void testPreprocessingDoubleTriggerG1EvacuationPauseToSpaceExhausted() {
        File testFile = TestUtil.getFile("dataset102.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        assertEquals(1, jvmRun.getEventTypes().size(), "Event type count not correct.");
        assertFalse(jvmRun.getEventTypes().contains(LogEventType.UNKNOWN),
                JdkUtil.LogEventType.UNKNOWN.toString() + " collector identified.");
        assertTrue(jvmRun.getEventTypes().contains(LogEventType.G1_MIXED_PAUSE),
                JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + " collector not identified.");
        assertTrue(jvmRun.getAnalysis().contains(Analysis.ERROR_G1_EVACUATION_FAILURE),
                Analysis.ERROR_G1_EVACUATION_FAILURE + " analysis not identified.");
    }

    /**
     * Test preprocessing TRIGGER_G1_HUMONGOUS_ALLOCATION before "mixed" and TRIGGER_TO_SPACE_EXHAUSTED after "mixed".
     * 
     */
    @Test
    void testPreprocessingDoubleTriggerHumongousAllocationToSpaceExhausted() {
        File testFile = TestUtil.getFile("dataset133.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        assertEquals(1, jvmRun.getEventTypes().size(), "Event type count not correct.");
        assertFalse(jvmRun.getEventTypes().contains(LogEventType.UNKNOWN),
                JdkUtil.LogEventType.UNKNOWN.toString() + " collector identified.");
        assertTrue(jvmRun.getEventTypes().contains(LogEventType.G1_MIXED_PAUSE),
                JdkUtil.LogEventType.G1_MIXED_PAUSE.toString() + " collector not identified.");
        assertTrue(jvmRun.getAnalysis().contains(Analysis.ERROR_G1_EVACUATION_FAILURE),
                Analysis.ERROR_G1_EVACUATION_FAILURE + " analysis not identified.");
    }
}
