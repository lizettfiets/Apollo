package com.apollocurrency.aplwallet.apl.core.http.exchange.api;

import com.apollocurrency.aplwallet.apl.core.http.exchange.api.NotFoundException;

import javax.inject.Singleton;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
@Singleton
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2019-03-07T08:10:07.244Z")
public interface DexGetHistoryApiService {
      Response dexGetHistoryGet(String account,String pair,String type,SecurityContext securityContext)
      throws NotFoundException;
}
