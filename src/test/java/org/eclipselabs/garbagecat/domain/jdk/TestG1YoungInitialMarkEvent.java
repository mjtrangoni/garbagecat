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
class TestG1YoungInitialMarkEvent {

    @Test
    void testIsBlocking() {
        String logLine = "1244.357: [GC pause (young) (initial-mark) 847M->599M(970M), 0.0566840 secs]";
        assertTrue(JdkUtil.isBlocking(JdkUtil.identifyEventType(logLine)),
                JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + " not indentified as blocking.");
    }

    @Test
    void testInitialMark() {
        String logLine = "1244.357: [GC pause (young) (initial-mark) 847M->599M(970M), 0.0566840 secs] "
                + "[Times: user=0.18 sys=0.02, real=0.06 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals((long) 1244357, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(kilobytes(867328), event.getCombinedOccupancyInit(), "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(613376), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(993280), event.getCombinedSpace(), "Combined available size not parsed correctly.");
        assertEquals(56684, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(18, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(2, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(6, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(334, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testNotYoungPause() {
        String logLine = "1113.145: [GC pause (young) 849M->583M(968M), 0.0392710 secs]";
        assertFalse(G1YoungInitialMarkEvent.match(logLine),
                "Log line recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
    }

    @Test
    void testLogLineMetadataGCThresholdTrigger() {
        String logLine = "1.471: [GC pause (Metadata GC Threshold) (young) (initial-mark) 992M->22M(110G), "
                + "0.0210012 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals((long) 1471, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertTrue(event.getTrigger().matches(JdkRegEx.TRIGGER_METADATA_GC_THRESHOLD), "Trigger not parsed correctly.");
        assertEquals(kilobytes(992 * 1024), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(22 * 1024), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(110 * 1024 * 1024), event.getCombinedSpace(),
                "Combined available size not parsed correctly.");
        assertEquals(21001, event.getDuration(), "Duration not parsed correctly.");
    }

    @Test
    void testLogLineGCLockerInitiatedGCTriggerBeforeInitialMark() {
        String logLine = "2.443: [GC pause (GCLocker Initiated GC) (young) (initial-mark) 1061M->52M(110G), "
                + "0.0280096 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals((long) 2443, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertTrue(event.getTrigger().matches(JdkRegEx.TRIGGER_GCLOCKER_INITIATED_GC), "Trigger not parsed correctly.");
        assertEquals(kilobytes(1061 * 1024), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(52 * 1024), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(110 * 1024 * 1024), event.getCombinedSpace(),
                "Combined available size not parsed correctly.");
        assertEquals(28009, event.getDuration(), "Duration not parsed correctly.");
    }

    @Test
    void testLogLineToSpaceExhaustedTriggerAfterInitialMark() {
        String logLine = "60346.050: [GC pause (young) (initial-mark) (to-space exhausted), 1.0224350 secs]"
                + "[Eden: 14.2G(14.5G)->0.0B(1224.0M) Survivors: 40.0M->104.0M Heap: 22.9G(26.0G)->19.2G(26.0G)]"
                + " [Times: user=3.03 sys=0.02, real=1.02 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals((long) 60346050, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertTrue(event.getTrigger().matches(JdkRegEx.TRIGGER_TO_SPACE_EXHAUSTED), "Trigger not parsed correctly.");
        assertEquals(kilobytes(24012390), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(20132659), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(26 * 1024 * 1024), event.getCombinedSpace(),
                "Combined available size not parsed correctly.");
        assertEquals(1022435, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(303, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(2, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(102, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(300, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testLogLineNoTriggerNoInitialMark() {
        String logLine = "44620.073: [GC pause (young), 0.2752700 secs]"
                + "[Eden: 11.3G(11.3G)->0.0B(11.3G) Survivors: 192.0M->176.0M Heap: 23.0G(26.0G)->11.7G(26.0G)]"
                + " [Times: user=1.09 sys=0.00, real=0.27 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals((long) 44620073, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertTrue(event.getTrigger() == null, "Trigger not parsed correctly.");
        assertEquals(kilobytes(23 * 1024 * 1024), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(12268339), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(26 * 1024 * 1024), event.getCombinedSpace(),
                "Combined available size not parsed correctly.");
        assertEquals(275270, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(109, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(0, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(27, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(404, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testLogLineDatestampTimestamp() {
        String logLine = "2021-10-26T09:58:12.086-0400: 123.456: [GC pause (G1 Evacuation Pause) (young) "
                + "(initial-mark) 3879K->2859K(6144K), 0.0019560 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals((long) 123456, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(JdkRegEx.TRIGGER_G1_EVACUATION_PAUSE, event.getTrigger(), "Trigger not parsed correctly.");
        assertEquals(kilobytes(3879), event.getCombinedOccupancyInit(), "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(2859), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(6144), event.getCombinedSpace(), "Combined available size not parsed correctly.");
        assertEquals(1956, event.getDuration(), "Duration not parsed correctly.");
    }

    @Test
    void testLogLineDatestamp() {
        String logLine = "2021-10-26T09:58:12.086-0400: [GC pause (G1 Evacuation Pause) (young) (initial-mark) "
                + "3879K->2859K(6144K), 0.0019560 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals(688553892086L, event.getTimestamp(), "Time stamp not parsed correctly.");
    }

    @Test
    void testLogLinePreprocessedNoTrigger() {
        String logLine = "27474.176: [GC pause (young) (initial-mark), 0.4234530 secs]"
                + "[Eden: 5376.0M(7680.0M)->0.0B(6944.0M) Survivors: 536.0M->568.0M "
                + "Heap: 13.8G(26.0G)->8821.4M(26.0G)] [Times: user=1.66 sys=0.02, real=0.43 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals((long) 27474176, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertTrue(event.getTrigger() == null, "Trigger not parsed correctly.");
        assertEquals(kilobytes(14470349), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(9033114), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(26 * 1024 * 1024), event.getCombinedSpace(),
                "Combined available size not parsed correctly.");
        assertEquals(423453, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(166, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(2, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(43, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(391, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testLogLinePreprocessedTriggerMetadataGcThreshold() {
        String logLine = "87.830: [GC pause (Metadata GC Threshold) (young) (initial-mark), 0.2932700 secs]"
                + "[Eden: 716.0M(1850.0M)->0.0B(1522.0M) Survivors: 96.0M->244.0M "
                + "Heap: 2260.0M(5120.0M)->1831.0M(5120.0M)] [Times: user=0.56 sys=0.04, real=0.29 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals((long) 87830, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertTrue(event.getTrigger().matches(JdkRegEx.TRIGGER_METADATA_GC_THRESHOLD), "Trigger not parsed correctly.");
        assertEquals(kilobytes(2260 * 1024), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(1831 * 1024), event.getCombinedOccupancyEnd(),
                "Combined end size not parsed correctly.");
        assertEquals(kilobytes(5120 * 1024), event.getCombinedSpace(), "Combined available size not parsed correctly.");
        assertEquals(293270, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(56, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(29, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(207, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testLogLinePreprocessedTriggerGcLockerInitiatedGc() {
        String logLine = "6896.482: [GC pause (GCLocker Initiated GC) (young) (initial-mark), 0.0525160 secs]"
                + "[Eden: 16.0M(3072.0M)->0.0B(3070.0M) Survivors: 0.0B->2048.0K "
                + "Heap: 828.8M(5120.0M)->814.8M(5120.0M)] [Times: user=0.09 sys=0.00, real=0.05 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals((long) 6896482, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertTrue(event.getTrigger().matches(JdkRegEx.TRIGGER_GCLOCKER_INITIATED_GC), "Trigger not parsed correctly.");
        assertEquals(kilobytes(848691), event.getCombinedOccupancyInit(), "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(834355), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(5120 * 1024), event.getCombinedSpace(), "Combined available size not parsed correctly.");
        assertEquals(52516, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(9, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(0, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(5, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(180, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testLogLinePreprocessedTriggerG1HumongousAllocation() {
        String logLine = "182.037: [GC pause (G1 Humongous Allocation) (young) (initial-mark), 0.0233585 secs]"
                + "[Eden: 424.0M(1352.0M)->0.0B(1360.0M) Survivors: 80.0M->72.0M Heap: 500.9M(28.0G)->72.0M(28.0G)] "
                + "[Times: user=0.14 sys=0.01, real=0.02 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals((long) 182037, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertTrue(event.getTrigger().matches(JdkRegEx.TRIGGER_G1_HUMONGOUS_ALLOCATION),
                "Trigger not parsed correctly.");
        assertEquals(kilobytes(512922), event.getCombinedOccupancyInit(), "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(72 * 1024), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(28 * 1024 * 1024), event.getCombinedSpace(),
                "Combined available size not parsed correctly.");
        assertEquals(23358, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(14, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(1, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(2, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(750, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testLogLinePreprocessedTriggerSystemGc() {
        String logLine = "2020-02-26T17:18:26.505+0000: 130.241: [GC pause (System.gc()) (young) (initial-mark), "
                + "0.1009346 secs][Eden: 220.0M(241.0M)->0.0B(277.0M) Survivors: 28.0M->34.0M "
                + "Heap: 924.5M(2362.0M)->713.5M(2362.0M)] [Times: user=0.19 sys=0.00, real=0.10 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals((long) 130241, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertTrue(event.getTrigger().matches(JdkRegEx.TRIGGER_SYSTEM_GC), "Trigger not parsed correctly.");
        assertEquals(kilobytes(946688), event.getCombinedOccupancyInit(), "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(730624), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(2362 * 1024), event.getCombinedSpace(), "Combined available size not parsed correctly.");
        assertEquals(100934, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(19, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(10, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(190, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testLogLinePreprocessedNoTriggerWholeNumberSizes() {
        String logLine = "449391.255: [GC pause (young) (initial-mark), 0.02147900 secs]"
                + "[Eden: 1792M(1792M)->0B(2044M) Survivors: 256M->4096K Heap: 7582M(12288M)->5537M(12288M)] "
                + "[Times: user=0.13 sys=0.00, real=0.02 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals((long) 449391255, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(kilobytes(7582 * 1024), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(5537 * 1024), event.getCombinedOccupancyEnd(),
                "Combined end size not parsed correctly.");
        assertEquals(kilobytes(12288 * 1024), event.getCombinedSpace(),
                "Combined available size not parsed correctly.");
        assertEquals(21479, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(13, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(0, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(2, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(650, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testLogLinePreprocessedDatestampTimestamp() {
        String logLine = "2016-02-09T06:12:45.414-0500: 27474.176: [GC pause (young) (initial-mark), 0.4234530 secs]"
                + "[Eden: 5376.0M(7680.0M)->0.0B(6944.0M) Survivors: 536.0M->568.0M "
                + "Heap: 13.8G(26.0G)->8821.4M(26.0G)] [Times: user=1.66 sys=0.02, real=0.43 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals((long) 27474176, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(kilobytes(14470349), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(9033114), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(26 * 1024 * 1024), event.getCombinedSpace(),
                "Combined available size not parsed correctly.");
        assertEquals(423453, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(166, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(43, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(391, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testLogLinePreprocessedDatestamp() {
        String logLine = "2016-02-09T06:12:45.414-0500: [GC pause (young) (initial-mark), 0.4234530 secs]"
                + "[Eden: 5376.0M(7680.0M)->0.0B(6944.0M) Survivors: 536.0M->568.0M "
                + "Heap: 13.8G(26.0G)->8821.4M(26.0G)] [Times: user=1.66 sys=0.02, real=0.43 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals(508313565414L, event.getTimestamp(), "Time stamp not parsed correctly.");
    }

    @Test
    void testLogLinePreprocessedTriggerG1HumongousAllocationNoSizeData() {
        String logLine = "2017-02-20T20:17:04.874-0500: 40442.077: [GC pause (G1 Humongous Allocation) (young) "
                + "(initial-mark), 0.0142482 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals((long) 40442077, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(kilobytes(0), event.getCombinedOccupancyInit(), "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(0), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(0), event.getCombinedSpace(), "Combined available size not parsed correctly.");
        assertEquals(14248, event.getDuration(), "Duration not parsed correctly.");
    }

    @Test
    void testLogLinePreprocessedNoDuration() {
        String logLine = "2017-06-23T10:50:04.403-0400: 9.915: [GC pause (Metadata GC Threshold) (young) "
                + "(initial-mark)[Eden: 304.0M(1552.0M)->0.0B(1520.0M) Survivors: 0.0B->32.0M Heap: "
                + "296.0M(30.5G)->23.2M(30.5G)] [Times: user=0.12 sys=0.01, real=0.03 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals((long) 9915, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(kilobytes(296 * 1024), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(23757), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(31981568), event.getCombinedSpace(), "Combined available size not parsed correctly.");
        assertEquals(10000, event.getDuration(), "Duration not parsed correctly.");
    }

    @Test
    void testTriggerG1EvacuationPause() {
        String logLine = "7.190: [GC pause (G1 Evacuation Pause) (young) (initial-mark) 407M->100M(8192M), "
                + "0.0720459 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals((long) 7190, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertTrue(event.getTrigger().matches(JdkRegEx.TRIGGER_G1_EVACUATION_PAUSE), "Trigger not parsed correctly.");
        assertEquals(kilobytes(407 * 1024), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(100 * 1024), event.getCombinedOccupancyEnd(), "Combined end size not parsed correctly.");
        assertEquals(kilobytes(8192 * 1024), event.getCombinedSpace(), "Combined available size not parsed correctly.");
        assertEquals(72045, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(0, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(0, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(0, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(100, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testTriggerG1EvacuationPauseDashDash() {
        String logLine = "424753.803: [GC pause (G1 Evacuation Pause) (young) (initial-mark)-- 8184M->8184M(8192M), "
                + "0.1294400 secs]";
        assertTrue(G1YoungInitialMarkEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        G1YoungInitialMarkEvent event = new G1YoungInitialMarkEvent(logLine);
        assertEquals((long) 424753803, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertTrue(event.getTrigger().matches(JdkRegEx.TRIGGER_G1_EVACUATION_PAUSE), "Trigger not parsed correctly.");
        assertEquals(kilobytes(8184 * 1024), event.getCombinedOccupancyInit(),
                "Combined begin size not parsed correctly.");
        assertEquals(kilobytes(8184 * 1024), event.getCombinedOccupancyEnd(),
                "Combined end size not parsed correctly.");
        assertEquals(kilobytes(8192 * 1024), event.getCombinedSpace(), "Combined available size not parsed correctly.");
        assertEquals(129440, event.getDuration(), "Duration not parsed correctly.");
        assertEquals(0, event.getTimeUser(), "User time not parsed correctly.");
        assertEquals(0, event.getTimeSys(), "Sys time not parsed correctly.");
        assertEquals(0, event.getTimeReal(), "Real time not parsed correctly.");
        assertEquals(100, event.getParallelism(), "Parallelism not calculated correctly.");
    }

    @Test
    void testAnalysisExplicitGc() {
        File testFile = TestUtil.getFile("dataset179.txt");
        GcManager gcManager = new GcManager();
        File preprocessedFile = gcManager.preprocess(testFile, null);
        gcManager.store(preprocessedFile, false);
        JvmRun jvmRun = gcManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        assertEquals(1, jvmRun.getEventTypes().size(), "Event type count not correct.");
        assertFalse(jvmRun.getEventTypes().contains(LogEventType.UNKNOWN),
                JdkUtil.LogEventType.UNKNOWN.toString() + " collector identified.");
        assertTrue(jvmRun.getEventTypes().contains(LogEventType.G1_YOUNG_INITIAL_MARK),
                "Log line not recognized as " + LogEventType.G1_YOUNG_INITIAL_MARK.toString() + ".");
        assertFalse(jvmRun.getAnalysis().contains(Analysis.ERROR_EXPLICIT_GC_SERIAL_G1),
                Analysis.ERROR_EXPLICIT_GC_SERIAL_G1 + " analysis incorrectly identified.");
        assertTrue(jvmRun.getAnalysis().contains(Analysis.WARN_EXPLICIT_GC_G1_YOUNG_INITIAL_MARK),
                Analysis.WARN_EXPLICIT_GC_G1_YOUNG_INITIAL_MARK + " analysis not identified.");

    }
}
