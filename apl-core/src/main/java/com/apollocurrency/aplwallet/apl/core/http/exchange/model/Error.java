package com.apollocurrency.aplwallet.apl.core.http.exchange.model;

import java.util.Objects;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.math.BigDecimal;
import javax.validation.constraints.*;
import io.swagger.annotations.*;


@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2019-03-07T08:10:07.244Z")
public class Error   {
  
  private String errorDescription = null;
  private BigDecimal errorCode = null;

  /**
   * Error description
   **/
  
  @ApiModelProperty(value = "Error description")
  @JsonProperty("errorDescription")
  public String getErrorDescription() {
    return errorDescription;
  }
  public void setErrorDescription(String errorDescription) {
    this.errorDescription = errorDescription;
  }

  /**
   * Error Code
   **/
  
  @ApiModelProperty(value = "Error Code")
  @JsonProperty("errorCode")
  public BigDecimal getErrorCode() {
    return errorCode;
  }
  public void setErrorCode(BigDecimal errorCode) {
    this.errorCode = errorCode;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Error error = (Error) o;
    return Objects.equals(errorDescription, error.errorDescription) &&
        Objects.equals(errorCode, error.errorCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(errorDescription, errorCode);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Error {\n");
    
    sb.append("    errorDescription: ").append(toIndentedString(errorDescription)).append("\n");
    sb.append("    errorCode: ").append(toIndentedString(errorCode)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

