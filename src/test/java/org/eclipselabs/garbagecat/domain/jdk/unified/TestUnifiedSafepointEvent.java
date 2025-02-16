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
package org.eclipselabs.garbagecat.domain.jdk.unified;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipselabs.garbagecat.util.jdk.JdkUtil;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil.LogEventType;
import org.eclipselabs.garbagecat.util.jdk.unified.UnifiedSafepoint.Trigger;
import org.eclipselabs.garbagecat.util.jdk.unified.UnifiedUtil;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:mmillson@redhat.com">Mike Millson</a>
 * 
 */
class TestUnifiedSafepointEvent {

    @Test
    void testIdentityEventType() {
        String logLine = "[2021-09-14T11:40:53.379-0500][144.035s][info][safepoint     ] Entering safepoint region: "
                + "CollectForMetadataAllocation[2021-09-14T11:40:53.379-0500][144.036s][info][safepoint     ] "
                + "Leaving safepoint region[2021-09-14T11:40:53.379-0500][144.036s][info][safepoint     ] Total time "
                + "for which application threads were stopped: 0.0004546 seconds, Stopping threads took: 0.0002048 "
                + "seconds";
        assertEquals(JdkUtil.LogEventType.UNIFIED_SAFEPOINT, JdkUtil.identifyEventType(logLine),
                JdkUtil.LogEventType.UNIFIED_SAFEPOINT + "not identified.");
    }

    @Test
    void testJdk11Time() {
        String logLine = "[2021-09-14T11:40:53.379-0500][info][safepoint     ] Entering safepoint region: "
                + "CollectForMetadataAllocation[2021-09-14T11:40:53.379-0500][info][safepoint     ] "
                + "Leaving safepoint region[2021-09-14T11:40:53.379-0500][info][safepoint     ] Total time for which "
                + "application threads were stopped: 0.0004546 seconds, Stopping threads took: 0.0002048 seconds";
        assertTrue(UnifiedSafepointEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_SAFEPOINT.toString() + ".");
        UnifiedSafepointEvent event = new UnifiedSafepointEvent(logLine);
        assertEquals(Trigger.COLLECT_FOR_METADATA_ALLOCATION, event.getTrigger(), "Trigger not parsed correctly.");
        assertEquals(684934853379L, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(204, event.getTimeToStopThreads(), "Time to stop threads not parsed correctly.");
        assertEquals(454, event.getTimeThreadsStopped(), "Time threads stopped not parsed correctly.");
    }

    @Test
    void testJdk11TimeUptime() {
        String logLine = "[2021-09-14T11:40:53.379-0500][144.035s][info][safepoint     ] Entering safepoint region: "
                + "CollectForMetadataAllocation[2021-09-14T11:40:53.379-0500][144.036s][info][safepoint     ] "
                + "Leaving safepoint region[2021-09-14T11:40:53.379-0500][144.036s][info][safepoint     ] Total time "
                + "for which application threads were stopped: 0.0004546 seconds, Stopping threads took: 0.0002048 "
                + "seconds";
        assertTrue(UnifiedSafepointEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_SAFEPOINT.toString() + ".");
        UnifiedSafepointEvent event = new UnifiedSafepointEvent(logLine);
        assertEquals(Trigger.COLLECT_FOR_METADATA_ALLOCATION, event.getTrigger(), "Trigger not parsed correctly.");
        assertEquals(144035, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(204, event.getTimeToStopThreads(), "Time to stop threads not parsed correctly.");
        assertEquals(454, event.getTimeThreadsStopped(), "Time threads stopped not parsed correctly.");
    }

    @Test
    void testJdk11TimeUptimeMillis() {
        String logLine = "[2021-09-14T11:40:53.379-0500][144035ms][info][safepoint     ] Entering safepoint region: "
                + "CollectForMetadataAllocation[2021-09-14T11:40:53.379-0500][144036ms][info][safepoint     ] "
                + "Leaving safepoint region[2021-09-14T11:40:53.379-0500][144036ms][info][safepoint     ] Total time "
                + "for which application threads were stopped: 0.0004546 seconds, Stopping threads took: 0.0002048 "
                + "seconds";
        assertTrue(UnifiedSafepointEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_SAFEPOINT.toString() + ".");
        UnifiedSafepointEvent event = new UnifiedSafepointEvent(logLine);
        assertEquals(Trigger.COLLECT_FOR_METADATA_ALLOCATION, event.getTrigger(), "Trigger not parsed correctly.");
        assertEquals(144035, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(204, event.getTimeToStopThreads(), "Time to stop threads not parsed correctly.");
        assertEquals(454, event.getTimeThreadsStopped(), "Time threads stopped not parsed correctly.");
    }

    @Test
    void testJdk11Uptime() {
        String logLine = "[144.035s][info][safepoint     ] Entering safepoint region: CollectForMetadataAllocation"
                + "[144.036s][info][safepoint     ] Leaving safepoint region[144.036s][info][safepoint     ] Total "
                + "time for which application threads were stopped: 0.0004546 seconds, Stopping threads took: "
                + "0.0002048 seconds";
        assertTrue(UnifiedSafepointEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_SAFEPOINT.toString() + ".");
        UnifiedSafepointEvent event = new UnifiedSafepointEvent(logLine);
        assertEquals(Trigger.COLLECT_FOR_METADATA_ALLOCATION, event.getTrigger(), "Trigger not parsed correctly.");
        assertEquals(144035, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(204, event.getTimeToStopThreads(), "Time to stop threads not parsed correctly.");
        assertEquals(454, event.getTimeThreadsStopped(), "Time threads stopped not parsed correctly.");
    }

    @Test
    void testJdk11UptimeMillis() {
        String logLine = "[144035ms][info][safepoint     ] Entering safepoint region: CollectForMetadataAllocation"
                + "[144036ms][info][safepoint     ] Leaving safepoint region[144036ms][info][safepoint     ] Total "
                + "time for which application threads were stopped: 0.0004546 seconds, Stopping threads took: "
                + "0.0002048 seconds";
        assertTrue(UnifiedSafepointEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_SAFEPOINT.toString() + ".");
        UnifiedSafepointEvent event = new UnifiedSafepointEvent(logLine);
        assertEquals(Trigger.COLLECT_FOR_METADATA_ALLOCATION, event.getTrigger(), "Trigger not parsed correctly.");
        assertEquals(144035, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(204, event.getTimeToStopThreads(), "Time to stop threads not parsed correctly.");
        assertEquals(454, event.getTimeThreadsStopped(), "Time threads stopped not parsed correctly.");
    }

    @Test
    void testJdk17CleanClassLoaderDataMetaspaces() {
        String logLine = "[0.192s][info][safepoint   ] Safepoint \"CleanClassLoaderDataMetaspaces\", Time since last: "
                + "1223019 ns, Reaching safepoint: 138450 ns, At safepoint: 73766 ns, Total: 212216 ns";
        assertTrue(UnifiedSafepointEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_SAFEPOINT.toString() + ".");
        UnifiedSafepointEvent event = new UnifiedSafepointEvent(logLine);
        assertEquals(Trigger.CLEAN_CLASSLOADER_DATA_METASPACES, event.getTrigger(), "Trigger not parsed correctly.");
        assertEquals(192, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(138, event.getTimeToStopThreads(), "Time to stop threads not parsed correctly.");
        assertEquals(73, event.getTimeThreadsStopped(), "Time threads stopped not parsed correctly.");
    }

    @Test
    void testJdk17G1Concurrent() {
        String logLine = "[0.064s][info][safepoint   ] Safepoint \"G1Concurrent\", Time since last: 1666947 ns, "
                + "Reaching safepoint: 79150 ns, At safepoint: 349999 ns, Total: 429149 ns";
        assertTrue(UnifiedSafepointEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_SAFEPOINT.toString() + ".");
        UnifiedSafepointEvent event = new UnifiedSafepointEvent(logLine);
        assertEquals(Trigger.G1_CONCURRENT, event.getTrigger(), "Trigger not parsed correctly.");
        assertEquals(64, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(79, event.getTimeToStopThreads(), "Time to stop threads not parsed correctly.");
        assertEquals(349, event.getTimeThreadsStopped(), "Time threads stopped not parsed correctly.");
    }

    @Test
    void testJdk17Uptime() {
        String logLine = "[0.061s][info][safepoint   ] Safepoint \"GenCollectForAllocation\", Time since last: "
                + "24548411 ns, Reaching safepoint: 69521 ns, At safepoint: 779732 ns, Total: 849253 ns";
        assertTrue(UnifiedSafepointEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_SAFEPOINT.toString() + ".");
        UnifiedSafepointEvent event = new UnifiedSafepointEvent(logLine);
        assertEquals(Trigger.GEN_COLLECT_FOR_ALLOCATION, event.getTrigger(), "Trigger not parsed correctly.");
        assertEquals(61, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(69, event.getTimeToStopThreads(), "Time to stop threads not parsed correctly.");
        assertEquals(779, event.getTimeThreadsStopped(), "Time threads stopped not parsed correctly.");
    }

    @Test
    void testJdk17ZMarkEnd() {
        String logLine = "[0.129s] Safepoint \"ZMarkEnd\", Time since last: 4051145 ns, Reaching safepoint: 79105 ns, "
                + "At safepoint: 16082 ns, Total: 95187 ns";
        assertTrue(UnifiedSafepointEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_SAFEPOINT.toString() + ".");
        UnifiedSafepointEvent event = new UnifiedSafepointEvent(logLine);
        assertEquals(Trigger.Z_MARK_END, event.getTrigger(), "Trigger not parsed correctly.");
        assertEquals(129, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(79, event.getTimeToStopThreads(), "Time to stop threads not parsed correctly.");
        assertEquals(16, event.getTimeThreadsStopped(), "Time threads stopped not parsed correctly.");
    }

    @Test
    void testJdk17ZMarkStart() {
        String logLine = "[0.124s][info][safepoint   ] Safepoint \"ZMarkStart\", Time since last: 103609844 ns, "
                + "Reaching safepoint: 99888 ns, At safepoint: 30677 ns, Total: 130565 ns";
        assertTrue(UnifiedSafepointEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_SAFEPOINT.toString() + ".");
        UnifiedSafepointEvent event = new UnifiedSafepointEvent(logLine);
        assertEquals(Trigger.Z_MARK_START, event.getTrigger(), "Trigger not parsed correctly.");
        assertEquals(124, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(99, event.getTimeToStopThreads(), "Time to stop threads not parsed correctly.");
        assertEquals(30, event.getTimeThreadsStopped(), "Time threads stopped not parsed correctly.");
    }

    @Test
    void testJdk17ZRelocateStart() {
        String logLine = "[0.132s] Safepoint \"ZRelocateStart\", Time since last: 1366138 ns, Reaching safepoint: "
                + "138018 ns, At safepoint: 15653 ns, Total: 153671 ns";
        assertTrue(UnifiedSafepointEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_SAFEPOINT.toString() + ".");
        UnifiedSafepointEvent event = new UnifiedSafepointEvent(logLine);
        assertEquals(Trigger.Z_RELOCATE_START, event.getTrigger(), "Trigger not parsed correctly.");
        assertEquals(132, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(138, event.getTimeToStopThreads(), "Time to stop threads not parsed correctly.");
        assertEquals(15, event.getTimeThreadsStopped(), "Time threads stopped not parsed correctly.");
    }

    @Test
    void testLogLine() {
        String logLine = "[2021-09-14T11:40:53.379-0500][144.035s][info][safepoint     ] Entering safepoint region: "
                + "CollectForMetadataAllocation[2021-09-14T11:40:53.379-0500][144.036s][info][safepoint     ] "
                + "Leaving safepoint region[2021-09-14T11:40:53.379-0500][144.036s][info][safepoint     ] Total time "
                + "for which application threads were stopped: 0.0004546 seconds, Stopping threads took: 0.0002048 "
                + "seconds";
        assertTrue(UnifiedSafepointEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_SAFEPOINT.toString() + ".");
        UnifiedSafepointEvent event = new UnifiedSafepointEvent(logLine);
        assertEquals(Trigger.COLLECT_FOR_METADATA_ALLOCATION, event.getTrigger(), "Trigger not parsed correctly.");
        assertEquals(144035, event.getTimestamp(), "Time stamp not parsed correctly.");
        assertEquals(204, event.getTimeToStopThreads(), "Time to stop threads not parsed correctly.");
        assertEquals(454, event.getTimeThreadsStopped(), "Time threads stopped not parsed correctly.");
    }

    @Test
    void testNotBlocking() {
        String logLine = "[2021-09-14T11:40:53.379-0500][144.035s][info][safepoint     ] Entering safepoint region: "
                + "CollectForMetadataAllocation[2021-09-14T11:40:53.379-0500][144.036s][info][safepoint     ] "
                + "Leaving safepoint region[2021-09-14T11:40:53.379-0500][144.036s][info][safepoint     ] Total time "
                + "for which application threads were stopped: 0.0004546 seconds, Stopping threads took: 0.0002048 "
                + "seconds";
        assertFalse(JdkUtil.isBlocking(JdkUtil.identifyEventType(logLine)),
                JdkUtil.LogEventType.UNIFIED_SAFEPOINT.toString() + " incorrectly indentified as blocking.");
    }

    @Test
    void testParseLogLine() {
        String logLine = "[2021-09-14T11:40:53.379-0500][144.035s][info][safepoint     ] Entering safepoint region: "
                + "CollectForMetadataAllocation[2021-09-14T11:40:53.379-0500][144.036s][info][safepoint     ] "
                + "Leaving safepoint region[2021-09-14T11:40:53.379-0500][144.036s][info][safepoint     ] Total time "
                + "for which application threads were stopped: 0.0004546 seconds, Stopping threads took: 0.0002048 "
                + "seconds";
        assertTrue(JdkUtil.parseLogLine(logLine) instanceof UnifiedSafepointEvent,
                JdkUtil.LogEventType.UNIFIED_SAFEPOINT.toString() + " not parsed.");
    }

    @Test
    void testReportable() {
        assertFalse(JdkUtil.isReportable(JdkUtil.LogEventType.UNIFIED_SAFEPOINT),
                JdkUtil.LogEventType.UNIFIED_SAFEPOINT.toString() + " incorrectly indentified as reportable.");
    }

    @Test
    void testUnified() {
        List<LogEventType> eventTypes = new ArrayList<LogEventType>();
        eventTypes.add(LogEventType.UNIFIED_SAFEPOINT);
        assertTrue(UnifiedUtil.isUnifiedLogging(eventTypes),
                JdkUtil.LogEventType.UNIFIED_SAFEPOINT.toString() + " not indentified as unified.");
    }

    @Test
    void testWithSpacesAtEnd() {
        String logLine = "[2021-09-14T11:40:53.379-0500][144.035s][info][safepoint     ] Entering safepoint region: "
                + "CollectForMetadataAllocation[2021-09-14T11:40:53.379-0500][144.036s][info][safepoint     ] "
                + "Leaving safepoint region[2021-09-14T11:40:53.379-0500][144.036s][info][safepoint     ] Total time "
                + "for which application threads were stopped: 0.0004546 seconds, Stopping threads took: 0.0002048 "
                + "seconds   ";
        assertTrue(UnifiedSafepointEvent.match(logLine),
                "Log line not recognized as " + JdkUtil.LogEventType.UNIFIED_SAFEPOINT.toString() + ".");
    }
}
