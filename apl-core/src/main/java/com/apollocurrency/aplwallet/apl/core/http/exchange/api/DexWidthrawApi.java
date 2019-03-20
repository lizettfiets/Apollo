package com.apollocurrency.aplwallet.apl.core.http.exchange.api;

import com.apollocurrency.aplwallet.apl.core.http.exchange.api.DexWidthrawApiService;


import java.math.BigDecimal;
import com.apollocurrency.aplwallet.apl.core.http.exchange.model.Error;
import com.apollocurrency.aplwallet.apl.core.http.exchange.model.TransactionDetails;

import com.apollocurrency.aplwallet.apl.core.http.exchange.api.NotFoundException;


import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.*;
import javax.inject.Inject;

import javax.validation.constraints.*;

@Path("/dexWidthraw")

@Produces({ "application/json" })
@io.swagger.annotations.Api(description = "the dexWidthraw API")
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2019-03-07T08:10:07.244Z")
public class DexWidthrawApi  {

    @Inject DexWidthrawApiService service;

    @POST
    
    
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "Widthraw cryptocurrency", notes = "dexWidthraw endpoint provides transfer of Ethereum", response = TransactionDetails.class, tags={  })
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Wallets balances", response = TransactionDetails.class),
        
        @io.swagger.annotations.ApiResponse(code = 200, message = "Unexpected error", response = Error.class) })
    public Response dexWidthrawPost( @NotNull  @QueryParam("account") String account, @NotNull  @QueryParam("secretPhrase") String secretPhrase, @NotNull  @QueryParam("amount") BigDecimal amount, @NotNull  @QueryParam("address") String address, @NotNull  @QueryParam("cryptocurrency") String cryptocurrency,@Context SecurityContext securityContext)
    throws NotFoundException {
        return service.dexWidthrawPost(account,secretPhrase,amount,address,cryptocurrency,securityContext);
    }
}
