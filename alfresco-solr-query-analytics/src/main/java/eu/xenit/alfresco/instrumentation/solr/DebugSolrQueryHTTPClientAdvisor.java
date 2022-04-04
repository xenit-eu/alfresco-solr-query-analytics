package eu.xenit.alfresco.instrumentation.solr;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import org.alfresco.repo.management.subsystems.SubsystemProxyFactory;
import org.alfresco.repo.search.impl.lucene.JSONResult;
import org.alfresco.repo.search.impl.lucene.SolrJSONResultSet;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.util.ReflectionUtils;

import eu.xenit.util.SolrQueryParser;
import eu.xenit.util.SolrTimingParser;

public class DebugSolrQueryHTTPClientAdvisor {

	private static final Logger logger = LoggerFactory.getLogger(DebugSolrQueryHTTPClientAdvisor.class);

	private final Properties globalProperties;
	private final SolrQueryParser solrQueryParser;

	public DebugSolrQueryHTTPClientAdvisor(final SubsystemProxyFactory bean, Properties globalProperties,
			final SolrQueryParser solrQueryParser) {
		this.globalProperties = globalProperties;
		this.solrQueryParser = solrQueryParser;

		final Method debugMethod = ReflectionUtils.findMethod(SolrJSONResultSet.class, "getResponseBodyAsJSONObject");
		if (isUseSolrDebug() && debugMethod != null) {

			bean.addAdvisor(new DefaultPointcutAdvisor(new MethodInterceptor() {
				public Object invoke(MethodInvocation mi) throws Throwable {
					if ("query".equals(mi.getMethod().getName())) {

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
								if (extraParameters.containsKey("shards")) {
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
