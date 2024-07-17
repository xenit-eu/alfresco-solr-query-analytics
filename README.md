## Functionality

This module builds an Alfresco amp which logs more information for a solr query. It is especially useful for distributed search, to analyze performance per shard.

The functionality can be controlled with the boolean parameter *solr.useDebug=true*.

Tested and tailor-made for alfrescoVersion = **'7.4.1.3'**.

**warning**
Using this amp for any other version as specified above can have unforeseen consequences.

### Technical implementation
This AMP module uses AOP to intercept the calls located in the *searchServiceSubsystemProxy* bean. 
When a call to the "query" method is detected *and* *solr.useDebug=true* then the Debug parameter will be added to the API call to SOLR, causing SOLR to return a lot more debug data. 

When the API call from SOLR returns, we will again be in the intercepted flow, where we retrieve the SOLR response (JSONObject jsonresponse) parse it, and write it to the solrquery.log file.


**warning**
We have to note that we are overriding the *SolrJSONResultSet.class* file with our own variant. Our variant has an additional function (namely *getResponseBodyAsJSONObject*)
Before intercepting the query method, we will check if this function exists. In the build.gradle you can find the *sourceSets* directive that will copy our class to the classes directory in tomcat, effectively overriding the existing one.
You can find the overriding class here inside the running alfresco container:

***/usr/local/tomcat/webapps/alfresco/WEB-INF/classes/org/alfresco/repo/search/impl/lucene***

The risk exists in the fact that, when we upgrade alfresco, we no longer know if the copy we took of *SolrJSONResultSet.class* to add our *getResponseBodyAsJSONObject* function to is still correct. This can lead to very hard to troubleshoot issues like methodDoesNotExist


Alfresco's *org.alfresco.repo.search.impl.solr.SolrQueryHTTPClient* and configures a separate appender for its logger, json-based, whose output is solr debug information.

Integration tests show how to send the logs to elasticsearch, via filebeat, for a stack with 1 Alfresco node and 2 solr shards. Multiple (combinations of) versions for Alfresco and Solr can easily be added and tested.  

A sidecar container is added to Alfresco, having as output solr debug lines, parsed with a custom filebeat module and sent to a separate index in ES.

Filebeat is configured to use docker autodiscovery based on container/image labels. Example of filebeat-related labels:

  * co.elastic.logs/module=json-alfresco - to use module json-alfresco for parsing
  * co.elastic.logs/enabled=false - to not send logs to ES
  * eu.xenit.index=solrlogs - custom ES index to send log lines to

### SolrQueryParser

The SolrQueryParser utility extracts properties from the search query. This extraction allows to gather insights about which property is regularly searched upon. This information aides in property relevancy tuning for search interfaces used to query Alfresco. 

Example: 

```
query= TYPE:"{http://www.alfresco.org/model/site/1.0}site\" AND cm:name:"test"
result= [TYPE, cm:name]
```

Properties after the NOT keyword or contained in the NOT brackets are left out of the extraction.

```
query= test cm:name:test AND NOT (TYPE:"{http://www.alfresco.org/model/content/1.0}dictionaryModel" OR TYPE:"{http://www.alfresco.org/model/datalist/1.0}issue") 
result= [FullText, cm:name]
``` 

Lucene search options are removed if present.

```
query= +TYPE:"{http://www.alfresco.org/model/site/1.0}site\" AND !cm:name:"test"
result= [TYPE, cm:name]
```

## How to start

Prerequisites:

* filebeat/ needs ot be owned by root and should only be writable by owner
* virtual memory has to be increased: sysctl -w vm.max_map_count=262144

1. Start up elasticsearch and kibana.

        cd integration-tests/src/main/compose/
        docker-compose -f docker-compose-elk.yml up -d

2. Once elasticsearch container are fully started, run the *post-start.sh* script.

        cd elasticsearch
         ./post-start.sh

This script takes care of the index lifetime management for the custom index where logs are being written, also of fields mapping for this index. The name of this index should be the same with the label `eu.xenit.index`.
In an ansible setup, the post-start.sh will be done via ansible.


3. Start alfresco (with version 74) and solr

           ./gradlew integration-tests:alfresco-enterprise-74:cU
    

4. Trigger some searches

        cd integration-tests/src/main/compose
        ./send_searches.sh
	
5. Access kibana at localhost:5601 (username=elastic,password=changeme)

Use discovery mode and explore using fields like

        xenit.totalElapsedTime
        xenit.totalNumFound
        xenit.shard1.ElapsedTime
        xenit.shard1.NumFound
        xenit.queryString
        xenit.queryProperties

There are also some dashboards available with information about performance per shard and properties searched for.
The dashboards are "injected"  by Filebeat, if they are not visible, try restarting the Filebeat container, it will re-initialize the dashboards.

Alfresco is available at http://localhost/alfresco. A finder has been included in the stack, available at http://localhost/finder.

## Maintenance

The amp will override the original  **SolrJSONResultSet.class** by being unpacked by the amp in:
***/usr/local/tomcat/webapps/alfresco/WEB-INF/classes/org/alfresco/repo/search/impl/lucene***

* only logger lines need to be changed, depending on the useSolrDebug variable:


For efficiency, it is possible to only send to ES a subset of the debug information, for example total number of hits, total time elapsed and information per shard.



