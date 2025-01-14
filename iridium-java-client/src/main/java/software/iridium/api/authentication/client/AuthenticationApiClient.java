/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package software.iridium.api.authentication.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import software.iridium.api.authentication.domain.AuthenticationRequest;
import software.iridium.api.authentication.domain.AuthenticationResponse;
import software.iridium.api.authentication.domain.IdentityResponse;
import software.iridium.api.base.client.ErrorHandler;
import software.iridium.api.base.client.RestGetter;
import software.iridium.api.base.client.RestPoster;
import software.iridium.api.base.domain.ApiDataResponse;

public class AuthenticationApiClient {

  private final RestTemplate restTemplate;
  private final String authenticationBaseUrl;

  private static final String IDENTITIES_PATH_FMT = "%sidentities/";

  private static final String IDENTITIES_PATH_FMT_V2 = "%stenants/%s/identities/";

  public AuthenticationApiClient(
      final String authenticationBaseUrl, final RestTemplate restTemplate) {
    super();
    this.restTemplate = restTemplate;
    this.authenticationBaseUrl = authenticationBaseUrl;
  }

  public ApiDataResponse<IdentityResponse> requestWithShortLivedToken(
      final String token, final String subdomain) {
    final var restGetter =
        new RestGetter<ApiDataResponse<IdentityResponse>>(new ErrorHandler(), restTemplate);
    final var headers = new HttpHeaders();
    headers.set("Accept", IdentityResponse.MEDIA_TYPE);
    headers.set("X-IRIDIUM-AUTH-TOKEN", "Bearer " + token);
    final var url = String.format(IDENTITIES_PATH_FMT_V2, authenticationBaseUrl, subdomain);
    return restGetter.get(
        url, headers, new ParameterizedTypeReference<ApiDataResponse<IdentityResponse>>() {});
  }

  public ApiDataResponse<IdentityResponse> requestWithBearerToken(final String token) {
    final var restGetter =
        new RestGetter<ApiDataResponse<IdentityResponse>>(new ErrorHandler(), restTemplate);
    final var headers = new HttpHeaders();
    headers.set("Accept", IdentityResponse.MEDIA_TYPE);
    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    final var url = String.format(IDENTITIES_PATH_FMT, authenticationBaseUrl);
    return restGetter.get(
        url, headers, new ParameterizedTypeReference<ApiDataResponse<IdentityResponse>>() {});
  }

  public ApiDataResponse<AuthenticationResponse> authenticate(final AuthenticationRequest request) {
    final var restPoster =
        new RestPoster<AuthenticationRequest, ApiDataResponse<AuthenticationResponse>>(
            restTemplate);
    final var headers = new HttpHeaders();
    headers.set("Accept", AuthenticationRequest.MEDIA_TYPE);
    headers.set("Content-Type", AuthenticationResponse.MEDIA_TYPE);
    final var url = String.format(IDENTITIES_PATH_FMT, authenticationBaseUrl);
    return restPoster.post(
        url,
        headers,
        request,
        new ParameterizedTypeReference<ApiDataResponse<AuthenticationResponse>>() {},
        new ErrorHandler());
  }
}
