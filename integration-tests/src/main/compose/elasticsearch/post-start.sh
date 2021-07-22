username=elastic
password=changeme
host=localhost
port=9200
protocol=http
hostkibana=localhost
portkibana=5601
customindex=solrlogs
replicas=1

############ ILM for custom index

ilm_body="{\"policy\": {\"phases\": {\"hot\": {\"actions\": {\"rollover\": {\"max_age\": \"3d\",\"max_size\": \"10gb\"}}},\"delete\": { \"min_age\": \"30d\",\"actions\": { \"delete\": {} }}}}}"

curl -XPUT -H 'Content-Type: application/json' -d "$ilm_body" -u "$username:$password" "$protocol://$host:$port/_ilm/policy/$customindex"

mapping=`cat mapping.json`

template_body="{\"index_patterns\": [\"$customindex*\"],\"settings\": {\"number_of_shards\": 1,\"number_of_replicas\": $replicas,\"index.lifecycle.name\": \"$customindex\",\"index.lifecycle.rollover_alias\": \"$customindex\",\"index.max_docvalue_fields_search\": \"200\",\"mapping\": {\"total_fields\": {\"limit\": \"10000\"}}},\"mappings\":$mapping }"

echo "$template_body" >/tmp/template_body

curl -XPUT -H 'Content-Type: application/json' -d "@/tmp/template_body" -u "$username:$password" "$protocol://$host:$port/_template/$customindex"


firstindex_body="{\"aliases\": {\"$customindex\":{\"is_write_index\": true}}}"

curl -XPUT -H 'Content-Type: application/json' -d "$firstindex_body" -u "$username:$password" "$protocol://$host:$port/$customindex-000001"

############ Index pattern in kibana

curl -XPOST -u "$username:$password" -H 'Content-Type: application/json' -H 'kbn-xsrf: this_is_required_header' -d "{\"attributes\":{\"title\":\"$customindex-*\",\"timeFieldName\":\"@timestamp\"}}" "$protocol://$hostkibana:$portkibana/api/saved_objects/index-pattern/$customindex-*?overwrite=true"
