## Functionality

This module builds an Alfresco amp which logs more information for a solr query. It is useful especially for distributed search, to analyze performance per shard.

The functionality can be controlled with the boolean parameter *solr.useDebug=true*.

The module replaces Alfresco's *org.alfresco.repo.search.impl.solr.SolrQueryHTTPClient* and configures a separate appender for its logger, json-based, whose output is solr debug information.

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

Properties subsequent to the NOT keyword or contained in the NOT brackets are left out of the extraction.

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

Once elasticsearch container started, run the *post-start.sh* script.

        cd elasticsearch
     	./post-start.sh

This script takes care of the index lifetime management for the custom index where logs are being written, also of fields mapping for this index. The name of this index should be the same with the label `eu.xenit.index`.
In an ansible setup, the post-start.sh will be done via ansible.



3. (optional) check supported alfresco versions

           ./gradlew integration-tests:projects
    

4. Start alfresco (with a specific version, eg 62) and solr

           ./gradlew integration-tests:alfresco-enterprise-{version}:cU
    

3. Trigger some searches

        cd integration-tests/src/main/compose
        ./send_searches.sh
	
4. Access kibana at localhost:5601 (username=elastic,password=changeme)

Use discovery mode and explore using fields like

        xenit.totalElapsedTime
        xenit.totalNumFound
        xenit.shard1.ElapsedTime
        xenit.shard1.NumFound
        xenit.queryString
        xenit.queryProperties

There are also some dashboards available with information about performance per shard and properties searched for.

Alfresco is available at http://localhost/alfresco. A finder has been included in the stack, available at http://localhost/finder.

## Maintenance

Copy bean definition for SolrQueryHTTPClient from the enterprise context and:

* replace the implementation class
* make sure the shard registry bean being used is search.solrShardRegistry (lowercase)
* add useSolrDebug property
* add SolrQuery bean setter


* only logger lines need to be changed, depending on the useSolrDebug variable:

        url.append("&debugQuery=on");
        ...
        s_logger.debug("Debug: " + json.get("debug"));

For efficiency, it is possible to only send to ES a subset of the debug information, for example total number of hits, total time elapsed and information per shard.



