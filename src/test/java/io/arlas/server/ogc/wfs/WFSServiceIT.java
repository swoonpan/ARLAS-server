package io.arlas.server.wfs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.arlas.server.AbstractTestWithCollection;
import io.arlas.server.model.request.Expression;
import io.arlas.server.model.request.Filter;
import io.arlas.server.model.request.MultiValueFilter;
import io.arlas.server.model.request.OperatorEnum;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class WFSServiceIT extends AbstractTestWithCollection {

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected String getUrlPath(String collection) {
        return arlasPath + "wfs/" + collection;
    }

    @Test
    public void testHeaderFilter() throws Exception {
        request.filter.f = Arrays.asList(new MultiValueFilter<>(new Expression("params.job", OperatorEnum.like, "Architect")),//"job:eq:Architect"
                new MultiValueFilter<>(new Expression("params.startdate", OperatorEnum.range, "[1009799<1009801]")));
        handleHeaderFilter(
                get(Arrays.asList(
                        new ImmutablePair<>("SERVICE", "WFS"),
                        new ImmutablePair<>("VERSION", "2.0.0"),
                        new ImmutablePair<>("COUNT", "1000"),
                        new ImmutablePair<>("REQUEST", "GetFeature")), request.filter)
        );
    }

    @Test
    public void testNoHeaderFilter() throws Exception {
        handleNoHeaderFilter(
                get(Arrays.asList(
                        new ImmutablePair<>("SERVICE", "WFS"),
                        new ImmutablePair<>("VERSION", "2.0.0"),
                        new ImmutablePair<>("COUNT", "1000"),
                        new ImmutablePair<>("REQUEST", "GetFeature")), new Filter())
        );
    }

    @Test
    public void testDescribeFeature() throws Exception {
        handleDescribeFeature(
                get(Arrays.asList(
                        new ImmutablePair<>("SERVICE", "WFS"),
                        new ImmutablePair<>("VERSION", "2.0.0"),
                        new ImmutablePair<>("REQUEST", "DescribeFeatureType")), new Filter())
        );
    }

    public void handleHeaderFilter(ValidatableResponse then) throws Exception {
        then.statusCode(200)
                .body("wfs:FeatureCollection.@numberReturned", equalTo("2"))
                .body("wfs:FeatureCollection.member[1].geodata.params_job", equalTo("Architect"))
                .body("wfs:FeatureCollection.member[1].geodata.params_country.size()", equalTo(0))
                .body("wfs:FeatureCollection.member[1].geodata.params_city.size()", equalTo(0));
    }

    public void handleNoHeaderFilter(ValidatableResponse then) throws Exception {
        then.statusCode(200)
                .body("wfs:FeatureCollection.@numberReturned", equalTo("595"))
                .body("wfs:FeatureCollection.member[1].geodata.params_job", equalTo("Dancer"))
                .body("wfs:FeatureCollection.member[1].geodata.params_country.size()", equalTo(0))
                .body("wfs:FeatureCollection.member[1].geodata.params_city.size()", equalTo(0));
    }

    public void handleDescribeFeature(ValidatableResponse then) throws Exception {
        then.statusCode(200)
                .body("xs:schema.complexType.complexContent.extension.sequence.element.size()", equalTo(6));
    }

    protected RequestSpecification givenFilterableRequestParams() {
        return given().contentType("application/xml");
    }

    private ValidatableResponse get(List<Pair<String, String>> params, Filter headerFilter) throws JsonProcessingException {
        RequestSpecification req = givenFilterableRequestParams().header("Partition-Filter", objectMapper.writeValueAsString(headerFilter));
        for (Pair<String, String> param : params) {
            req = req.param(param.getKey(), param.getValue());
        }
        return req
                .when().get(getUrlPath("geodata"))
                .then();
    }
}