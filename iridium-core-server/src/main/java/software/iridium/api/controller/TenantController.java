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
package software.iridium.api.controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import software.iridium.api.authentication.domain.CreateTenantRequest;
import software.iridium.api.authentication.domain.CreateTenantResponse;
import software.iridium.api.authentication.domain.TenantSummary;
import software.iridium.api.base.domain.ApiDataResponse;
import software.iridium.api.base.domain.ApiListResponse;
import software.iridium.api.service.TenantService;

@CrossOrigin
@RestController
public class TenantController {

  @Resource private TenantService tenantService;

  @GetMapping(value = "tenants", produces = TenantSummary.MEDIA_TYPE_LIST)
  public ApiListResponse<TenantSummary> getTenantSummaries(HttpServletRequest request) {
    return new ApiListResponse<>(tenantService.getTenantSummaries(request));
  }

  @PostMapping(
      value = "tenants",
      consumes = CreateTenantRequest.MEDIA_TYPE,
      produces = CreateTenantResponse.MEDIA_TYPE)
  public ApiDataResponse<CreateTenantResponse> create(
      final HttpServletRequest request, @RequestBody final CreateTenantRequest tenantRequest) {
    return new ApiDataResponse<>(tenantService.create(request, tenantRequest));
  }
}