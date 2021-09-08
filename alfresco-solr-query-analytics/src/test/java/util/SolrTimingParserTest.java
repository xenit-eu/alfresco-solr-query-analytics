package util;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static junit.framework.Assert.assertTrue;

public class SolrTimingParserTest {


    SolrTimingParser solrTimingParser = new SolrTimingParser();

    @Test
    public void testTimingsJsonFormatting() {
        JSONObject defaultTimeJson = new JSONObject();
        defaultTimeJson.put("time" , 10);

        JSONObject prepareTiming = new JSONObject();
        prepareTiming.put("A" , defaultTimeJson);
        prepareTiming.put("B", defaultTimeJson);
        prepareTiming.put("time" , 20);

        JSONObject processTiming = new JSONObject();
        processTiming.put("C" , defaultTimeJson);
        processTiming.put("A" , defaultTimeJson);
        processTiming.put("time" , 20);

        JSONObject timingInfo = new JSONObject();
        timingInfo.put("time" , 40);
        timingInfo.put("prepare" , prepareTiming);
        timingInfo.put("process" , processTiming);

        JSONObject formattedJson = solrTimingParser.transformTimingJson(timingInfo);

        System.out.println(formattedJson);
        assertEquals(formattedJson.getLong("A"), 20 );
        assertEquals(formattedJson.getLong("B"), 10);
        assertEquals(formattedJson.getLong("C"), 10);
        assertFalse(formattedJson.has("time"));
    }
}
