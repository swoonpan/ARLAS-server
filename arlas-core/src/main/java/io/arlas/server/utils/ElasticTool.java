/*
 * Licensed to Gisaïa under one or more contributor
 * license agreements. See the NOTICE.txt file distributed with
 * this work for additional information regarding copyright
 * ownership. Gisaïa licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.arlas.server.utils;

import com.fasterxml.jackson.databind.ObjectReader;
import io.arlas.server.exceptions.ArlasException;
import io.arlas.server.exceptions.InternalServerErrorException;
import io.arlas.server.exceptions.NotFoundException;
import io.arlas.server.model.CollectionReference;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.logging.log4j.core.util.IOUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetFieldMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.joda.Joda;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.IndexNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;
import java.util.List;

public class ElasticTool {

    private static final String ES_DATE_TYPE = "date";
    private static final String ES_TYPE = "type";

    public static CreateIndexResponse createArlasIndex(Client client, String arlasIndexName, String arlasMappingName, String arlasMappingFileName)  {
        CreateIndexResponse createIndexResponse = null;
        try {
            String arlasMapping = IOUtils.toString(new InputStreamReader(ElasticTool.class.getClassLoader().getResourceAsStream(arlasMappingFileName)));
            createIndexResponse = client.admin().indices().prepareCreate(arlasIndexName).addMapping(arlasMappingName, arlasMapping, XContentType.JSON).get();
        } catch (IOException e) {
            new InternalServerErrorException("Can not initialize the collection database", e);
        }
        return createIndexResponse;
    }

    public static AcknowledgedResponse putExtendedMapping(Client client, String arlasIndexName, String arlasMappingName, InputStream in) {
        AcknowledgedResponse putMappingResponse = null;
        try {
            String arlasMapping = IOUtils.toString(new InputStreamReader(in));
            putMappingResponse = client.admin().indices().preparePutMapping(arlasIndexName).setType(arlasMappingName).setSource(arlasMapping, XContentType.JSON).get();
        } catch (IOException e) {
            new InternalServerErrorException("Cannot update " + arlasIndexName + " mapping");
        }
        return putMappingResponse;
    }

    public static boolean checkIndexMappingFields(Client client, String index, String typeName, String... fields) throws ArlasException {
        GetFieldMappingsResponse response = client.admin().indices().prepareGetFieldMappings(index).setTypes(typeName).setFields(fields).get();
        for (String field : fields) {
            GetFieldMappingsResponse.FieldMappingMetaData data = response.fieldMappings(index, typeName, field);
            if (data == null || data.isNull()) {
                throw new NotFoundException("Unable to find " + field + " from " + typeName + " in " + index + ".");
            }
        }
        return true;
    }

    public static boolean checkAliasMappingFields(Client client, String alias, String typeName, String... fields) throws ArlasException {
        List<String> indeces = ElasticTool.getIndecesName(client, alias, typeName);
        for (String index : indeces) { checkIndexMappingFields(client, index, typeName, fields); }
        return true;
    }

    public static List<String> getIndecesName(Client client, String alias, String typeName) throws ArlasException {
        GetMappingsResponse response;
        try {
            //check index
            response = client.admin().indices().prepareGetMappings(alias)/*.setTypes(collectionReference.params.typeName)*/.get();
            if (response.getMappings().isEmpty()) {
                throw new NotFoundException("No types in " + alias + ".");
            }
        } catch (ArlasException e) {
            throw e;
        } catch (IndexNotFoundException e) {
            throw new NotFoundException("Index " + alias + " does not exist.");
        } catch (Exception e) {
            throw new NotFoundException("Unable to access " + typeName + " in " + alias + ".");
        }

        List<String> indeces = IteratorUtils.toList(response.getMappings().keysIt());
        for (String index : indeces) {
            //check type
            try {
                if (!response.getMappings().get(index).containsKey(typeName)) {
                    throw new NotFoundException("Type " + typeName + " does not exist in " + alias + ".");
                }
                Object properties = response.getMappings().get(index).get(typeName).sourceAsMap().get("properties");
                if (properties == null) {
                    throw new NotFoundException("Unable to find properties from " + typeName + " in " + index + ".");
                }
            } catch (Exception e) {
                throw new NotFoundException("Unable to get " + typeName + " in " + index + ".");
            }
        };
        return indeces;
    }

    public static CollectionReference getCollectionReferenceFromES(Client client, String index, String type, ObjectReader reader, String ref) throws ArlasException {
        CollectionReference collection = new CollectionReference(ref);
        //Exclude old include_fields for support old collection
        GetResponse hit = client.prepareGet(index, type, ref).setFetchSource(null, "include_fields").get();
        String source = hit.getSourceAsString();
        if (source != null) {
            try {
                collection.params = reader.readValue(source);
            } catch (IOException e) {
                throw new InternalServerErrorException("Can not fetch collection " + ref, e);
            }
        } else {
            throw new NotFoundException("Collection " + ref + " not found.");
        }
        return collection;
    }

    public static boolean isDateField(String field, Client client, String index, String typeName) throws ArlasException {
        GetFieldMappingsResponse response;
        try {
            response = client.admin().indices().prepareGetFieldMappings(index).setTypes(typeName).setFields(field).get();
        } catch (IndexNotFoundException e) {
            throw new NotFoundException("Index " + index + " does not exist.");
        }
        String lastKey = field.substring(field.lastIndexOf(".") + 1);
        return response.mappings().keySet()
                .stream()
                .anyMatch(indexName -> {
                    GetFieldMappingsResponse.FieldMappingMetaData data = response.fieldMappings(indexName, typeName, field);
                    boolean isFieldMetadaAMap = (data != null && !data.isNull() && data.sourceAsMap().get(lastKey) instanceof Map);
                    if (isFieldMetadaAMap) {
                        return Optional.of(((Map)data.sourceAsMap().get(lastKey)))
                                .map(m -> m.get(ES_TYPE))
                                .map(Object::toString)
                                .filter(t -> t.equals(ES_DATE_TYPE))
                                .isPresent();
                    } else {
                        // TODO : check if there is another way to fetch field type in this case
                        return false;
                    }
                });
    }

    public static Joda.EpochTimeParser getElasticEpochTimeParser(boolean isMilliSecond) {
        return new Joda.EpochTimeParser(BooleanUtils.isTrue(isMilliSecond));
    }

    public static Joda.EpochTimePrinter getElasticEpochTimePrinter(boolean isMilliSecond) {
        return new Joda.EpochTimePrinter(BooleanUtils.isTrue(isMilliSecond));
    }
}
