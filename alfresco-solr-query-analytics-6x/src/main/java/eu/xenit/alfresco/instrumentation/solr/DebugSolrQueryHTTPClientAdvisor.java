package eu.xenit.alfresco.instrumentation.solr;

import eu.xenit.util.SolrQueryParser;
import eu.xenit.util.SolrTimingParser;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import org.alfresco.repo.search.impl.lucene.JSONResult;
import org.alfresco.repo.search.impl.lucene.SolrJSONResultSet;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.util.ReflectionUtils;

public class DebugSolrQueryHTTPClientAdvisor {

    private static final Logger logger = LoggerFactory.getLogger(DebugSolrQueryHTTPClientAdvisor.class);

    private final Properties globalProperties;
    private final SolrQueryParser solrQueryParser;

    public DebugSolrQueryHTTPClientAdvisor(final Advised bean, Properties globalProperties,
            final SolrQueryParser solrQueryParser) {
        this.globalProperties = globalProperties;
        this.solrQueryParser = solrQueryParser;

        //logger.debug("Setting up AOP intercept for SOLR query Analytics");
        final Method debugMethod = ReflectionUtils.findMethod(SolrJSONResultSet.class, "getResponseBodyAsJSONObject");

        if (debugMethod != null) {

            //logger.debug("Solr Debug enabled, activating advisor");
            //We need to add the "0" as first argument or this won't work
            bean.addAdvisor(0, new DefaultPointcutAdvisor(new MethodInterceptor() {
                public Object invoke(MethodInvocation mi) throws Throwable {
                    if ("query".equals(mi.getMethod().getName()) && isUseSolrDebug()) {
                        //logger.debug("Appending debug parameter to query");
                        Object[] arguments = mi.getArguments();

                        SearchParameters params = (SearchParameters) arguments[0];
                        Map<String, String> extraParameters = params.getExtraParameters();
                        if (!extraParameters.containsKey("debug")) {
                            params.addExtraParameter("debug", "all");
                        }

                        ResultSet resultSet = (ResultSet) mi.proceed();

                        try {
                            JSONObject jsonResponse = (JSONObject) ReflectionUtils.invokeMethod(debugMethod, resultSet);
                            JSONObject queryString = DebugSolrQueryHTTPClientAdvisor.this.solrQueryParser
                                    .formatToQueryJson(params.getQuery());

                            if (resultSet instanceof JSONResult) {
                                JSONObject timingJson = jsonResponse.optJSONObject("debug").optJSONObject("timing");
                                JSONObject timingInfo = SolrTimingParser.transformTimingJson(timingJson);
                                if (jsonResponse.optJSONObject("debug").has("track")) {
                                    logger.debug("{\"parsedQuery\":" + queryString + ", \"track\":"
                                            + jsonResponse.optJSONObject("debug").optJSONObject("track")
                                            + ", \"timing\":" + timingInfo + ",\"totalNumFound\":"
                                            + resultSet.getNumberFound() + ",\"totalElapsedTime\":"
                                            + ((JSONResult) resultSet).getQueryTime() + "}");
                                } else {
                                    logger.debug("{\"parsedQuery\":" + queryString + ", \"timing\":" + timingInfo
                                            + ",\"totalNumFound\":" + resultSet.getNumberFound()
                                            + ",\"totalElapsedTime\":" + ((JSONResult) resultSet).getQueryTime() + "}");
                                }
                            } else {
                                JSONObject timingJson = jsonResponse.optJSONObject("debug").optJSONObject("timing");
                                JSONObject timingInfo = SolrTimingParser.transformTimingJson(timingJson);
                                if (extraParameters.containsKey("shards")) {
                                    logger.debug("{\"parsedQuery\":" + queryString + ", \"track\":"
                                            + jsonResponse.optJSONObject("debug").optJSONObject("track")
                                            + ", \"timing\":" + timingInfo + ",\"totalNumFound\":"
                                            + resultSet.getNumberFound() + "}");
                                } else {
                                    logger.debug("{\"parsedQuery\":" + queryString + ", \"timing\":" + timingInfo
                                            + ",\"totalNumFound\":" + resultSet.getNumberFound() + "}");
                                }
                            }
                        } catch (Throwable e) {
                            String queryString = params.getQuery();
                            logger.debug(
                                    "{\"parsedQuery\":" + queryString + ", \"debugError\":" + e.getMessage() + "}");
                        }

                        return resultSet;
                    }

                    Object ret = mi.proceed();
                    return ret;
                }
            }));

        }
    }

    public boolean isUseSolrDebug() {
        if (globalProperties == null) {
            return false;
        }
        return Boolean.TRUE.equals(Boolean.valueOf(globalProperties.getProperty("solr.useDebug")));
    }

}
