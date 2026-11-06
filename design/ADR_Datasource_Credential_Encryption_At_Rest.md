# ADR: Datasource Credential Encryption at Rest

**Status:** Proposed  
**Date:** 2026-09-21  
**Authors:** Bhakta, Saad

---

## 1. Context

Datasource authentication credentials such as passwords, tokens, API keys, and client secrets are currently stored as part of the datasource authentication configuration in the `kruize_authentication` database table.

These credentials are sensitive information and must not be stored in plaintext. If an unauthorized user gains access to the database, database backups, or other persisted database artifacts, plaintext credentials could be exposed.

The security concern was identified during security testing of the datasource API. Although the ROS team is currently the primary consumer of Kruize and does not use the datasource credentials API in its production workflow, the datasource API and its persisted credentials remain part of the Kruize product and need to be protected.

The encryption solution should therefore protect sensitive credentials at rest while minimizing impact to existing datasource configurations and deployments.

---

## 2. Problem Statement

The datasource authentication configuration contains both sensitive and non-sensitive information.

The sensitive information varies depending on the authentication type. For example, one authentication type may contain a password, while another may contain a token, API key, client secret, or another authentication-specific credential.

The system therefore needs to:

- Protect sensitive authentication credentials when persisted in the database.
- Preserve non-sensitive datasource configuration in its existing representation.
- Avoid unnecessary database schema changes.
- Ensure that all supported authentication types have their sensitive fields protected.
- Provide a clear mechanism for managing the encryption key.
- Define how encryption-key changes are handled.
- Prevent an accidental configuration change from causing encrypted credentials to be silently treated as plaintext.

---

## 3. Goals

The solution should:

1. Encrypt sensitive datasource credentials at rest.
2. Keep the existing datasource JSON/JSONB structure where possible.
3. Encrypt only fields that contain sensitive credentials.
4. Support different sensitive fields for different authentication types.
5. Make encryption transparent to the rest of the application.
6. Avoid requiring changes to deployments that do not use credential-backed datasources.
7. Provide a defined encryption-key management mechanism.
8. Define the behavior for encryption-key rotation.
9. Provide a safe migration path for existing plaintext credentials.

The implementation mechanism can be decided separately after this architectural decision is approved.

---

## 5. Sensitive Fields

The fields requiring encryption depend on the authentication type.

The implementation will maintain a mapping between the supported authentication type and its sensitive fields.

Conceptually:

| Authentication Type                  | Sensitive Fields                      |
|--------------------------------------|---------------------------------------|
| Basic Authentication                 | Password / authentication secret      |
| Token Authentication                 | Token                                 |
| API Key Authentication               | API key                               |
| Client Credential Authentication     | Client secret                         |
| Other supported authentication types | Authentication-specific secret fields |

The exact field names are determined by the datasource authentication schema.

This mapping is part of the encryption design because only sensitive fields need to be encrypted.

When a new authentication type is introduced, its sensitive fields must be explicitly identified and added to the encryption mapping before the authentication type is considered fully supported.

---

## 6. Proposed Solution

Sensitive datasource authentication fields will be **encrypted at rest before being persisted to the database**.

Only the sensitive values will be encrypted. Non-sensitive datasource configuration will remain in its existing JSON/JSONB representation.

For example:

### Before encryption

```json
{
  "type": "basic",
  "url": "https://example.com",
  "username": "user",
  "password": "secret"
}
```

### Persisted representation

```json
{
  "type": "basic",
  "url": "https://example.com",
  "username": "user",
  "password": "<encrypted-value>"
}
```

The application will transparently decrypt the sensitive fields when they are required internally.

The plaintext credential must never be exposed through the datasource API response.

---

## 7. Approaches Considered

### 7.1 Approach 1 — Encrypt the Entire Authentication JSON

Under this approach, the complete authentication configuration is serialized and encrypted before being stored in the database.

Conceptually:

```text
Authentication JSON
        |
        v
Serialize
        |
        v
Encrypt entire JSON
        |
        v
Persist encrypted payload
```

#### Advantages

- Simple encryption boundary.
- No need to identify individual sensitive fields.
- New authentication types do not require additional sensitive-field mappings.
- Any future secret introduced into the authentication configuration is automatically protected.

#### Disadvantages

- The existing JSON/JSONB representation cannot be retained in its current form once the entire payload is encrypted.
- The database column would need to change to an encrypted representation such as `TEXT` or another binary/string representation.
- Existing JSON querying capability on the column would be lost.
- Database migration is required.
- Existing deployments need to handle the schema change.
- Non-sensitive fields are also encrypted even though they do not require confidentiality.

#### Database Impact

The current JSON/JSONB column would need to be changed to accommodate the encrypted payload.

This introduces a DDL/migration requirement. The approach therefore has greater database and upgrade impact than field-level encryption.

#### Impact on ROS

**Moderate to Significant impact.**

The ROS team currently does not use the datasource credentials API in its production workflow, so there is no functional requirement for ROS to enable credential encryption.

However, this approach requires changing the database representation of the authentication credentials from JSON/JSONB to an encrypted representation such as `TEXT`. This introduces a database schema migration that would also affect ROS deployments using the affected database schema.

ROS deployments would therefore need to:

- Apply the database migration when upgrading to the new version.
- Ensure that existing authentication records are migrated correctly.
- Ensure that the encryption key is configured if encrypted credential storage is enabled.
- Account for the loss of native JSON/JSONB querying on the encrypted column.

For ROS deployments that do not use the datasource credentials API, this provides limited functional benefit while still introducing database migration and operational considerations.

---

### 7.2 Approach 2 — Encrypt Sensitive Fields Only

Under this approach, only the sensitive fields identified for each authentication type are encrypted.

Conceptually:

```text
Authentication JSON
        |
        v
Identify authentication type
        |
        v
Identify sensitive fields
        |
        v
Encrypt sensitive values
        |
        v
Persist JSON/JSONB
```

For example:

```json
{
  "type": "basic",
  "url": "https://example.com",
  "username": "user",
  "password": "<encrypted-value>"
}
```

#### Advantages

- No fundamental database schema change is required.
- Existing JSON/JSONB representation can be retained.
- Non-sensitive fields remain available in their existing representation.
- Only confidential values are encrypted.
- Existing datasource database structure remains compatible.
- Migration impact is lower than whole-payload encryption.
- Encryption can be extended to new authentication types by adding their sensitive-field definitions.

#### Disadvantages

- The application must maintain a mapping between authentication types and their sensitive fields.
- When a new authentication type introduces a sensitive field, the mapping must be updated.
- Failure to correctly identify a new sensitive field could result in that field being persisted without encryption.
- The mapping must therefore be treated as part of the security-sensitive configuration of the datasource authentication model.

#### Impact on ROS

**Minimal impact.**

The ROS team currently does not use the datasource credentials API in its production workflow. Therefore, there is no expected functional impact to existing ROS deployments.

Because only sensitive fields within the existing authentication JSON are encrypted, the existing database JSON/JSONB representation can be retained. No database column-type migration is required.

ROS deployments therefore do not need to:

- Change the existing database schema.
- Modify existing datasource configurations.
- Change their datasource API consumers.

If ROS later uses credential-backed datasources, the encryption key can be supplied through the existing Kruize ConfigMap configuration mechanism to enable encrypted credential storage.

If key rotation is enabled, ROS deployments using encrypted credentials will need to ensure that the configured encryption key/version is maintained consistently across upgrades.

#### Assessment

This approach provides the required protection while preserving the existing datasource JSON/JSONB representation and minimizing database migration impact.

---

### 7.3 Approach 3 — External Secrets Manager / Vault

Under this approach, credentials are not stored directly in the database. An external secret-management system such as HashiCorp Vault, AWS Secrets Manager, or an equivalent platform service stores the credentials. The database contains only a reference or alias.

#### Advantages

- Credentials are not stored in the application database.
- External secret managers can provide centralized access control and auditing.
- Many secret-management systems provide built-in rotation capabilities.
- Provides a path toward centralized enterprise secret management.

#### Disadvantages

- Introduces a new infrastructure dependency.
- Requires changes to the datasource credential flow.
- Adds operational complexity.
- Requires additional deployment and access-control configuration.
- May require changes to existing consumers and deployment environments.

#### Impact on ROS

**Significant impact.**

The ROS team would need to introduce and operate an external secret-management solution such as Vault, a cloud secrets manager, or an equivalent platform service.

This would introduce:

- Additional infrastructure dependencies.
- Changes to the ROS deployment configuration.
- Additional access-control/IAM configuration.
- Changes to the way datasource credentials are provisioned and retrieved.
- Additional operational and maintenance responsibilities.

Since ROS currently does not use the datasource credentials API in its production workflow, introducing an external secret-management dependency would add significant complexity without a corresponding immediate functional requirement.

#### Assessment

This is a potential long-term option but is outside the current scope because it introduces infrastructure and operational complexity that is not required for the current problem.

## 8. Approach Comparison

| Consideration                      | Encrypt Entire JSON    | Encrypt Sensitive Fields    | External Secrets / Vault       |
|------------------------------------|------------------------|-----------------------------|--------------------------------|
| Credentials protected at rest      | Yes                    | Yes                         | Yes                            |
| Existing JSON/JSONB representation | No                     | Yes                         | No / reference-based           |
| Database schema change             | Required / likely      | No                          | Likely                         |
| Sensitive-field mapping            | Not required           | Required                    | Not required                   |
| New authentication type            | Automatically covered  | Mapping must be updated     | Automatically covered          |
| Key rotation                       | Requires re-encryption | Can use eager re-encryption | Built-in depending on provider |
| Infrastructure dependency          | Low                    | Low                         | High                           |
| ROS impact                         | Moderate–Significant   | **Minimal**                 | Significant                    |
| Current scope fit                  | Moderate               | **High**                    | Low                            |

---

## Explore More

The following approaches were considered during the initial exploration but are not included as primary alternatives in this ADR. They introduce additional infrastructure or implementation complexity beyond the current requirements and are therefore kept as future options rather than primary decision paths.

### KMS / Envelope Encryption

A Key Management Service can manage a master key while data encryption keys are used to encrypt the credentials.

This provides strong enterprise key-management and rotation capabilities, but introduces KMS infrastructure, IAM configuration, and potentially cloud-provider-specific dependencies.

This approach can be revisited if future requirements introduce centralized enterprise key management or external KMS integration.

### PostgreSQL `pgcrypto`

PostgreSQL's `pgcrypto` extension can provide database-level encryption.

This would reduce some application-level encryption logic, but couples the solution to PostgreSQL and introduces database-specific key-management considerations.

This approach can be revisited if database-level encryption becomes a specific requirement.

## 9. Decision

**Approach 2 — Encrypt Sensitive Fields Only** is selected.

The solution will:

1. Retain the existing datasource authentication JSON/JSONB representation.
2. Identify sensitive fields based on authentication type.
3. Encrypt those fields before persistence.
4. Decrypt those fields transparently when required internally.
5. Ensure credentials are not returned through datasource API responses.
6. Require the sensitive-field mapping to be updated whenever a new authentication type introduces a sensitive credential field.

The architectural decision is intentionally independent of the specific persistence framework.

The implementation may use JPA, Hibernate converters, annotations, serialization hooks, or another appropriate mechanism, but these are implementation details and are not part of this architectural decision.

---

## 10. Encryption Key Management

Kruize currently uses Kubernetes **ConfigMap** for application configuration, including existing datastore-related configuration. The encryption key will follow the same configuration mechanism for consistency with the current Kruize deployment model.

The encryption key will be provided through the Kruize ConfigMap and loaded by the application at startup.

Conceptually:

```text
Kruize ConfigMap
      |
      | encryption key
      v
Kruize Application
      |
      v
Credential Encryption
      |
      v
Encrypted sensitive fields
      |
      v
Database
```

The encryption key must not be persisted in the `kruize_authentication` database table alongside encrypted credentials.

The exact ConfigMap property name and representation will be defined as part of the implementation.

---

## 11. Encryption Key Identification

The system should maintain a key identifier/version rather than storing the actual encryption key in the database.

For example:

```text
ConfigMap:
    encryption-key-version = v2
    encryption-key = <key>

Database:
    credential key version = v1
```

The key identifier allows the application to determine which key was used to encrypt an existing credential without exposing the actual encryption key.

This also provides the basis for key rotation.

---

## 12. Encryption Key Rotation

Key rotation must be explicitly defined.

When the configured encryption key changes, the application must detect that credentials were encrypted using a previous key version.

The preferred strategy is **eager re-encryption**.

### Rotation flow

```text
Application startup
        |
        v
Read configured encryption key/version
        |
        v
Determine stored credential key version
        |
        v
Compare versions
        |
   +----+----+
   |         |
  Same    Different
   |         |
   v         v
Continue   Re-encrypt
             |
             v
       Decrypt with old key
             |
             v
       Encrypt with new key
             |
             v
       Persist new value/version
```

The system should perform re-encryption when the key change is detected rather than waiting for individual datasource records to be accessed.

This ensures that credentials are migrated to the new key proactively.

---

## 13. Key Rotation Failure Handling

The application must not silently continue if required credential re-encryption fails.

If re-encryption fails, the application should:

- Clearly report the failure.
- Prevent partially migrated credentials from being treated as successfully rotated.
- Avoid deleting or overwriting the only valid encrypted representation.
- Provide sufficient logging and diagnostics for the operator to resolve the issue.
- Retry the operation safely when appropriate.

The exact transactional/batching strategy will be defined during implementation.

---

## 14. Backward Compatibility

The solution should minimize impact to deployments that do not use credential-backed datasources.

In particular:

- Existing datasource configurations should remain structurally compatible.
- No database column type change should be required as part of this approach.
- Deployments that do not use sensitive datasource credentials should not require unnecessary infrastructure changes.
- Existing API consumers should not need to understand the encrypted representation.

The encryption/decryption should remain internal to datasource credential handling.

---

## 15. API Security (When the datasource creation API is added in future)

Encryption at rest does not replace API-level credential protection.

The datasource API must continue to ensure that sensitive credentials are not returned in API responses.

For example:

```text
Database
   |
   v
Encrypted credential
   |
   v
Application decrypts internally
   |
   v
Datasource API
   |
   v
Sensitive fields excluded/masked
```

The plaintext credential should only exist in application memory when it is required for an authorized internal operation.

---

## 16. Consequences

### Positive

- Sensitive datasource credentials are protected at rest.
- Existing JSON/JSONB database representation is preserved.
- No fundamental database DDL change is required.
- Non-sensitive datasource configuration remains unchanged.
- The solution supports different authentication types.
- Key rotation can be supported without changing the database representation.
- The design is independent of JPA/Hibernate implementation details.
- Existing ROS deployments that do not use credential-backed datasources have minimal impact.

### Negative

- Sensitive-field mappings must be maintained.
- Adding a new authentication type requires explicitly identifying its sensitive fields.
- Incorrect or incomplete sensitive-field mapping could result in credential leakage.
- Encryption introduces cryptographic processing overhead.
- Encryption-key management becomes an operational responsibility.
- Key rotation requires re-encryption of existing credentials.
- Losing the encryption key makes encrypted credentials unrecoverable.

---

## 17. Risks and Mitigations

### Risk: New authentication type introduces an unencrypted secret

**Mitigation:**  
Every new authentication type must define its sensitive fields as part of its implementation. Tests should verify that all sensitive fields are encrypted before persistence.

### Risk: Encryption key is accidentally removed

**Mitigation:**  
If encrypted credentials are present but the required encryption key is unavailable, the application must fail safely rather than interpreting encrypted values as plaintext.

### Risk: Encrypted value is tampered with

**Mitigation:**  
Use authenticated encryption so that modification of ciphertext is detected during decryption.

### Risk: Encryption key is lost

**Mitigation:**  
The deployment/operator is responsible for securely managing and backing up the encryption key. Loss of the key results in the inability to decrypt existing credentials.

### Risk: Partial key rotation

**Mitigation:**  
Re-encryption should be performed using a controlled, retryable process that does not destroy the existing valid encrypted representation until the new representation has been successfully created.

---

## 18. Final Decision Summary

The decision was made after considering three primary approaches:

1. Encrypt the entire authentication JSON.
2. Encrypt sensitive fields only.
3. Store credentials using an external secrets manager.

The selected architecture is:

```text
              Datasource Authentication
                         |
                         v
                Identify Auth Type
                         |
                         v
                Identify Sensitive
                     Fields
                         |
                         v
              Encrypt Sensitive Values
                         |
                         v
                  JSON / JSONB
                         |
                         v
                    Database
```

Encryption keys are supplied through the existing Kruize ConfigMap configuration mechanism and are not stored in the database.

Key versions are tracked so that a change in encryption key can be detected.

When key rotation is required, existing credentials are **eagerly re-encrypted** using the new key rather than waiting for individual records to be accessed.

The ADR deliberately leaves JPA/Hibernate and other framework-specific mechanisms to the implementation design.
