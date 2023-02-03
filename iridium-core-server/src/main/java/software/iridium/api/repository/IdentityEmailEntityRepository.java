/*
 *  Copyright (C) Josh Fischer - All Rights Reserved
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *  Written by Josh Fischer <josh@joshfischer.io>, 2023.
 */
package software.iridium.api.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import software.iridium.api.entity.IdentityEmailEntity;

public interface IdentityEmailEntityRepository extends JpaRepository<IdentityEmailEntity, String> {

  Optional<IdentityEmailEntity> findByEmailAddress(final String emailAddress);

  Optional<IdentityEmailEntity> findByEmailAddressAndIdentity_ParentTenantId(
      final String emailAddress, final String parentTenantId);
}
