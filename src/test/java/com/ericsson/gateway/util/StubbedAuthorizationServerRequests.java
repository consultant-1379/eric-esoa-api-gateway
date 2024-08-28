/*
 * Copyright Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 */
package com.ericsson.gateway.util;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

public final class StubbedAuthorizationServerRequests {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(StubbedAuthorizationServerRequests.class);

  private static final String RESOURCE_RESPONSE =
      "{\n"
          + "  \"name\": \"Resource 1\",\n"
          + "  \"type\": \"urn:eo:resources:default\",\n"
          + "  \"owner\": {\n"
          + "    \"id\": \"216b2c74-fec0-4655-a314-bd66c12632e6\"\n"
          + "  },\n"
          + "  \"ownerManagedAccess\": false,\n"
          + "  \"attributes\": {},\n"
          + "  \"_id\": \"80e8ff6c-04d1-4ab4-8fff-bb075d4961de\",\n"
          + "  \"uris\": [\"/resource1\", \"/resource1/{id}\"],\n"
          + "  \"scopes\": [{\n"
          + "    \"name\": \"GET\"\n"
          + "  }\n"
          + "  ]\n"
          + "}";

  private final WireMockServer wireMockServer;

  public StubbedAuthorizationServerRequests(WireMockServer wireMockServer) {
    this.wireMockServer = wireMockServer;
  }

  public void resetMappingsAndRequests() {
    wireMockServer.resetMappings();
    wireMockServer.resetRequests();
  }

  public StubbedAuthorizationServerRequests stubAuthorizationSuccessWithGetScope() {
    wireMockServer.stubFor(
        post(urlPathMatching(".*/token"))
            .withRequestBody(
                containing("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Auma-ticket"))
            .withRequestBody(matching(".*permission=.*GET.*"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"access_token\": \"rpt\",\"refresh_token\": \"rpt_refresh\",\"expires_in\": \"60\",\"token_type\": \"bearer\"}")));
    return this;
  }

  public StubbedAuthorizationServerRequests stubAuthorizationSuccessWithoutScope() {
    wireMockServer.stubFor(
        post(urlPathMatching(".*/token"))
            .withRequestBody(
                containing("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Auma-ticket"))
            .withRequestBody(notMatching(".*permission=.*%23.*"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"access_token\": \"rpt\",\"refresh_token\": \"rpt_refresh\",\"expires_in\": \"60\",\"token_type\": \"bearer\"}")));
    return this;
  }

  public StubbedAuthorizationServerRequests stubAuthorizationDenied() {
    wireMockServer.stubFor(
        post(urlPathMatching(".*/token"))
            .withRequestBody(
                containing("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Auma-ticket"))
            .willReturn(
                aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"error\": \"access_denied\",\"error_description\": \"not_authorized\"}")));
    return this;
  }

  public StubbedAuthorizationServerRequests stubGetProtectionApiToken() {
    wireMockServer.stubFor(
        post(urlPathMatching(".*/token"))
            .withRequestBody(
                equalTo("grant_type=client_credentials&client_id=eo&client_secret=secret"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"access_token\": \"pat\",\"refresh_token\": \"pat_refresh\",\"expires_in\": \"60\",\"token_type\": \"bearer\"}")));
    return this;
  }

  public StubbedAuthorizationServerRequests stubVerifyAccessToken() {
    wireMockServer.stubFor(
        post(urlPathMatching(".*/token"))
            .withRequestBody(
                containing("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Auma-ticket"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"access_token\": \"rpt\",\"refresh_token\": \"rpt_refresh\",\"expires_in\": \"60\",\"token_type\": \"bearer\"}")));
    return this;
  }

  public StubbedAuthorizationServerRequests stubGetProtectedResources() {
    wireMockServer.stubFor(
        get(urlPathMatching(".*/resource_set"))
            .willReturn(
                aResponse().withHeader("Content-Type", "application/json").withBody("[\"1\"]")));

    wireMockServer.stubFor(
        get(urlPathMatching(".*/resource_set/1"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(RESOURCE_RESPONSE)));
    return this;
  }

  public StubbedAuthorizationServerRequests stubGetDuplicateProtectedResources() {
    wireMockServer.stubFor(
        get(urlPathMatching(".*/resource_set"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("[\"1\", \"2\"]")));

    wireMockServer.stubFor(
        get(urlPathMatching(".*/resource_set/1"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(RESOURCE_RESPONSE)));

    wireMockServer.stubFor(
        get(urlPathMatching(".*/resource_set/2"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(RESOURCE_RESPONSE)));
    return this;
  }

  public StubbedAuthorizationServerRequests stubGetProtectionPasswordApiToken() {
    wireMockServer.stubFor(
        post(urlPathMatching(".*/token"))
            .withRequestBody(containing("grant_type=password&client_id=eo&client_secret=secret"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"access_token\": \"pat\",\"refresh_token\": \"pat_refresh\",\"expires_in\": \"60\",\"token_type\": \"bearer\"}")));
    return this;
  }

  public StubbedAuthorizationServerRequests stubGetProtectionPasswordApiTokenError(int status) {
    wireMockServer.stubFor(
        post(urlPathMatching(".*/token"))
            .withRequestBody(containing("grant_type=password&client_id=eo&client_secret=secret"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(status)
                    .withBody(
                        "{\"path\": \"/auth/v1/login\",\"status\": \"UNAUTHORIZED\",\"error\": \"Internal Server Error\",\"message\": \"Authorization Failed!\"}")));
    return this;
  }

  public StubbedAuthorizationServerRequests stubTokenLogout() {
    wireMockServer.stubFor(
        post(urlPathMatching(".*/logout"))
            .willReturn(aResponse().withStatus(HttpStatus.NO_CONTENT.value())));
    return this;
  }

  public StubbedAuthorizationServerRequests stubTokenLogoutWithError() {
    wireMockServer.stubFor(
        post(urlPathMatching(".*/logout"))
            .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));
    return this;
  }
}
