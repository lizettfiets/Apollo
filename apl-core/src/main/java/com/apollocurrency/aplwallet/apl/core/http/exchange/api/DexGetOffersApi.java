package com.apollocurrency.aplwallet.apl.core.http.exchange.api;

import com.apollocurrency.aplwallet.apl.core.http.exchange.model.*;
import com.apollocurrency.aplwallet.apl.core.http.exchange.api.DexGetOffersApiService;

import io.swagger.annotations.ApiParam;

import java.math.BigDecimal;
import com.apollocurrency.aplwallet.apl.core.http.exchange.model.Error;
import com.apollocurrency.aplwallet.apl.core.http.exchange.model.Orders;

import java.util.Map;
import java.util.List;
import com.apollocurrency.aplwallet.apl.core.http.exchange.api.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.*;
import javax.inject.Inject;

import javax.validation.constraints.*;

@Path("/dexGetOffers")

@Produces({ "application/json" })
@io.swagger.annotations.Api(description = "the dexGetOffers API")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2019-03-07T08:10:07.244Z")
public class DexGetOffersApi  {

    @Inject DexGetOffersApiService service;

    @GET
    
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Get exchange offers", notes = "dexGetOffers endpoint list of opened pending exchange orders", response = Orders.class, responseContainer = "List", tags={  })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Exchange offers", response = Orders.class, responseContainer = "List"),
        
        @io.swagger.annotations.ApiResponse(code = 200, message = "Unexpected error", response = Error.class) })
    public Response dexGetOffersGet(  @QueryParam("account") String account,  @QueryParam("pair") String pair,  @QueryParam("type") String type,  @QueryParam("minAskPrice") BigDecimal minAskPrice,  @QueryParam("maxBidPrice") BigDecimal maxBidPrice,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.dexGetOffersGet(account,pair,type,minAskPrice,maxBidPrice,securityContext);
    }
}
