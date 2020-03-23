package org.onedatashare.server.service.oauth;


import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import org.onedatashare.server.model.credential.OAuthCredential;
import org.onedatashare.server.module.googledrive.GDriveConfig;
import org.onedatashare.server.module.googledrive.GoogleDriveSession;
import org.onedatashare.server.service.ODSLoggerService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

@Service
public class GDriveOauthService implements OAuthService{
    @Autowired
    private GDriveConfig driveConfig;

    private final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE_METADATA_READONLY, DriveScopes.DRIVE);

    private String getUrl() {
        try {
            // Build flow and trigger user authorization request.
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                            GDriveConfig.getHttpTransport(), GDriveConfig.getJsonFactory(),
                            driveConfig.getDriveClientSecrets(), SCOPES)
                            .setAccessType("offline").setApprovalPrompt("force")
                            .setDataStoreFactory(GDriveConfig.getDATA_STORE_FACTORY())
                            .build();

            AuthorizationCodeRequestUrl authorizationUrl =
                    flow.newAuthorizationUrl().setRedirectUri(driveConfig.getRedirectUri()).setState(flow.getClientId());

            return authorizationUrl.toURL().toString();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String start() {
        GoogleClientSecrets cs = driveConfig.getDriveClientSecrets();
        if (driveConfig.getRedirectUri() == null)
            throw new RuntimeException("Google Drive config missing");
        return getUrl();
    }

    private OAuthCredential storeCredential(String code) {
        try {
            GoogleAuthorizationCodeFlow flow = driveConfig.getFlow();
            // Build flow and trigger user authorization request.
            TokenResponse response = flow.newTokenRequest(code).setRedirectUri(driveConfig.getRedirectUri()).execute();

            OAuthCredential oauth = new OAuthCredential(response.getAccessToken());
            oauth.refreshToken = response.getRefreshToken();


            Calendar calendar = Calendar.getInstance(); // gets a calendar using the default time zone and locale.
            calendar.add(Calendar.SECOND, response.getExpiresInSeconds().intValue());

            oauth.expiredTime = calendar.getTime();

            flow.createAndStoreCredential(response, oauth.token);
            return oauth;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public OAuthCredential finish(Map<String, String> queryParameters) {
        String token = queryParameters.get("code");
        OAuthCredential oauth = storeCredential(token);
        try {
            Drive service = new GoogleDriveSession().getDriveService(oauth.getToken());
            String userId = service.about().get().setFields("user").execute().getUser().getEmailAddress();
            oauth.name = "GoogleDrive: " + userId;
            return oauth;
        } catch (Exception e) {
            ODSLoggerService.logError("Runtime exception occurred while finishing initializing Google drive oauth session");
            throw new RuntimeException(e);
        }
    }
}

