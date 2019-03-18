package com.apollocurrency.aplwallet.apl.core.http.exchange.api;

import com.apollocurrency.aplwallet.apl.core.http.exchange.api.*;
import com.apollocurrency.aplwallet.apl.core.http.exchange.model.*;


import java.math.BigDecimal;
import com.apollocurrency.aplwallet.apl.core.http.exchange.model.Error;
import com.apollocurrency.aplwallet.apl.core.http.exchange.model.Orders;

import java.util.List;
import com.apollocurrency.aplwallet.apl.core.http.exchange.api.NotFoundException;

import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2019-03-07T08:10:07.244Z")
public interface DexGetOffersApiService {
      Response dexGetOffersGet(String account,String pair,String type,BigDecimal minAskPrice,BigDecimal maxBidPrice,SecurityContext securityContext)
      throws NotFoundException;
}
