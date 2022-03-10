package util;

import eu.xenit.util.SolrQueryParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SolrQueryParserTest {

    SolrQueryParser solrQueryParser = new SolrQueryParser();
    public final String FULL_TEXT_PROPERTY = solrQueryParser.FULL_TEXT_PROPERTY;

    private String query;
    private LinkedHashMap<String, String> propertyMap = new LinkedHashMap<>();

    private String createSearchQuery(LinkedHashMap<String, String> propertyMap) {
        String q = "";
        for (String property : propertyMap.keySet()){
            switch (property) {
                case "FullText":
                    q +=  propertyMap.get(property);
                    break;
                case "AND":
                    q += property;
                    break;
                case "OR":
                    q += property;
                    break;
                case "NOT":
                    q += property;
                    break;
                case "TEXT":
                    q +=  propertyMap.get(property);
                    propertyMap.put(FULL_TEXT_PROPERTY, propertyMap.get(property));
                    propertyMap.remove("TEXT");
                    break;
                default:
                    q += property + ":" + propertyMap.get(property);
            }
            q += " ";
        }
        System.out.println("q=" + q);
        return q;
    }

    private void assertExtraction(LinkedHashMap<String, String> propertyMap, Set<String> extractedProperties) {
        Set<String> formattedPropertySet = new HashSet<>();
        for (String property : propertyMap.keySet()){
            if (shouldBeExtracted(property)) {
                formattedPropertySet.add(removeLuceneQueryOptions(solrQueryParser.escapeIllegalChars(property)));
            }
        }
        for (String property : extractedProperties){
            assertTrue(shouldBeExtracted(property));
        }
        assertEquals(formattedPropertySet, extractedProperties);
    }

    private String removeLuceneQueryOptions(String property) {
        return property.replaceAll(solrQueryParser.ALLOWED_FTS_OPTIONS, "");
    }

    private boolean shouldBeExtracted(String property) {
        if (property.equals("AND") || property.equals("OR") || property.equals("NOT")) {
            return false;
        } else if (property.startsWith("!")) {
            return false;
        } else {
            return true;
        }
    }

    @BeforeEach
    private void initPropertyMap() {
        propertyMap.clear();
        query = null;
    }

    @AfterEach
    private void assertTest() {
        if (!propertyMap.isEmpty()) {
            Set<String> extractedProperties = solrQueryParser.extractProperties(createSearchQuery(propertyMap));
            assertExtraction(propertyMap, extractedProperties);
        }
        if (query != null) {
            Set<String> extractedProperties = solrQueryParser.extractProperties(query);
            assertExtraction(propertyMap, extractedProperties);
        }
    }

    @Test
    public void simpleFullTextTest() {
        propertyMap.put(FULL_TEXT_PROPERTY,"test");
    }

    @Test
    public void simplePropertyTest() {
        propertyMap.put("cm:name","test");
    }

    @Test
    public void simpleFTSandPropertyTest() {
        propertyMap.put(FULL_TEXT_PROPERTY,"test");
        propertyMap.put("cm:name","test");
    }

    @Test
    public void simpleFTSandMultiplePropertiesTest() {
        propertyMap.put(FULL_TEXT_PROPERTY,"test");
        propertyMap.put("cm:name","test");
        propertyMap.put("cm:description","test");
    }

    @Test
    public void simpleExactQueriesTest() {
        propertyMap.put("cm:name","\"test\"");
        propertyMap.put("cm:description","\"test  test   test  test     \"");
    }

    @Test
    public void simpleAndQueryTest() {
        propertyMap.put("cm:name","test");
        propertyMap.put("AND","");
        propertyMap.put("cm:description","test");
    }

    @Test
    public void simpleORQueryTest() {
        propertyMap.put(FULL_TEXT_PROPERTY,"test");
        propertyMap.put("OR","");
        propertyMap.put("cm:description","test");
    }

    @Test
    public void typeQueryTest() {
        propertyMap.put("TYPE","\"cm:content\"");
    }

    @Test
    public void textTest() {
        propertyMap.put("TEXT","\"Full text search\"");
    }

    @Test
    public void typeVariationQueryTest() {
        propertyMap.put("+TYPE","\"{http://www.alfresco.org/model/site/1.0}site\"");
    }

    @Test
    public void lunceneQueryOptionsTest() {
        propertyMap.put("+cm:name","\" test\"");
        propertyMap.put("OR","");
        propertyMap.put("-cm:title","\" test*\"");
    }

    @Test
    public void typeCombinationQueryTest() {
        query= "+TYPE:\"{http://www.alfresco.org/model/site/1.0}site\" AND ( cm:name:\" test\" OR  cm:title: (\"test*\" ) OR cm:description:\"test\")";
        propertyMap.put("+TYPE","\"{http://www.alfresco.org/model/site/1.0}site\"");
        propertyMap.put("cm:name","\" test\"");
        propertyMap.put("OR","");
        propertyMap.put("cm:title","\" test*\"");
        propertyMap.put("OR","");
        propertyMap.put("cm:description","\" test*\"");
    }

    @Test
    public void fullContentModelURITest() {
        propertyMap.put("{http://www.alfresco.org/model/content/1.0}created","(\"NOW/DAY-1MONTH\"..\"NOW/DAY+1DAY\" )");
        propertyMap.put("OR","");
        propertyMap.put("{http://www.alfresco.org/model/content/1.0}content.size","(\"1048576\"..\"16777216\")");
    }

    @Test
    public void fullContentModelAndFullTextTest() {
        query = "(test doc ) AND ({http://www.alfresco.org/model/content/1.0}created:(\"NOW/DAY-1MONTH\"..\"NOW/DAY+1DAY\" )";
        propertyMap.put(FULL_TEXT_PROPERTY,"\" test\"");
        propertyMap.put("AND","");
        propertyMap.put("{http://www.alfresco.org/model/content/1.0}created","(\"NOW/DAY-1MONTH\"..\"NOW/DAY+1DAY\" )");
    }

    @Test
    public void pathQueryTest() {
        propertyMap.put("PATH","/app:company_home/st:sites/cm:test-site//*");
    }

    @Test
    public void aspectQuery() {
        propertyMap.put("ASPECT", "\"cm:auditable\"");
    }

    @Test
    public void pathCombinationTest() {
        query = "((PATH:\"/app:company_home/st:sites/cm:test-site//*\" AND {http://www.alfresco.org/model/content/1.0}created:(\"NOW/DAY-1MONTH\"..\"NOW/DAY+1DAY\") AND {http://www.alfresco.org/model/content/1.0}creator:\"admin\") AND TYPE:\"cm:content\"";
        propertyMap.put("PATH","/app:company_home/st:sites/cm:test-site//*");
        propertyMap.put("{http://www.alfresco.org/model/content/1.0}created","(\"NOW/DAY-1MONTH\"..\"NOW/DAY+1DAY\" )");
        propertyMap.put("{http://www.alfresco.org/model/content/1.0}creator","\"admin\"");
        propertyMap.put("TYPE", "\"cm:content\"");
    }

    @Test
    public void mimeTypeTest() {
        query = "{http://www.alfresco.org/model/content/1.0}creator:\"admin\" " +
                "AND {http://www.alfresco.org/model/content/1.0}content.mimetype:\"application/vnd.openxmlformats-officedocument.wordprocessingml.document\") " +
                "AND TYPE:\"cm:content\"";
        propertyMap.put("{http://www.alfresco.org/model/content/1.0}creator","\"admin\"");
        propertyMap.put("{http://www.alfresco.org/model/content/1.0}content.mimetype","\"application/vnd.openxmlformats-officedocument.wordprocessingml.document\"");
        propertyMap.put("TYPE", "\"cm:content\"");
    }

    @Test
    public void removeNotFunctionTest() {
        String testQ = " NOT    (()()(())) test NOT    (()()(()))";
        assertEquals("  test ", solrQueryParser.removeNOTProperties(testQ));

        String testPropQ = " NOT TYPE:cm:content";
        assertEquals(" ", solrQueryParser.removeNOTProperties(testPropQ));

        String testCombination = "NOT ((TYPE:cm:content) AND (PATH:/app:company_home/st:sites/cm:test-site//*)) AND NOT TYPE:cm:content";
        assertEquals("  AND ", solrQueryParser.removeNOTProperties(testCombination));

        String complexQ = "test cm:name:test AND NOT (TYPE:\\\"{http://www.alfresco.org/model/content/1.0}dictionaryModel\\\" OR TYPE:\\\"{http://www.alfresco.org/model/datalist/1.0}issue\\\")";
        assertEquals("test cm:name:test AND ", solrQueryParser.removeNOTProperties(complexQ));
    }

    @Test
    public void simpleNotQueryTest() {
        query = "TYPE:cm:content AND NOT cm:name: \"test\"";
        propertyMap.put("TYPE","cm:content");
        propertyMap.put("AND","");
        propertyMap.put("NOT","");
        propertyMap.put("!cm:name", "test");
    }

    @Test
    public void specialCaseQueryTest() {
        query = "cm:id:[18 TO 30]";
        propertyMap.put("cm:id","[18 TO 30]");
    }

    @Test
    public void rangeQueryTest() {
        String refactoredRange = solrQueryParser.refactorRange("cm:modified:[1972-05-20T17:33:18.772Z TO 2072-05-20T17:33:18.772Z]");
        String property = "cm:modified:";
        assertTrue(refactoredRange.contains(property));
        assertFalse(refactoredRange.substring(property.length()).contains(":"));
    }

    @Test
    public void advancedQueryTest() {
        //Real Finder Query
        query = "((({http://www.alfresco.org/model/content/1.0}creator:\"test\" AND" +
                " (TYPE:\"{http://www.xenit.eu/xenit/model/0.3}project\" AND TEXT:\"Test\")) " +
                "AND {http://www.alfresco.org/model/content/1.0}content.mimetype:\"application/vnd.openxmlformats-officedocument.wordprocessingml.document\") " +
                "AND NOT " +
                "(TYPE:\"{http://www.alfresco.org/model/content/1.0}dictionaryModel\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/datalist/1.0}issue\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/forum/1.0}post\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/datalist/1.0}todoList\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/linksmodel/1.0}link\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/download/1.0}download\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/datalist/1.0}issue\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataList\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataListItem\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/content/1.0}systemfolder\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/content/1.0}rating\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/content/1.0}thumbnail\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/content/1.0}failedThumbnail\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/bpm/1.0}package\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/forum/1.0}forum\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/forum/1.0}topic\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/forum/1.0}post\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/site/1.0}site\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/transfer/1.0}tempTransferStore\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/transfer/1.0}transferReportDest\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/transfer/1.0}transferReport\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/transfer/1.0}transferLock\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/transfer/1.0}transferRecord\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/bpm/1.0}workflowDefinition\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/content/1.0}savedquery\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/content/smartfolder/1.0}smartFolderTemplate\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/solrfacet/1.0}facetField\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/surf/1.0}amdpage\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/calendar}updateEvent\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/calendar}ignoreEvent\" " +
                "OR TYPE:\"{http://www.alfresco.org/model/calendar}calendarEvent\" " +
                "OR ASPECT:\"{http://www.alfresco.org/model/system/1.0}hidden\"))";
        propertyMap.put("{http://www.alfresco.org/model/content/1.0}creator","\"test\"");
        propertyMap.put("TYPE","{http://www.xenit.eu/xenit/model/0.3}project");
        propertyMap.put(FULL_TEXT_PROPERTY,"\"Test\"");
        propertyMap.put("{http://www.alfresco.org/model/content/1.0}content.mimetype",
                "\"application/vnd.openxmlformats-officedocument.wordprocessingml.document\"");
    }

    @Test
    public void testShareQuery(){
        query = "+@cm\\:modified:[2021\\-7\\-27T00\\:00\\:00.000 TO 2021\\-8\\-3T23\\:59\\:59.999] " +
                "+@cm\\:modifier:\"admin\" +TYPE:\"cm:content\" -TYPE:\"cm:systemfolder\" " +
                "-TYPE:\"fm:forums\" -TYPE:\"fm:forum\" -TYPE:\"fm:topic\" -TYPE:\"fm:post\" " +
                "+(TYPE:\"content\" OR TYPE:\"app:filelink\" OR TYPE:\"folder\")";
        propertyMap.put("cm:modified","[2021\\-7\\-27T00\\:00\\:00.000 TO 2021\\-8\\-3T23\\:59\\:59.999]");
        propertyMap.put("cm:modifier","\"admin\"");
        propertyMap.put("TYPE","\"cm:content\"");
    }

    @Test
    public void testLuceneQuery() {
        query = "+@cm\\:modified:[NOW/DAY-7DAYS TO NOW/DAY+1DAY] +TYPE:\"cm:content\"";
        propertyMap.put("cm:modified","[NOW/DAY-7DAYS TO NOW/DAY+1DAY]");
        propertyMap.put("TYPE", "\"cm:content\"");
    }

}
