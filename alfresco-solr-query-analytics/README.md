# important notes:

The SolrJsonResultSet is extracted from:

[sha256] 03bacac73bf2a5bad2600216b6249b673cea090c884cdfd18eb1ab2d51b97580  **alfresco-repository-21.20-sources.jar**

The original checksum of the unaltered **SolrJSONResultSet.java** file:

[sha256] b8ba53b23e58c973cf0e98f37396adeda9782dd3c359ca1aaf3fd2ddc546fc5c  **SolrJSONResultSet.java**

This matches with the 295a8f7 commit: https://github.com/Alfresco/alfresco-community-repo/commit/295a8f7ba2925308b54a5599289ab0ea2609bb3b

The **SolrJSONResultSet.java** file is altered with an additional getResponseBodyAsJSONObject function
and tailored to the 7.4 release, as we cannot guarantee this file remains unchanged between alfresco versions.