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
package software.iridium.api.service;

import static com.google.common.base.Preconditions.checkArgument;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.Map;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.iridium.api.authentication.domain.AuthenticationRequest;
import software.iridium.api.authentication.domain.CreateIdentityRequest;
import software.iridium.api.authentication.domain.IdentityResponse;
import software.iridium.api.base.error.DuplicateResourceException;
import software.iridium.api.base.error.NotAuthorizedException;
import software.iridium.api.base.error.ResourceNotFoundException;
import software.iridium.api.handler.NewIdentityEventHandler;
import software.iridium.api.instantiator.AuthenticationRequestInstantiator;
import software.iridium.api.instantiator.IdentityCreateRequestDetailsInstantiator;
import software.iridium.api.instantiator.IdentityEntityInstantiator;
import software.iridium.api.mapper.IdentityEntityMapper;
import software.iridium.api.mapper.IdentityResponseMapper;
import software.iridium.api.repository.*;
import software.iridium.api.util.AttributeValidator;
import software.iridium.api.util.ServletTokenExtractor;

@Service
public class IdentityService {

  @Autowired private AuthenticationEntityRepository authenticationRepository;
  @Autowired private IdentityEntityMapper identityEntityMapper;
  @Autowired private IdentityEntityInstantiator identityInstantiator;
  @Autowired private IdentityEntityRepository identityRepository;
  @Autowired private IdentityResponseMapper responseMapper;
  @Autowired private BCryptPasswordEncoder encoder;
  @Autowired private NewIdentityEventHandler eventHandler;
  @Autowired private IdentityEmailEntityRepository emailRepository;
  @Autowired private ServletTokenExtractor tokenExtractor;
  @Autowired private TenantEntityRepository tenantRepository;
  @Autowired private ApplicationEntityRepository applicationRepository;
  @Autowired private AttributeValidator attributeValidator;
  @Autowired private IdentityCreateRequestDetailsInstantiator requestDetailsInstantiator;
  @Autowired private AuthenticationService authenticationService;
  @Autowired private AuthenticationRequestInstantiator authenticationRequestInstantiator;
  @Autowired private AccessTokenEntityRepository accessTokenRepository;

  private static final Logger logger = LoggerFactory.getLogger(IdentityService.class);

  @Transactional(propagation = Propagation.REQUIRED)
  public IdentityResponse getIdentity(final HttpServletRequest request) {
    logger.info("retrieving identity " + request.getServerName());

    final var now = Calendar.getInstance().getTime();
    final var token = tokenExtractor.extractBearerToken(request);
    final var accessToken =
        accessTokenRepository
            .findFirstByAccessTokenAndExpirationAfter(token, now)
            .orElseThrow(NotAuthorizedException::new);

    final var identity =
        identityRepository
            .findById(accessToken.getIdentityId())
            .orElseThrow(NotAuthorizedException::new);

    return identityEntityMapper.map(identity);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public IdentityResponse create(
      final CreateIdentityRequest request, final Map<String, String> requestParams) {
    final var emailAddress = request.getUsername();
    checkArgument(
        EmailValidator.getInstance().isValid(emailAddress),
        String.format("email must not be blank and properly formatted: %s", request.getUsername()));
    checkArgument(
        attributeValidator.isNotBlank(request.getClientId()),
        String.format("clientId must not be blank: %s", request.getClientId()));
    // todo add password requirements
    final var application =
        applicationRepository
            .findByClientId(request.getClientId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "application not found for clientId: " + request.getClientId()));

    if (tenantRepository.findById(application.getTenantId()).isEmpty()) {
      throw new ResourceNotFoundException(
          String.format("tenant not found for id: %s", application.getTenantId()));
    }

    if (emailRepository
        .findByEmailAddressAndIdentity_ParentTenantId(emailAddress, application.getTenantId())
        .isPresent()) {
      throw new DuplicateResourceException(
          String.format(
              "Account already registered with: %s in tenant: %s",
              emailAddress, application.getTenantId()));
    }

    final var identity =
        identityRepository.save(
            identityInstantiator.instantiate(
                request, encoder.encode(request.getPassword()), application.getTenantId()));
    final var sessionDetails = requestDetailsInstantiator.instantiate(requestParams, identity);
    identity.setCreateSessionDetails(sessionDetails);

    eventHandler.handleEvent(identity, application.getClientId());
    final AuthenticationRequest authenticationRequest =
        authenticationRequestInstantiator.instantiate(request);
    final var authenticationResponse =
        authenticationService.authenticate(authenticationRequest, requestParams);
    return responseMapper.map(identity, authenticationResponse);
  }
}
