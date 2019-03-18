package com.apollocurrency.aplwallet.apl.core.http.exchange.api;

import com.apollocurrency.aplwallet.apl.core.http.exchange.model.*;
import com.apollocurrency.aplwallet.apl.core.http.exchange.api.DexGetBalancesApiService;

import io.swagger.annotations.ApiParam;


import com.apollocurrency.aplwallet.apl.core.http.exchange.model.Balances;
import com.apollocurrency.aplwallet.apl.core.http.exchange.model.Error;

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

@Path("/dexGetBalances")

@Produces({ "application/json" })
@io.swagger.annotations.Api(description = "the dexGetBalances API")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2019-03-07T08:10:07.244Z")
public class DexGetBalancesApi  {

    @Inject DexGetBalancesApiService service;

    @GET
    
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Balances of cryptocurrency wallets", notes = "dexGetBalances endpoint returns cryptocurrency wallets' (ETH/BTC/PAX) balances", response = Balances.class, tags={  })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Wallets balances", response = Balances.class),
        
        @io.swagger.annotations.ApiResponse(code = 200, message = "Unexpected error", response = Error.class) })
    public Response dexGetBalancesGet( @NotNull  @QueryParam("account") String account, @NotNull  @QueryParam("secretPhrase") String secretPhrase,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.dexGetBalancesGet(account,secretPhrase,securityContext);
    }
}
