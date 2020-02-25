package org.onedatashare.server.model.useraction;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.onedatashare.module.globusapi.EndPoint;
import org.springframework.data.annotation.Transient;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserActionCredential {
  private String type;
  private String uuid;
  private String name;

  private EndPoint globusEndpoint;

  @Transient
  private boolean tokenSaved;
  @Transient
  private String token;
  @Transient
  private String username;
  @Transient
  private String password;
}
