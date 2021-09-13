package util;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;


public class SolrTimingParser {
    private static final Logger logger = LoggerFactory.getLogger(SolrTimingParser.class);

    /**
     * Timing information included in the solr debug information usually consists of two main phases: prepare and process.
     * This function makes abstraction of the main phases and collects the total time spent on the subphases of these mainphases.
     * If a subphase occurs in multiple main phases, the values are summed together.
     * @param timings JSONObject as returned by SOLR with debugQuery=on param.
     * @return JSONObject with subphases keys and cumulative timings of the different main phases
     */
    public static JSONObject transformTimingJson(JSONObject timings) {
        JSONObject timingInfo = new JSONObject();
        Iterator<String> phaseIterator = timings.keys();
        while (phaseIterator.hasNext()) {
            String phase = phaseIterator.next();
            if (!phase.equals("time")) {
                JSONObject phaseTimings = timings.getJSONObject(phase);
                Iterator<String> subPhaseIterator = phaseTimings.keys();
                while (subPhaseIterator.hasNext()) {
                    String subPhase = subPhaseIterator.next();
                    if (!subPhase.equals("time")) {
                        if (!timingInfo.has(subPhase)) {
                            timingInfo.put(subPhase, phaseTimings.getJSONObject(subPhase).getLong("time"));
                        } else {
                            timingInfo.put(subPhase, timingInfo.getLong(subPhase) + (phaseTimings.getJSONObject(subPhase)).getLong("time"));
                        }
                    }
                }
            }
        }
        return timingInfo;
    }

}
