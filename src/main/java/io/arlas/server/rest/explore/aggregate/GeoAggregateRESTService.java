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

package io.arlas.server.rest.explore.aggregate;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.arlas.server.exceptions.ArlasException;
import io.arlas.server.model.CollectionReference;
import io.arlas.server.model.request.AggregationsRequest;
import io.arlas.server.model.request.MixedRequest;
import io.arlas.server.model.response.AggregationResponse;
import io.arlas.server.model.response.Error;
import io.arlas.server.rest.explore.Documentation;
import io.arlas.server.rest.explore.ExploreRESTServices;
import io.arlas.server.rest.explore.ExploreServices;
import io.arlas.server.utils.ParamsParser;
import io.dropwizard.jersey.params.LongParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.geojson.Point;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class GeoAggregateRESTService extends ExploreRESTServices {

    public GeoAggregateRESTService(ExploreServices exploreServices) {
        super(exploreServices);
    }

    @Timed
    @Path("{collection}/_geoaggregate")
    @GET
    @Produces(UTF8JSON)
    @Consumes(UTF8JSON)
    @ApiOperation(value = "GeoAggregate", produces = UTF8JSON, notes = Documentation.GEOAGGREGATION_OPERATION, consumes = UTF8JSON, response = FeatureCollection.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation", response = FeatureCollection.class, responseContainer = "FeatureCollection" ),
            @ApiResponse(code = 500, message = "Arlas Server Error.", response = Error.class), @ApiResponse(code = 400, message = "Bad request.", response = Error.class),
            @ApiResponse(code = 501, message = "Not implemented functionality.", response = Error.class)})
    public Response geoaggregate(
            // --------------------------------------------------------
            // ----------------------- PATH -----------------------
            // --------------------------------------------------------
            @ApiParam(
                    name = "collection",
                    value="collection",
                    allowMultiple = false,
                    required=true)
            @PathParam(value = "collection") String collection,

            // --------------------------------------------------------
            // ----------------------- AGGREGATION -----------------------
            // --------------------------------------------------------
            @ApiParam(name ="agg",
                    value= Documentation.GEOAGGREGATION_PARAM_AGG,
                    allowMultiple = false,
                    required=true)
            @QueryParam(value="agg") List<String> agg,

            // --------------------------------------------------------
            // ----------------------- FILTER -----------------------
            // --------------------------------------------------------
            @ApiParam(name ="f",
                    value= Documentation.FILTER_PARAM_F,
                    allowMultiple = true,
                    required=false)
            @QueryParam(value="f") List<String> f,

            @ApiParam(name ="q", value=Documentation.FILTER_PARAM_Q,
                    allowMultiple = false,
                    required=false)
            @QueryParam(value="q") String q,

            @ApiParam(name ="before", value=Documentation.FILTER_PARAM_BEFORE,
                    allowMultiple = false,
                    type = "integer",
                    required=false)
            @QueryParam(value="before") LongParam before,

            @ApiParam(name ="after", value=Documentation.FILTER_PARAM_AFTER,
                    allowMultiple = false,
                    type = "integer",
                    required=false)
            @QueryParam(value="after") LongParam after,

            @ApiParam(name ="pwithin", value=Documentation.FILTER_PARAM_PWITHIN,
                    allowMultiple = true,
                    required=false)
            @QueryParam(value="pwithin") String pwithin,

            @ApiParam(name ="gwithin", value=Documentation.FILTER_PARAM_GWITHIN,
                    allowMultiple = true,
                    required=false)
            @QueryParam(value="gwithin") String gwithin,

            @ApiParam(name ="gintersect", value=Documentation.FILTER_PARAM_GINTERSECT,
                    allowMultiple = true,
                    required=false)
            @QueryParam(value="gintersect") String gintersect,

            @ApiParam(name ="notpwithin", value=Documentation.FILTER_PARAM_NOTPWITHIN,
                    allowMultiple = true,
                    required=false)
            @QueryParam(value="notpwithin") String notpwithin,

            @ApiParam(name ="notgwithin", value=Documentation.FILTER_PARAM_NOTGWITHIN,
                    allowMultiple = true,
                    required=false)
            @QueryParam(value="notgwithin") String notgwithin,

            @ApiParam(name ="notgintersect", value=Documentation.FILTER_PARAM_NOTGINTERSECT,
                    allowMultiple = true,
                    required=false)
            @QueryParam(value="notgintersect") String notgintersect,

            @HeaderParam(value="Partition-Filter") String partitionFilter,

            // --------------------------------------------------------
            // ----------------------- FORM -----------------------
            // --------------------------------------------------------
            @ApiParam(name ="pretty", value=Documentation.FORM_PRETTY,
                    allowMultiple = false,
                    defaultValue = "false",
                    required=false)
            @QueryParam(value="pretty") Boolean pretty,

            @ApiParam(name ="human", value=Documentation.FORM_HUMAN,
                    allowMultiple = false,
                    defaultValue = "false",
                    required=false)
            @QueryParam(value="human") Boolean human,

            // --------------------------------------------------------
            // ----------------------- EXTRA -----------------------
            // --------------------------------------------------------
            @ApiParam(value = "max-age-cache", required = false)
            @QueryParam(value = "max-age-cache") Integer maxagecache
    ) throws InterruptedException, ExecutionException, IOException, NotFoundException, ArlasException, JsonProcessingException {
        Long startArlasTime = System.nanoTime();
        CollectionReference collectionReference = exploreServices.getDaoCollectionReference()
                .getCollectionReference(collection);
        if (collectionReference == null) {
            throw new NotFoundException(collection);
        }
        AggregationsRequest aggregationsRequest = new AggregationsRequest();
        aggregationsRequest.filter = ParamsParser.getFilter(f,q,before,after,pwithin,gwithin,gintersect,notpwithin,notgwithin,notgintersect);
        aggregationsRequest.aggregations = ParamsParser.getAggregations(agg);
        AggregationsRequest aggregationsRequestHeader = new AggregationsRequest();
        aggregationsRequestHeader.filter = ParamsParser.getFilter(partitionFilter);
        MixedRequest request = new MixedRequest();
        request.basicRequest = aggregationsRequest;
        request.headerRequest = aggregationsRequestHeader;
        FeatureCollection fc = getFeatureCollection(request,collectionReference);
        return cache(Response.ok(fc),maxagecache);
    }

    @Timed
    @Path("{collection}/_geoaggregate")
    @POST
    @Produces(UTF8JSON)
    @Consumes(UTF8JSON)
    @ApiOperation(value = "GeoAggregate", produces = UTF8JSON, notes = Documentation.GEOAGGREGATION_OPERATION, consumes = UTF8JSON, response = FeatureCollection.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation", response = FeatureCollection.class, responseContainer = "FeatureCollection" ),
            @ApiResponse(code = 500, message = "Arlas Server Error.", response = Error.class), @ApiResponse(code = 400, message = "Bad request.", response = Error.class),
            @ApiResponse(code = 501, message = "Not implemented functionality.", response = Error.class)})
    public Response geoaggregatePost(
            // --------------------------------------------------------
            // ----------------------- PATH -----------------------
            // --------------------------------------------------------
            @ApiParam(
                    name = "collection",
                    value = "collection",
                    allowMultiple = false,
                    required = true)
            @PathParam(value = "collection") String collection,
            // --------------------------------------------------------
            // ----------------------- AGGREGATION -----------------------
            // --------------------------------------------------------
            AggregationsRequest aggregationRequest,

            // --------------------------------------------------------
            // -----------------------  FILTER  -----------------------
            // --------------------------------------------------------

            @HeaderParam(value="Partition-Filter") String partitionFilter,

            // --------------------------------------------------------
            // ----------------------- EXTRA -----------------------
            // --------------------------------------------------------
            @ApiParam(value = "max-age-cache", required = false)
            @QueryParam(value = "max-age-cache") Integer maxagecache
    ) throws InterruptedException, ExecutionException, IOException, NotFoundException, ArlasException {
        CollectionReference collectionReference = exploreServices.getDaoCollectionReference()
                .getCollectionReference(collection);
        if (collectionReference == null) {
            throw new NotFoundException(collection);
        }

        AggregationsRequest aggregationsRequestHeader = new AggregationsRequest();
        aggregationsRequestHeader.filter = ParamsParser.getFilter(partitionFilter);
        MixedRequest request = new MixedRequest();
        request.basicRequest = aggregationRequest;
        request.headerRequest = aggregationsRequestHeader;

        FeatureCollection fc = getFeatureCollection(request,collectionReference);

        return cache(Response.ok(fc),maxagecache);
    }

    private FeatureCollection getFeatureCollection(MixedRequest request, CollectionReference collectionReference) throws ArlasException, IOException{
        FeatureCollection fc;
        AggregationResponse aggregationResponse = new AggregationResponse();
        SearchResponse response = this.getExploreServices().aggregate(request,collectionReference,true);
        MultiBucketsAggregation aggregation;
        aggregation = (MultiBucketsAggregation)response.getAggregations().asList().get(0);
        aggregationResponse = this.getExploreServices().formatAggregationResult(aggregation, aggregationResponse);
        fc = toGeoJson(aggregationResponse);
        return fc;
    }

    private FeatureCollection toGeoJson(AggregationResponse aggregationResponse) throws IOException{
        FeatureCollection fc = new FeatureCollection();
        List<AggregationResponse> elements = aggregationResponse.elements;
        if (elements != null && elements.size()>0){
            for (AggregationResponse element : elements){
                Feature feature = new Feature();
                Map<String,Object> properties = new HashMap<>();
                GeoPoint geoPoint = (GeoPoint)element.key;
                properties.put("count", element.count);
                properties.put("geohash", element.keyAsString);
                properties.put("elements", element.elements);
                feature.setProperties(properties);
                GeoJsonObject g = new Point(geoPoint.getLon(),geoPoint.getLat());
                feature.setGeometry(g);
                fc.add(feature);
            }
        }
        return fc;
    }
}
