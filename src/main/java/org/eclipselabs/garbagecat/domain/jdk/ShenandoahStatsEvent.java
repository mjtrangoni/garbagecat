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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipselabs.garbagecat.domain.ThrowAwayEvent;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil;
import org.eclipselabs.garbagecat.util.jdk.unified.UnifiedRegEx;

/**
 * <p>
 * SHENANDOAH_STATS
 * </p>
 * 
 * <p>
 * Output from XX:+PrintGCDetails (JDK8 extended logs) or -Xlog:gc+stats (JDK11 extended statistics).
 * </p>
 * 
 * <h3>Example Logging</h3>
 * 
 * <pre>
 * All times are wall-clock times, except per-root-class counters, that are sum over
 * all workers. Dividing the &lt;total&gt; over the root stage time estimates parallelism.
 *   Update Region States              789 us
 *     S: &lt;total&gt;                    69130 us
 *     S: JNI Handles Roots              7 us, workers (us): ---,   7, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---,
 *     S: JFR Weak Roots                 1 us, workers (us): ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---,   1, ---, ---, ---, ---, ---, ---, ---,
 *     S: Flat Profiler Roots          129 us, workers (us): ---, ---, 129, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---,
 *   Weak Roots                         36 us, parallelism: 0.94x
 *     WR: &lt;total&gt;                      34 us
 *     WR: JFR Weak Roots                0 us, workers (us):   0, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---,
 *     WR: JNI Weak Roots               33 us, workers (us):  33, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---,
 *   Update Region States              234 us
 *   Choose Collection Set             440 us
 *   Rebuild Free Set                   36 us
 *     E: &lt;total&gt;                    69151 us
 *     E: JNI Handles Roots              3 us, workers (us):   3, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---,
 *     E: JFR Weak Roots                 1 us, workers (us): ---, ---, ---,   1, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---,
 *     E: Flat Profiler Roots           22 us, workers (us):  22, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---, ---,
 *     UR: &lt;total&gt;                    3127 us
 *   Update Region States              226 us
 *   Trash Collection Set               61 us
 *   Rebuild Free Set                   45 us
 * </pre>
 * 
 * 
 * @author <a href="mailto:mmillson@redhat.com">Mike Millson</a>
 * 
 */
public class ShenandoahStatsEvent extends ShenandoahCollector implements ThrowAwayEvent {

    /**
     * Regular expression defining standard logging.
     */
    private static final String REGEX[] = {
            //
            "^(" + UnifiedRegEx.DECORATOR
                    + ")?[ ]{0,}All times are wall-clock times, except per-root-class counters, that are sum over$",
            //
            "^(" + UnifiedRegEx.DECORATOR
                    + ")?[ ]{0,}all workers. Dividing the <total> over the root stage time estimates parallelism.$",
            //
            "^(" + UnifiedRegEx.DECORATOR
                    + " )?Concurrent (Class Unloading|Cleanup|Evacuation|Marking|Reset|Precleaning|"
                    + "(Mark|Strong|Thread) Roots|Update (Refs|Thread Roots)|Weak (References|Roots)).+$",
            //
            "^(" + UnifiedRegEx.DECORATOR + " )?Pause (Init|Final) Mark \\((G|N)\\).+$",
            //
            "^(" + UnifiedRegEx.DECORATOR
                    + " )?[ ]{2,4}(Accumulate Stats|Exception Caches|Finish (Mark|Queues|Work)|Make Parsable|"
                    + "Manage GCLABs|Purge Unlinked|(Code )?Roots|Rendezvous|System (Purge|Dictionary)|Unlink Stale|"
                    + "Update Region States|Weak (Class Links|References)).*$",
            //
            "^(" + UnifiedRegEx.DECORATOR + " )?[ ]{2,4}(Scan|Update) Roots.*$",
            //
            "^(" + UnifiedRegEx.DECORATOR + ")?[ ]{4,5}(DU|E|S): JNI Handles Roots.+$",
            //
            "^(" + UnifiedRegEx.DECORATOR + ")?[ ]{4,6}(DU|E|FA|FU|S|WR): JFR Weak Roots.+$",
            //
            "^(" + UnifiedRegEx.DECORATOR + ")?    (DU|S|WR): JNI Weak Roots.+$",
            //
            "^(" + UnifiedRegEx.DECORATOR + ")?    (E|S): Flat Profiler Roots.+$",
            //
            "^(" + UnifiedRegEx.DECORATOR + ")?[ ]{2,3}(Choose|Trash) Collection Set.+$",
            //
            "^(" + UnifiedRegEx.DECORATOR + ")?[ ]{2,3}Rebuild Free Set.+$",
            //
            "^(" + UnifiedRegEx.DECORATOR + " )?  Finish Work.+$",
            //
            "^(" + UnifiedRegEx.DECORATOR + " )?Pause (Degenerated|Full) GC \\((G|N)\\).+$",
            //
            "^(" + UnifiedRegEx.DECORATOR + " )?  Degen Update Roots.+$",
            //
            "^(" + UnifiedRegEx.DECORATOR
                    + " )?[ ]{2,6}(Cleanup|CLDG|Deallocate Metadata|Enqueue|Parallel Cleanup|Process|Unload Classes|"
                    + "Weak Roots).*$",
            //
            "^(" + UnifiedRegEx.DECORATOR + " )?  (Initial|Prepare)( Evacuation)?.*$",
            //
            "^(" + UnifiedRegEx.DECORATOR + " )?  (Resize|Retire|Sync|Trash) (CSet|GCLABs|Pinned|TLABs).*$",
            //
            "^(" + UnifiedRegEx.DECORATOR
                    + " )?  (Adjust Pointers|Calculate Addresses|Copy Objects|(Post|Pre) Heap Dump|Mark).*$",
            //
            "^(" + UnifiedRegEx.DECORATOR
                    + " )?    ((Humongous|Regular) Objects|Rebuild Region Sets|Reset Complete Bitmap).*$",
            //
            "^(" + UnifiedRegEx.DECORATOR
                    + " )?[ ]{4,6}(DU|E|FA|FS|FU|S|U|UR): (CLDG|Code Cache|Flat Profiler|JNI|JNI Handles|JNI Weak|"
                    + "Management|String Table|Synchronizer|System Dict|Thread|Universe|JVMTI) Roots.*$",
            //
            "^(" + UnifiedRegEx.DECORATOR + " )?Pause (Init[ ]{0,1}|Final) (Mark|Update Refs|Evac) \\([G|N]\\).*$",
            //
            "^(" + UnifiedRegEx.DECORATOR
                    + ")?    (DU|S): (CLDG|Code Cache|Flat Profiler|JVMTI|Management|String Table|Synchronizer|"
                    + "System Dict|Thread|Universe) Roots.+$",
            //
            "^(" + UnifiedRegEx.DECORATOR + " )?Allocation pacing accrued:$",
            //
            "^(" + UnifiedRegEx.DECORATOR + " )?Pacing.*$",
            // ,
            "^(" + UnifiedRegEx.DECORATOR
                    + " )?[ ]{0,7}\\d{1,8} of[ ]{0,5}\\d{1,7} ms \\([ ]{0,2}\\d{1,}\\.\\d%\\):.+$",
            //
            "^(" + UnifiedRegEx.DECORATOR + " )?[ ]{2,6}(CMR|CSR|CTR|CU|CWR|CWRF|DU|E|FA|FS|FU|S|UR|WR): "
                    + "(<total>|Code Cache|Weak References|(CLDG|(Resolved|String) Table|Thread|VM (Strong|Weak)) "
                    + "Roots) .*$"
            //
    };

    private static final List<Pattern> REGEX_PATTERN_LIST = new ArrayList<>(REGEX.length);

    static {
        for (String regex : REGEX) {
            REGEX_PATTERN_LIST.add(Pattern.compile(regex));
        }
    }

    public String getLogEntry() {
        throw new UnsupportedOperationException("Event does not include log entry information");
    }

    public String getName() {
        return JdkUtil.LogEventType.SHENANDOAH_STATS.toString();
    }

    public long getTimestamp() {
        throw new UnsupportedOperationException("Event does not include timestamp information");
    }

    /**
     * Determine if the logLine matches the logging pattern(s) for this event.
     * 
     * @param logLine
     *            The log line to test.
     * @return true if the log line matches the event pattern, false otherwise.
     */
    public static final boolean match(String logLine) {
        boolean match = false;
        for (int i = 0; i < REGEX_PATTERN_LIST.size(); i++) {
            Pattern pattern = REGEX_PATTERN_LIST.get(i);
            if (pattern.matcher(logLine).matches()) {
                match = true;
                break;
            }
        }
        return match;
    }
}
