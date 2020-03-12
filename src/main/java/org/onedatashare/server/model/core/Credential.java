package org.onedatashare.server.model.core;

import lombok.Data;

@Data
public abstract class Credential {
  public EndpointType type;
}
