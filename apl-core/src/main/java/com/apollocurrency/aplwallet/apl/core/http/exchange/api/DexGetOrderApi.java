package com.apollocurrency.aplwallet.apl.core.http.exchange.api;

import com.apollocurrency.aplwallet.apl.core.http.exchange.model.*;
import com.apollocurrency.aplwallet.apl.core.http.exchange.api.DexGetOrderApiService;

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

@Path("/dexGetOrder")

@Produces({ "application/json" })
@io.swagger.annotations.Api(description = "the dexGetOrder API")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2019-03-07T08:10:07.244Z")
public class DexGetOrderApi  {

    @Inject DexGetOrderApiService service;

    @GET
    @Path("/{orderID}")
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "get Order By Id", notes = "extract one order by OrderID", response = Orders.class, tags={  })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Order", response = Orders.class),
        
        @io.swagger.annotations.ApiResponse(code = 200, message = "Unexpected error", response = Error.class) })
    public Response dexGetOrderOrderIDGet( @PathParam("orderID") BigDecimal orderID,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.dexGetOrderOrderIDGet(orderID,securityContext);
    }
}
