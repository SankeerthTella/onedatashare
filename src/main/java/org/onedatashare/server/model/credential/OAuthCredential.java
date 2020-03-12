package org.onedatashare.server.model.credential;

import lombok.Data;
import org.onedatashare.server.model.core.Credential;
import org.onedatashare.server.model.core.EndpointType;

import java.util.Date;

@Data
public class OAuthCredential extends Credential {
  public transient String token;
  public String name;
  public String dropboxID;
  public String refreshToken;
  public Date expiredTime;
  public boolean refreshTokenExp = false;
  public Date lastRefresh;

  public OAuthCredential(String token) {
    this.type = EndpointType.DROPBOX;
    this.token = token;
  }
}
