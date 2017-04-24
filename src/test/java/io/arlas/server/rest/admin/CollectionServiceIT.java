package io.arlas.server.rest.admin;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import io.restassured.RestAssured;

public class CollectionServiceIT {

    static {
        RestAssured.baseURI = "http://arlas-server";
        RestAssured.port = 9999;
        RestAssured.basePath = "/arlas";
    }

    @Test
    public void testLifecycle() throws Exception {
        Map<String, Object> jsonAsMap = new HashMap<String, Object>();
        jsonAsMap.put("index_name", "bar");
        jsonAsMap.put("type_name", "type");
        jsonAsMap.put("id_path", "path");
        jsonAsMap.put("geometry_path", "geopath");
        jsonAsMap.put("centroid_path", "centroidpath");
        jsonAsMap.put("timestamp_path", "tspath");

        // PUT new collection
        given().contentType("application/json").body(jsonAsMap)
        .when().put("/collections/foo")
        .then().statusCode(200);

        // GET collection
        when().get("/collections/foo")
        .then().statusCode(200)
            .body("collection_name", equalTo("foo"))
            .body("params.index_name", equalTo("bar"))
            .body("params.type_name", equalTo("type"))
            .body("params.id_path", equalTo("path"))
            .body("params.geometry_path", equalTo("geopath"))
            .body("params.centroid_path", equalTo("centroidpath"))
            .body("params.timestamp_path", equalTo("tspath"));

        // DELETE collection
        when().delete("/collections/foo")
        .then().statusCode(200);

        // GET deleted collection
        when().get("/collections/foo")
        .then().statusCode(404);
    }
}
