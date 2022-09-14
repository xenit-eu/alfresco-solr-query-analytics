# important notes:

The SolrJsonResultSet is extracted from:

[sha256] 2698a97859bd0594f8bc5dc687ad5168d0c0329798a642163eb51bfe1a7d6251  **alfresco-repository-7.42-sources.jar**

The original checksum of the unaltered **SolrJSONResultSet.java** file:

[sha256] 2e2608c5fd273128186b46e7d158407af37491f87188f752b87a7603c44de518  **SolrJSONResultSet.java**
This matches with the 6bf5b9f commit, after this the file is renamed:
https://github.com/Alfresco/alfresco-community-repo/commits/5e247e50b48eeda597eefada2cc990b1797e3689/source/java/org/alfresco/repo/search/impl/lucene/SolrJSONResultSet.java?browsing_rename_history=true&new_path=repository/src/main/java/org/alfresco/repo/search/impl/solr/SolrJSONResultSet.java&original_branch=1ff90242b0e53b1ab4f10b2d6cb49b982a0458fc


the altered **SolrJSONResultSet.java** file with the additional getResponseBodyAsJSONObject function:

[sha256] be0f32d6e37421b8fc0221c29a10ef6e6094a56c97e231603c9570a1bb4f9581  **SolrJSONResultSet.java**

This is then tailored to the 6.2 release, as we cannot guarantee this file remains unchanged between alfresco versions.