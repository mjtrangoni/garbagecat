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

import static org.eclipselabs.garbagecat.util.Memory.memory;
import static org.eclipselabs.garbagecat.util.Memory.Unit.KILOBYTES;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipselabs.garbagecat.domain.BlockingEvent;
import org.eclipselabs.garbagecat.domain.CombinedData;
import org.eclipselabs.garbagecat.domain.ParallelEvent;
import org.eclipselabs.garbagecat.domain.TimesData;
import org.eclipselabs.garbagecat.domain.TriggerData;
import org.eclipselabs.garbagecat.util.Memory;
import org.eclipselabs.garbagecat.util.jdk.JdkMath;
import org.eclipselabs.garbagecat.util.jdk.JdkRegEx;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil;

/**
 * <p>
 * G1_YOUNG_INITIAL_MARK
 * </p>
 * 
 * <p>
 * G1 collector young generation initial marking. Piggybacks a G1_YOUNG_PAUSE collection.
 * </p>
 * 
 * <h3>Example Logging</h3>
 * 
 * <p>
 * 1) Standard format:
 * </p>
 * 
 * <pre>
 * 1.305: [GC pause (young) (initial-mark) 102M-&gt;24M(512M), 0.0254200 secs]
 * </pre>
 * 
 * <p>
 * 2) With -XX:+PrintGCDateStamps:
 * </p>
 * 
 * <pre>
 * 2010-02-26T08:31:51.990-0600: [GC pause (young) (initial-mark) 102M-&gt;24M(512M), 0.0254200 secs]
 * </pre>
 * 
 * <p>
 * 3) Preprocessed:
 * </p>
 * 
 * <pre>
 * 2970.268: [GC pause (G1 Evacuation Pause) (young) (initial-mark), 0.0698627 secs][Eden: 112.0M(112.0M)-&gt;0.0B(112.0M) Survivors: 16.0M-&gt;16.0M Heap: 13.6G(30.0G)-&gt;13.5G(30.0G)] [Times: user=0.28 sys=0.00, real=0.08 secs]
 * </pre>
 * 
 * @author <a href="mailto:mmillson@redhat.com">Mike Millson</a>
 * @author James Livingston
 * 
 */
public class G1YoungInitialMarkEvent extends G1Collector
        implements BlockingEvent, CombinedData, TriggerData, ParallelEvent, TimesData {

    /**
     * Regular expressions defining the logging.
     * 
     * 1244.357: [GC pause (young) (initial-mark) 847M->599M(970M), 0.0566840 secs]
     */
    private static final String REGEX = "^" + JdkRegEx.DECORATOR + " \\[GC pause (\\(("
            + JdkRegEx.TRIGGER_METADATA_GC_THRESHOLD + "|" + JdkRegEx.TRIGGER_GCLOCKER_INITIATED_GC + "|"
            + JdkRegEx.TRIGGER_G1_HUMONGOUS_ALLOCATION + "|" + JdkRegEx.TRIGGER_G1_EVACUATION_PAUSE
            + ")\\) )?\\(young\\) \\(initial-mark\\)(--)? " + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE + "\\("
            + JdkRegEx.SIZE + "\\), " + JdkRegEx.DURATION + "\\]" + TimesData.REGEX + "?[ ]*$";

    private static final Pattern REGEX_PATTERN = Pattern.compile(REGEX);

    /**
     * Regular expression preprocessed.
     * 
     * 27474.176: [GC pause (young) (initial-mark), 0.4234530 secs][Eden: 5376.0M(7680.0M)->0.0B(6944.0M) Survivors:
     * 536.0M->568.0M Heap: 13.8G(26.0G)->8821.4M(26.0G)] [Times: user=1.66 sys=0.02, real=0.43 secs]
     * 
     * 2017-02-20T20:17:04.874-0500: 40442.077: [GC pause (G1 Humongous Allocation) (young) (initial-mark), 0.0142482
     * secs]
     */
    private static final String REGEX_PREPROCESSED = "^" + JdkRegEx.DECORATOR + " \\[GC pause (\\(("
            + JdkRegEx.TRIGGER_G1_EVACUATION_PAUSE + "|" + JdkRegEx.TRIGGER_METADATA_GC_THRESHOLD + "|"
            + JdkRegEx.TRIGGER_GCLOCKER_INITIATED_GC + "|" + JdkRegEx.TRIGGER_G1_HUMONGOUS_ALLOCATION + "|"
            + JdkRegEx.TRIGGER_SYSTEM_GC + ")\\) )?\\(young\\)( \\(initial-mark\\))?( \\(("
            + JdkRegEx.TRIGGER_TO_SPACE_EXHAUSTED + ")\\))?(, " + JdkRegEx.DURATION + "\\])?(\\[Eden: " + JdkRegEx.SIZE
            + "\\(" + JdkRegEx.SIZE + "\\)->" + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\) Survivors: "
            + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE + " Heap: " + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\)->"
            + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\)\\]" + TimesData.REGEX + "?)?[ ]*$";

    private static final Pattern REGEX_PREPROCESSED_PATTERN = Pattern.compile(REGEX_PREPROCESSED);

    /**
     * The log entry for the event. Can be used for debugging purposes.
     */
    private String logEntry;

    /**
     * The elapsed clock time for the GC event in microseconds (rounded).
     */
    private long duration;

    /**
     * The time when the GC event started in milliseconds after JVM startup.
     */
    private long timestamp;

    /**
     * Combined generation size at beginning of GC event.
     */
    private Memory combined = Memory.ZERO;

    /**
     * Combined generation size at end of GC event.
     */
    private Memory combinedEnd = Memory.ZERO;

    /**
     * Available space in multiple generation.
     */
    private Memory combinedAvailable = Memory.ZERO;

    /**
     * The trigger for the GC event.
     */
    private String trigger;

    /**
     * The time of all user (non-kernel) threads added together in centiseconds.
     */
    private int timeUser;

    /**
     * The time of all system (kernel) threads added together in centiseconds.
     */
    private int timeSys;

    /**
     * The wall (clock) time in centiseconds.
     */
    private int timeReal;

    /**
     * Create event from log entry.
     * 
     * @param logEntry
     *            The log entry for the event.
     */
    public G1YoungInitialMarkEvent(String logEntry) {
        this.logEntry = logEntry;

        Matcher matcher;

        if ((matcher = REGEX_PATTERN.matcher(logEntry)).matches()) {
            // standard format
            matcher.reset();
            if (matcher.find()) {
                if (matcher.group(13) != null && matcher.group(13).matches(JdkRegEx.TIMESTAMP)) {
                    timestamp = JdkMath.convertSecsToMillis(matcher.group(13)).longValue();
                } else if (matcher.group(1).matches(JdkRegEx.TIMESTAMP)) {
                    timestamp = JdkMath.convertSecsToMillis(matcher.group(1)).longValue();
                } else {
                    // Datestamp only.
                    timestamp = JdkUtil.convertDatestampToMillis(matcher.group(1));
                }
                trigger = matcher.group(15);
                combined = memory(matcher.group(17), matcher.group(19).charAt(0)).convertTo(KILOBYTES);
                combinedEnd = memory(matcher.group(20), matcher.group(22).charAt(0)).convertTo(KILOBYTES);
                combinedAvailable = memory(matcher.group(23), matcher.group(25).charAt(0)).convertTo(KILOBYTES);
                duration = JdkMath.convertSecsToMicros(matcher.group(26)).intValue();
                if (matcher.group(29) != null) {
                    timeUser = JdkMath.convertSecsToCentis(matcher.group(30)).intValue();
                    timeSys = JdkMath.convertSecsToCentis(matcher.group(31)).intValue();
                    timeReal = JdkMath.convertSecsToCentis(matcher.group(32)).intValue();
                }
            }
        } else if ((matcher = REGEX_PREPROCESSED_PATTERN.matcher(logEntry)).matches()) {
            // preprocessed format
            matcher.reset();
            if (matcher.find()) {
                if (matcher.group(13) != null && matcher.group(13).matches(JdkRegEx.TIMESTAMP)) {
                    timestamp = JdkMath.convertSecsToMillis(matcher.group(13)).longValue();
                } else if (matcher.group(1).matches(JdkRegEx.TIMESTAMP)) {
                    timestamp = JdkMath.convertSecsToMillis(matcher.group(1)).longValue();
                } else {
                    // Datestamp only.
                    timestamp = JdkUtil.convertDatestampToMillis(matcher.group(1));
                }
                if (matcher.group(15) != null) {
                    trigger = matcher.group(15);
                } else if (matcher.group(19) != null) {
                    trigger = matcher.group(19);
                }
                if (matcher.group(20) != null) {
                    duration = JdkMath.convertSecsToMicros(matcher.group(21)).intValue();
                } else {
                    if (matcher.group(55) != null) {
                        // Use Times block duration
                        duration = JdkMath.convertSecsToMicros(matcher.group(57)).intValue();
                    }
                }
                if (matcher.group(24) != null) {
                    combined = JdkMath.convertSizeToKilobytes(matcher.group(43), matcher.group(45).charAt(0));
                    combinedEnd = JdkMath.convertSizeToKilobytes(matcher.group(49), matcher.group(51).charAt(0));
                    combinedAvailable = JdkMath.convertSizeToKilobytes(matcher.group(52), matcher.group(54).charAt(0));
                }
                if (matcher.group(55) != null) {
                    timeUser = JdkMath.convertSecsToCentis(matcher.group(56)).intValue();
                    timeSys = JdkMath.convertSecsToCentis(matcher.group(57)).intValue();
                    timeReal = JdkMath.convertSecsToCentis(matcher.group(58)).intValue();
                }
            }
        }
    }

    /**
     * Alternate constructor. Create detail logging event from values.
     * 
     * @param logEntry
     *            The log entry for the event.
     * @param timestamp
     *            The time when the GC event started in milliseconds after JVM startup.
     * @param duration
     *            The elapsed clock time for the GC event in microseconds.
     */
    public G1YoungInitialMarkEvent(String logEntry, long timestamp, int duration) {
        this.logEntry = logEntry;
        this.timestamp = timestamp;
        this.duration = duration;
    }

    public String getLogEntry() {
        return logEntry;
    }

    public long getDuration() {
        return duration;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Memory getCombinedOccupancyInit() {
        return combined;
    }

    public Memory getCombinedOccupancyEnd() {
        return combinedEnd;
    }

    public Memory getCombinedSpace() {
        return combinedAvailable;
    }

    public String getName() {
        return JdkUtil.LogEventType.G1_YOUNG_INITIAL_MARK.toString();
    }

    public String getTrigger() {
        return trigger;
    }

    public int getTimeUser() {
        return timeUser;
    }

    public int getTimeSys() {
        return timeSys;
    }

    public int getTimeReal() {
        return timeReal;
    }

    public int getParallelism() {
        return JdkMath.calcParallelism(timeUser, timeSys, timeReal);
    }

    /**
     * Determine if the logLine matches the logging pattern(s) for this event.
     * 
     * @param logLine
     *            The log line to test.
     * @return true if the log line matches the event pattern, false otherwise.
     */
    public static final boolean match(String logLine) {
        return REGEX_PATTERN.matcher(logLine).matches() || REGEX_PREPROCESSED_PATTERN.matcher(logLine).matches();
    }
}
