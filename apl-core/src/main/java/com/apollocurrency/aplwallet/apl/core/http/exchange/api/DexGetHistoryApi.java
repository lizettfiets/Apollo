package com.apollocurrency.aplwallet.apl.core.http.exchange.api;

import com.apollocurrency.aplwallet.apl.core.http.exchange.model.*;
import com.apollocurrency.aplwallet.apl.core.http.exchange.api.DexGetHistoryApiService;

import io.swagger.annotations.ApiParam;

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

@Path("/dexGetHistory")

@Produces({ "application/json" })
@io.swagger.annotations.Api(description = "the dexGetHistory API")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2019-03-07T08:10:07.244Z")
public class DexGetHistoryApi  {

    @Inject DexGetHistoryApiService service;

    @GET
    
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "get trading history for certain account", notes = "get trading history for certain account", response = Orders.class, responseContainer = "List", tags={  })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Wallets balances", response = Orders.class, responseContainer = "List"),
        
        @io.swagger.annotations.ApiResponse(code = 200, message = "Unexpected error", response = Error.class) })
    public Response dexGetHistoryGet( @NotNull  @QueryParam("account") String account,  @QueryParam("pair") String pair,  @QueryParam("type") String type,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.dexGetHistoryGet(account,pair,type,securityContext);
    }
}
