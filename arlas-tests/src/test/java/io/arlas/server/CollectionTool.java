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

package io.arlas.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.arlas.server.model.CollectionReference;
import io.arlas.server.model.DublinCoreElementName;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

public class CollectionTool extends AbstractTestContext {

    public static String COLLECTION_NAME = "geodata";


    public static void main(String[] args) throws IOException {
        switch (args[0]) {
            case "load":
                load(0);
                break;
            case "loadcsw":
                loadCsw(0);
                break;
            case "delete":
                delete();
                break;
            case "deletecsw":
                deleteCsw();
                break;
        }
        DataSetTool.close();
    }

    @Test
    public static void load(long sleepAfter) {
        try {
            DataSetTool.loadDataSet();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, Object> jsonAsMap = new HashMap<>();
        jsonAsMap.put(CollectionReference.INDEX_NAME, DataSetTool.DATASET_INDEX_NAME);
        jsonAsMap.put(CollectionReference.TYPE_NAME, DataSetTool.DATASET_TYPE_NAME);
        jsonAsMap.put(CollectionReference.ID_PATH, DataSetTool.DATASET_ID_PATH);
        jsonAsMap.put(CollectionReference.GEOMETRY_PATH, DataSetTool.DATASET_GEOMETRY_PATH);
        jsonAsMap.put(CollectionReference.CENTROID_PATH, DataSetTool.DATASET_CENTROID_PATH);
        jsonAsMap.put(CollectionReference.TIMESTAMP_PATH, DataSetTool.DATASET_TIMESTAMP_PATH);
        jsonAsMap.put(CollectionReference.EXCLUDE_FIELDS, DataSetTool.DATASET_EXCLUDE_FIELDS);
        jsonAsMap.put(CollectionReference.EXCLUDE_WFS_FIELDS, DataSetTool.DATASET_EXCLUDE_WFS_FIELDS);
        jsonAsMap.put(CollectionReference.TAGGABLE_FIELDS, DataSetTool.DATASET_TAGGABLE_FIELDS);

        // PUT new collection
        given().contentType("application/json").body(jsonAsMap).when().put(getUrlPath()).then().statusCode(200);

        try {
            Thread.sleep(sleepAfter);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public static void loadCsw(long sleepAfter) throws IOException {
        try {
            DataSetTool.loadDataSet();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        InputStreamReader dcelementForCollection = new InputStreamReader(CollectionTool.class.getClassLoader().getResourceAsStream("csw.collection.dcelements.json"));
        ObjectMapper objectMapper = new ObjectMapper();
        DublinCoreElementName[] dcelements = objectMapper.readValue(dcelementForCollection, DublinCoreElementName[].class);
        Arrays.asList(dcelements).forEach(dublinCoreElementName -> {
                    Map<String, Object> jsonAsMap = new HashMap<>();
                    jsonAsMap.put(CollectionReference.INDEX_NAME, DataSetTool.DATASET_INDEX_NAME);
                    jsonAsMap.put(CollectionReference.TYPE_NAME, DataSetTool.DATASET_TYPE_NAME);
                    jsonAsMap.put(CollectionReference.ID_PATH, DataSetTool.DATASET_ID_PATH);
                    jsonAsMap.put(CollectionReference.GEOMETRY_PATH, DataSetTool.DATASET_GEOMETRY_PATH);
                    jsonAsMap.put(CollectionReference.CENTROID_PATH, DataSetTool.DATASET_CENTROID_PATH);
                    jsonAsMap.put(CollectionReference.TIMESTAMP_PATH, DataSetTool.DATASET_TIMESTAMP_PATH);
                    jsonAsMap.put(CollectionReference.EXCLUDE_FIELDS, DataSetTool.DATASET_EXCLUDE_FIELDS);
                    jsonAsMap.put(CollectionReference.EXCLUDE_WFS_FIELDS, DataSetTool.DATASET_EXCLUDE_WFS_FIELDS);
                    jsonAsMap.put(CollectionReference.TAGGABLE_FIELDS, DataSetTool.DATASET_TAGGABLE_FIELDS);
                    jsonAsMap.put("dublin_core_element_name", dublinCoreElementName);
                    String url = arlasPath + "collections/" + dublinCoreElementName.title.split(" ")[0].toLowerCase();
                    // PUT new collection
                    given().contentType("application/json").body(jsonAsMap).when().put(url).then().statusCode(200);
                }
        );
        try {
            Thread.sleep(sleepAfter);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void delete() throws IOException {
        DataSetTool.clearDataSet();
        //DELETE collection
        when().delete(getUrlPath()).then().statusCode(200);
    }

    public static void deleteCsw() throws IOException {
        DataSetTool.clearDataSet();
        InputStreamReader dcelementForCollection = new InputStreamReader(CollectionTool.class.getClassLoader().getResourceAsStream("csw.collection.dcelements.json"));
        ObjectMapper objectMapper = new ObjectMapper();
        DublinCoreElementName[] dcelements = objectMapper.readValue(dcelementForCollection, DublinCoreElementName[].class);
        Arrays.asList(dcelements).forEach(dublinCoreElementName -> {
                    String url = arlasPath + "collections/" + dublinCoreElementName.title.split(" ")[0].toLowerCase();
                    //DELETE collection
                    when().delete(url).then().statusCode(200);
                }
        );
    }

    @Override
    protected String getUrlPath(String collection) {
        return getUrlPath();
    }

    protected static String getUrlPath() {
        return arlasPath + "collections/" + COLLECTION_NAME;
    }
}
