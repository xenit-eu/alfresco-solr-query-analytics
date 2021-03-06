package eu.xenit.util;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.alfresco.service.namespace.NamespaceService;

import java.util.Collection;
import java.util.HashMap;
import java.util.regex.*;
import java.util.Set;

public class SolrQueryParser {

    private static final Logger logger = LoggerFactory.getLogger(SolrQueryParser.class);

    public static final String FULL_TEXT_PROPERTY = "FullText";
    public final String QUERY_OPTIONS_REGEX = "\\*|\\?";
    public final String ALLOWED_PROPERTY_CHARS = "[\\w|\\.]";
    public final String ALLOWED_FTS_OPTIONS = "[\\-|\\!|\\+|\\~|\\^|=]";

    private NamespaceService namespaceService;

    public SolrQueryParser() {

    }

    public JSONObject formatToQueryJson(String query) {
        JSONObject json = new JSONObject();
        json.put("queryString", escapeIllegalChars(query));
        try {
            json.put("properties", extractProperties(query));
            json.put("extraction_status", "OK");
        } catch (Exception e) {
            json.put("extraction_status", e.getMessage());
        }
        logger.debug("Extraction complete. Returning " + json);
        return json;
    }

    public Set<String> extractProperties(String query) {
        HashMap<String, String> propertyMap = new HashMap<>();
        logger.debug("Extracting properties from query = " + query);
        query = escapeIllegalChars(query);
        query = removeLeadingSpacesAfterProperty(query);
        query = refactorRange(query);
        query = removeSpacesFromExactQueries(query);
        query = removeANDOR(query);
        query = removeNOTProperties(query);
        query = removeBrackets(query);
        logger.debug("Query after prefiltering = " + query);

        String[] splitQuery = query.split("\\s+");

        for (String keyword : splitQuery) {
            //Example +TYPE:"{http://www.alfresco.org/model/site/1.0}site"
            String typePropertyRegex = ALLOWED_FTS_OPTIONS + "?(TYPE):(.*)";
            //Example PATH:"/app:company_home/st:sites/cm:test-site//*"
            String pathRegex = ALLOWED_FTS_OPTIONS + "?(PATH):(.*)";
            //Example TEXT:"test"
            String textRegex = ALLOWED_FTS_OPTIONS + "?(TEXT):(.*)";
            //Example ASPECT:"cm:auditable"
            String aspectRegex = ALLOWED_FTS_OPTIONS + "?(ASPECT):(.*)";
            //Example {http://www.alfresco.org/model/content/1.0}created
            String qnamePropertyRegex = ALLOWED_FTS_OPTIONS + "?\\{([^\\s]*)\\}(" + ALLOWED_PROPERTY_CHARS + "*):(.*)";
            //Example +@cm\:modified:[NOW/DAY-7DAYS TO NOW/DAY+1DAY]
            String escapedPropertyRegex = ALLOWED_FTS_OPTIONS + "?@([^\\s]*)\\\\\\\\:([^\\s]*):(.*)";
            //Example cm:name:test
            String formattedPropertyRegex = ALLOWED_FTS_OPTIONS + "?(.*):(.*):(.*)";

            String property;
            String value;

            if (keyword.matches(typePropertyRegex)) {
                property = keyword.replaceAll(typePropertyRegex, "$1");
                value = keyword.replaceAll(typePropertyRegex, "$2");
            } else if (keyword.matches(pathRegex)) {
                property = keyword.replaceAll(pathRegex, "$1");
                value = keyword.replaceAll(pathRegex, "$2");
            } else if (keyword.matches(textRegex)) {
                property = FULL_TEXT_PROPERTY;
                value = keyword.replaceAll(textRegex, "$2");
            } else if (keyword.matches(aspectRegex)) {
                property = keyword.replaceAll(aspectRegex, "$1");
                value = keyword.replaceAll(aspectRegex, "$2");
            } else if (keyword.matches(qnamePropertyRegex)) {
                property = replaceNameSpaceToPrefix(keyword.replaceAll(qnamePropertyRegex, "$1")) + keyword.replaceAll(qnamePropertyRegex, "$2");;
                value = keyword.replaceAll(qnamePropertyRegex, "$3");
            } else if (keyword.matches(escapedPropertyRegex) ) {
                property = keyword.replaceAll(escapedPropertyRegex, "$1:$2");
                value = keyword.replaceAll(escapedPropertyRegex, "$3");
            } else if (keyword.matches(formattedPropertyRegex)) {
                property = keyword.replaceAll(formattedPropertyRegex, "$1:$2");
                value = keyword.replaceAll(formattedPropertyRegex, "$3");
            } else {
                property = FULL_TEXT_PROPERTY;
                value = keyword;
            }
            propertyMap.put(property, value);
        }
        logger.debug("Extracted properties= " + propertyMap);
        return propertyMap.keySet();
    }

    private String replaceNameSpaceToPrefix(String nameSpaceURI) {
        if ( namespaceService != null ) {
            Collection<String> prefixes = namespaceService.getPrefixes(nameSpaceURI.replace("\\",""));
            if (!prefixes.isEmpty()) {
                return prefixes.iterator().next() + ":";
            }
        }
        return "{" + nameSpaceURI + "}";
    }

    public String escapeIllegalChars(String query) {
        // Avoid already escaped forward slashes being escaped again
        query = query.replaceAll("\\\\/","/");
        return (new org.json.simple.JSONObject()).escape(query);
    }

    private String removeLeadingSpacesAfterProperty(String query) {
        Pattern leadingSpaces = Pattern.compile(":\\s+");
        Matcher matcher = leadingSpaces.matcher(query);
        return regexReplace(matcher, "\\s", "");
    }

    private String removeSpacesFromExactQueries(String query) {
        Pattern exactSearchPatter = Pattern.compile("\\\\\"[\\w|\\s|" + QUERY_OPTIONS_REGEX + "]*\\\\\"");
        Matcher matcher = exactSearchPatter.matcher(query);
        return regexReplace(matcher, "\\s", "");
    }

    private String regexReplace(Matcher matcher, String regex, String replacement) {
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(stringBuffer, matcher.group().replaceAll(regex, replacement));
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }

    public String removeNOTProperties(String query) {
        //TODO removing properties that are negated by ! or -
        // at the moment they will be added without the lucene search options
        Pattern exactSearchPatter = Pattern.compile("\\s*NOT\\s+");
        while (true) {
            Matcher matcher = exactSearchPatter.matcher(query);
            if (matcher.find() == false) {
                return query;
            } else {
                String subQuery = query.substring(matcher.end());
                int endIndex;
                if (subQuery.length() == 0 || subQuery.charAt(0) == '(') {
                    int openBr = 0;
                    endIndex = matcher.end();
                    for (int i = 0; i < subQuery.length(); i++) {
                        if (subQuery.charAt(i) == '(') {
                            openBr++;
                        } else if (subQuery.charAt(i) == ')') {
                            openBr--;
                        }
                        if (openBr == 0) {
                            endIndex += i + 1;
                            break;
                        }
                    }
                } else {
                    endIndex = matcher.end();
                    //Remove property until next space
                    for (int i = 0; i < subQuery.length(); i++) {
                        if (subQuery.charAt(i) == ' ') {
                            endIndex += i;
                            break;
                        }
                    }
                    //End of query string special case
                    if (endIndex == matcher.end()) {
                        endIndex += subQuery.length();
                    }
                }
                query = query.subSequence(0, matcher.start()) + " " + query.subSequence(endIndex, query.length());
            }
        }
    }

    private String removeANDOR(String query) {
        query = query.replaceAll("\\s+AND\\s+", " ");
        query = query.replaceAll("\\s+OR\\s+", " ");
        return query;
    }

    private String removeBrackets(String query) {
        return query.replaceAll("[(|)]", "");
    }

    public String refactorRange(String query) {
        Pattern rangeRegex = Pattern.compile("(\\[.*)\\s+TO\\s+(.*\\])");
        Matcher matcher = rangeRegex.matcher(query);
        if (matcher.find()) {
            String refactoredRange = matcher.group(1).replaceAll(":" , "-") + ".." + matcher.group(2).replaceAll(":" , "-");
            return query.replaceAll(rangeRegex.toString(), refactoredRange);
        } else {
            return query;
        }
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

}