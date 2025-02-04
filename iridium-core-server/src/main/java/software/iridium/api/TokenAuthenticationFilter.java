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
package software.iridium.api;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import software.iridium.api.base.error.NotAuthorizedException;
import software.iridium.api.repository.AccessTokenEntityRepository;
import software.iridium.api.user.PrincipalUser;
import software.iridium.entity.AccessTokenEntity;

public class TokenAuthenticationFilter extends AbstractPreAuthenticatedProcessingFilter {

  private AccessTokenEntityRepository tokenEntityRepository;

  public static final String BEARER_PREFIX_WITH_SPACE = "Bearer ";

  public TokenAuthenticationFilter(
      final AuthenticationManager authenticationManager,
      final AccessTokenEntityRepository accessTokenEntityRepository) {
    super.setAuthenticationManager(authenticationManager);
    this.tokenEntityRepository = accessTokenEntityRepository;
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(final HttpServletRequest httpServletRequest) {

    String token = extractBearerToken(httpServletRequest);

    if (StringUtils.isNotBlank(token)) {
      final AccessTokenEntity entity =
          tokenEntityRepository
              .findFirstByAccessTokenAndExpirationAfter(token, new Date())
              .orElseThrow(NotAuthorizedException::new);

      return new PrincipalUser(entity.getAccessToken(), entity.getIdentityId(), List.of());
    }
    return null;
  }

  private String extractBearerToken(final HttpServletRequest request) {
    final var header = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (header != null && !header.isBlank()) {
      return header.substring(BEARER_PREFIX_WITH_SPACE.length());
    }
    return null;
  }

  @Override
  protected Object getPreAuthenticatedCredentials(final HttpServletRequest request) {
    return "";
  }
}
