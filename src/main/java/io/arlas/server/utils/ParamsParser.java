package io.arlas.server.utils;

import io.arlas.server.core.FluidSearch;
import io.arlas.server.exceptions.ArlasException;
import io.arlas.server.exceptions.BadRequestException;
import io.arlas.server.exceptions.InvalidParameterException;
import io.arlas.server.model.request.*;
import io.dropwizard.jersey.params.IntParam;
import io.dropwizard.jersey.params.LongParam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by hamou on 24/04/17.
 */
public class ParamsParser {
    private static final String AGG_INTERVAL_PARAM = "interval-";
    private static final String AGG_FORMAT_PARAM = "format-";
    private static final String AGG_COLLECT_FIELD_PARAM = "collect_field-";
    private static final String AGG_COLLECT_FCT_PARAM = "collect_fct-";
    private static final String AGG_ORDER_PARAM = "order-";
    private static final String AGG_ON_PARAM = "on-";
    private static final String AGG_SIZE_PARAM = "size-";


    public static List<Aggregation> getAggregations(List<String> agg) throws ArlasException{
        List<Aggregation> aggregations = new ArrayList<>();
        if (agg != null && agg.size()>0 ){
            for (String aggregation : agg){
                Aggregation aggregationModel;
                if (CheckParams.isAggregationParamValid(aggregation)) {
                    List<String> aggParameters = Arrays.asList(aggregation.split(":"));
                    aggregationModel = getAggregationModel(aggParameters);
                    aggregations.add(aggregationModel);
                }
            }
        }
        return aggregations;
    }

    public static Aggregation getAggregationModel(List<String> agg) throws ArlasException {
        Aggregation aggregationModel = new Aggregation();
        aggregationModel.type = AggregationTypeEnum.valueOf(agg.get(0));
        aggregationModel.field = agg.get(1);

        for (String parameter : agg){
            if (parameter.contains(AGG_INTERVAL_PARAM)){
                if(aggregationModel.type.equals(AggregationTypeEnum.datehistogram)){
                    aggregationModel.interval = getDateInterval(parameter.substring(AGG_INTERVAL_PARAM.length()));
                }else{
                    aggregationModel.interval = new Interval(Integer.parseInt(parameter.substring(AGG_INTERVAL_PARAM.length())), null);
                }
            }
            if (parameter.contains(AGG_FORMAT_PARAM)){
                aggregationModel.format = parameter.substring(AGG_FORMAT_PARAM.length());
            }
            if (parameter.contains(AGG_COLLECT_FIELD_PARAM)){
                aggregationModel.collectField = parameter.substring(AGG_COLLECT_FIELD_PARAM.length());
            }
            if (parameter.contains(AGG_COLLECT_FCT_PARAM)){
                aggregationModel.collectFct = parameter.substring(AGG_COLLECT_FCT_PARAM.length());
            }
            if (parameter.contains(AGG_ORDER_PARAM)){
                aggregationModel.order = AggregationOrderEnum.valueOf(parameter.substring(AGG_ORDER_PARAM.length()));
            }
            if (parameter.contains(AGG_ON_PARAM)){
                aggregationModel.on = AggregationOnEnum.valueOf(parameter.substring(AGG_ON_PARAM.length()));
            }
            if (parameter.contains(AGG_SIZE_PARAM)){
                aggregationModel.size = parameter.substring(AGG_SIZE_PARAM.length());
            }
        }
        return aggregationModel;
    }

    public static Interval getDateInterval(String intervalString) throws ArlasException{
        if (intervalString != null && !intervalString.equals("")){
            String[] sizeAndUnit = intervalString.split("(?<=[a-zA-Z])(?=\\d)|(?<=\\d)(?=[a-zA-Z])");
            if (sizeAndUnit.length == 2) {
                Interval interval = new Interval(tryParseInteger(sizeAndUnit[0]), UnitEnum.valueOf(sizeAndUnit[1].toLowerCase()));
                if(interval.value == null){
                    throw new InvalidParameterException(FluidSearch.INVALID_SIZE + sizeAndUnit[0]);
                }
                return interval;
            }
            else throw new InvalidParameterException("The date interval " + intervalString + "is not valid");
        }
        else throw new BadRequestException(FluidSearch.INTREVAL_NOT_SPECIFIED);
    }

    public static Integer getAggregationGeohasPrecision(Interval interval) throws ArlasException{
        if(interval != null){
           if( interval.value >12 || interval.value<1) {
               throw new InvalidParameterException("Invalid geohash aggregation precision of " + interval.value + ". Must be between 1 and 12.");
           }else return interval.value;
        }
        throw new BadRequestException(FluidSearch.INTREVAL_NOT_SPECIFIED);
    }

    public static String getValidAggregationFormat(String aggFormat){
        //TODO: check if format is in DateTimeFormat (joda)
        if (aggFormat != null) {
            return aggFormat;
        }
        else {
            return "yyyy-MM-dd-HH:mm:ss";
        }
    }

    public static Filter getFilter(List<String> f, String q, LongParam before, LongParam after, String pwithin, String gwithin, String gintersect,String notpwithin, String notgwithin, String notgintersect) throws ArlasException{
        Filter filter = new Filter();
        filter.f  = f;
        filter.q = q;
        if (after != null) {
            filter.after = after.get();
        }
        if (before != null) {
            filter.before = before.get();
        }
        filter.pwithin = pwithin;
        filter.gwithin = gwithin;
        filter.gintersect = gintersect;
        filter.notpwithin =notpwithin;
        filter.notgwithin = notgwithin;
        filter.notgintersect = notgintersect;
        return filter;
    }

    public static Size getSize(IntParam size, IntParam from) throws ArlasException{
        Size sizeObject = new Size();
        sizeObject.size = size.get();
        sizeObject.from = from.get();
        return sizeObject;
    }

    public static Sort getSort(String sort){
        Sort sortObject = new Sort();
        sortObject.sort = sort;
        return sortObject;
    }

    public static Integer getValidAggregationSize(String size) throws ArlasException{
        Integer s  = tryParseInteger(size);
        if (s != null){
            return s;
        }
        else throw new InvalidParameterException(FluidSearch.INVALID_SIZE);
    }
    
    private static Integer tryParseInteger(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double tryParseDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
