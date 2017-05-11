package io.arlas.server.model.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "ArlasMD", description = "Metadata of the ARLAS hit")
public class ArlasMD {
    @ApiModelProperty(name = "id", value = "The unique identifier of the hit")
    public String id;

    @ApiModelProperty(name = "timestamp", value = "The timestamp of the hit")
    public Long timestamp;

    @ApiModelProperty(name = "geometry", value = "The geometry of the hit")
    public Object geometry;

    @ApiModelProperty(name = "centroid", value = "The centroid of the hit")
    public Object centroid;
}