# Alfresco Solr Query Analytics Changelog

## v2.0.1/2 - 23-07-2024
* [ETHALFOS24-54] Fixed bug that triggered ACS 7.4.x to load two different Log4J2 versions

## v2.0.0 - 17-07-2024
* [ETHALFOS24-42] Update for Alfresco version 7.4.1.3

## v1.0.0 - 14-09-2022
* [ALFREDOPS-813] Switched from bean-injection to AOP implementation

## v0.0.4 - 20-06-2022

### Added
* [ALFREDOPS-830] Escaped query handling[#21]


## v0.0.3 - 10-03-2022

### Added
* [ALFREDOPS-814] Disable logging for search check[#20]

### Changed
* [ALFREDOPS-813] Refactor to SolrDebugQueryHTTPClient[#19]

## v0.0.2 - 30-09-2021

### Added
* [ETHIASSLA-422] Custom appender for log rotation
Fix publishing via github actions.

## v0.0.1 - 09-08-2021

### Added
* [ETHIASSLA-400] Rename solr-debug into solr-query-analytics
* [XENOPS-858, XENOPS-865] SolrTimingParser for parsing timing information. Dashboard.[#12][#13]	
* [XENOPS-857] Support for non-sharded setups [#8]
* [ETHIASSLA-355] Kibana dashboards for sharded setups and searched properties [#5]
* [ETHIASSLA-341] SolrQueryParser, for parsing properties searched for [#2]

[#2]: https://github.com/xenit-eu/alfresco-solr-debug/pull/2
[#5]: https://github.com/xenit-eu/alfresco-solr-debug/pull/5
[#8]: https://github.com/xenit-eu/alfresco-solr-debug/pull/8
[#12]: https://github.com/xenit-eu/alfresco-solr-debug/pull/12
[#13]: https://github.com/xenit-eu/alfresco-solr-debug/pull/13
[#19]: https://github.com/xenit-eu/alfresco-solr-debug/pull/19
[#20]: https://github.com/xenit-eu/alfresco-solr-debug/pull/20


