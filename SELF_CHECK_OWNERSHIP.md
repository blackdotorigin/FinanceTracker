# Self/owner checks: how access to user data is enforced

This guide explains how the application decides whether the authenticated requester
may access a user profile or transaction. The short version is: the application
gets the user's ID from Spring Security's authenticated principal, not from a
transaction request body, and compares that ID with the owner ID stored on the
record (or uses it to scope the database query).

## What is being checked?

Each user has a UUID. A transaction stores the owning user's UUID in its `user_id`
column. The requester is allowed to access an individual transaction only when
the UUID on the authenticated principal equals the UUID stored as that
transaction's owner.

Conceptually, the rule is:

```text
authenticated user's UUID == requested record's owner UUID
```

This is an authorization check. It is separate from authentication:

- **Authentication** establishes who made the request.
- **Ownership authorization** decides whether that authenticated user may
  access this particular record.

## The parts involved

| Part | Responsibility |
| --- | --- |
| `SecurityConfig` | Requires authentication for protected routes and currently configures stateless HTTP Basic authentication. |
| `AppUserDetailsService` | Loads an active account by normalized username while Spring Security authenticates credentials. |
| `AuthUser` | Represents the authenticated principal and carries the user's UUID. |
| `CurrentUser` | Reads and validates the current `AuthUser` from Spring Security's request security context. |
| `OwnedEntity` | Defines the `getOwnerId()` contract for entities that belong to a user. |
| `Transaction.getOwnerId()` | Exposes a transaction's stored `userId` as its owner ID. |
| `AuthorizationService` | Implements the comparisons and denies access when the IDs do not match. |
| `TransactionService` | Applies ownership enforcement to transaction operations. |
| `SelfOnly` | Provides a method-security annotation for user operations that take a user ID. |

## How the authenticated user's ID is obtained

1. `SecurityConfig.chain(...)` marks every route other than user registration
   (`POST /api/v1/users`) and `/actuator/health` as requiring authentication.
   The configuration is stateless and currently uses HTTP Basic; the source
   comment describes a JWT filter as a future replacement, not the current
   implementation.
2. Spring Security authenticates the supplied credentials using the configured
   `UserDetailsService`. `AppUserDetailsService.loadUserByUsername(...)`
   normalizes the username and loads only an active user.
3. `AuthUser(User)` copies that user's UUID, username, and other authentication
   data into the principal Spring Security stores for the request.
4. `CurrentUser.get()` reads the `Authentication` from
   `SecurityContextHolder`, requires it to be authenticated, and requires its
   principal to be an `AuthUser`. If those conditions are not met, it returns
   an empty `Optional`.
5. `CurrentUser.id()` returns the principal's UUID. If no supported authenticated
   principal is available, it throws `AccessDeniedException` rather than
   returning a client-supplied or guessed ID.

The security context is request-scoped by Spring Security's filter chain; the
application does not take the transaction owner from a URL or request body.

## Transaction ownership, operation by operation

### Create: assign the owner on the server

`TransactionService.create(...)` builds a new `Transaction` and calls
`CurrentUser.id()` to set its `userId`. The create request DTO contains the
transaction's business fields (amount, currency, type, date, and description),
but no owner ID. This means a caller cannot create a transaction for another
user merely by including somebody else's ID in the JSON body. The database
column is also marked `updatable = false`.

The transaction is saved with that server-assigned owner. The response DTO does
not expose `userId`.

### Get one: load, then compare the owner

`TransactionService.get(id)` calls the private `findOwned(id)` helper:

1. `repo.findById(id)` loads the row.
2. If no row exists, the service throws `ResourceNotFoundException` with
   `"Transaction not found"`.
3. If a row exists, `auth.assertOwner(transaction)` compares the row's owner ID
   with the current principal's ID.
4. Only after the check succeeds does the service map the entity to a response.

The check happens in the service, not in `TransactionController`. The controller
accepts the route UUID and delegates to the service; it does not make the
authorization decision itself.

### Update: ownership before mutation

`TransactionService.update(id, request)` first calls `findOwned(id)`. Only a
successfully authorized transaction is passed to `apply(...)`, which changes
the business fields from the update DTO. `apply(...)` does not change `userId`.
An update request therefore cannot transfer ownership.

### Delete: ownership before deletion

`TransactionService.delete(id)` passes only the result of `findOwned(id)` to
`repo.delete(...)`. A transaction belonging to another user is not handed to
the delete operation.

### List: constrain the database query itself

`TransactionService.list(from, to, pageable)` calls
`CurrentUser.id()` and passes that ID to
`findByUserIdAndTransactionDateBetween(...)`. The database query returns only
rows whose `user_id` matches the current requester, within the requested date
range and page. This path does not fetch all users' rows and filter them in
application memory.

The controller supplies the date defaults (the current month) and pageable
settings; ownership comes from the service's current-user lookup, not from query
parameters.

## The reusable ownership methods

### `OwnedEntity.getOwnerId()`

`OwnedEntity` is a small interface requiring an entity to expose its owner's
UUID. `Transaction` implements it by returning `userId`. This lets
`AuthorizationService` make a generic owner check without needing transaction-
specific logic.

Any future user-owned entity must implement this interface correctly and its
service must enforce ownership when retrieving or mutating individual records.
Implementing the interface alone does not automatically protect an endpoint.

### `AuthorizationService.isSelf(UUID requestedId)`

This asks `CurrentUser.get()` for the principal and compares
`AuthUser.getId()` with `requestedId`. It returns `false` if there is no current
`AuthUser`; lack of a principal never counts as a match.

### `AuthorizationService.isOwner(Object obj)`

This accepts an object, checks whether it implements `OwnedEntity`, and, if so,
delegates to `isSelf(entity.getOwnerId())`. For any other object it returns
`false` (deny by default). It is a boolean check; it does not itself throw an
exception.

### `AuthorizationService.assertOwner(OwnedEntity entity)`

This performs the same ID comparison as a guard. If the current user is not the
owner, it throws Spring Security's `AccessDeniedException("Access denied")`.
`findOwned(...)` uses this guard for get, update, and delete.

## The separate user-profile check

The same comparison is also used for user profile operations:

- `SelfOnly` is meta-annotated with `@PreAuthorize("@auth.isSelf(#id)")`.
- `@EnableMethodSecurity` in `SecurityConfig` enables the method-level
  authorization expression.
- `UserService.get(id)`, `update(id, ...)`, and `delete(id)` are annotated with
  `@SelfOnly`.
- Before those service methods run, Spring evaluates `auth.isSelf(#id)`. If the
  requested user ID does not match the authenticated principal's ID, access is
  denied.
- `UserController`'s `/me` endpoint instead calls `CurrentUser.id()` and passes
  that ID to the user service.

This profile check is related to, but distinct from, transaction ownership:
`@SelfOnly` compares a requested *user ID* with the principal; transaction
operations compare the *owner ID on a transaction* with the principal.

## Request flow at a glance

```text
HTTP request
  -> Spring Security authenticates credentials
  -> AuthUser (contains user UUID) is placed in the security context
  -> Controller delegates to the service
  -> Service obtains current UUID through CurrentUser
       -> list: query database by user_id = current UUID
       -> get/update/delete: load row, compare its owner UUID, then proceed
       -> create: set user_id to current UUID, then save
  -> authorized result is mapped to a response
```

For an individual transaction lookup, a nonexistent ID is handled before the
owner comparison because the service cannot compare an owner until it has
loaded a row.

## What a caller can and cannot control

- A caller must authenticate for protected API routes.
- A caller controls the transaction ID in get/update/delete URLs, but that ID
  does not grant access by itself.
- A caller controls transaction business fields in create/update DTOs.
- A caller does not supply the owner UUID in those DTOs; create assigns it from
  the authenticated principal, and update does not overwrite it.
- A caller may supply date/page filters to list transactions, but cannot supply
  the owner ID used by the repository query.

## Important implementation boundaries

1. **Ownership is enforced in services, not by the controller.** New controller
   routes must call a service method that applies the same rule; adding a route
   that reads or mutates a repository directly would bypass this protection.
2. **A database lookup by ID is followed by an owner check.** This is the
   current get/update/delete pattern. A missing record throws
   `ResourceNotFoundException`; an existing foreign record causes
   `AccessDeniedException`.
3. **Exception types are explicit, but the HTTP mapping is not customized here.**
   The source has no application-specific exception handler for these
   exceptions. The service's comment describes the owner failure as 403, but
   the exact HTTP response depends on Spring MVC/Security exception handling in
   the running configuration. Do not infer a uniform response body or status
   from the service code alone.
4. **`isOwner` is not automatically applied.** The transaction service calls
   `assertOwner` directly; `isOwner` is another reusable method, not an
   annotation or a global filter.
5. **Owner IDs must remain trustworthy.** New write paths must not accept an
   arbitrary owner ID from a client, and all new reads/writes for owned records
   need an ownership guard or owner-scoped repository query.

## Source map

- Authentication and route rules: [`SecurityConfig`](./src/main/java/com/BlackDot/Finance/Tracker/Auth/SecurityConfig.java)
- User lookup for authentication: [`AppUserDetailsService`](./src/main/java/com/BlackDot/Finance/Tracker/Auth/AppUserDetailsService.java)
- Principal model: [`AuthUser`](./src/main/java/com/BlackDot/Finance/Tracker/Auth/AuthUser.java)
- Current requester lookup: [`CurrentUser`](./src/main/java/com/BlackDot/Finance/Tracker/Auth/CurrentUser.java)
- Shared ownership contract and comparisons: [`OwnedEntity`](./src/main/java/com/BlackDot/Finance/Tracker/Auth/OwnedEntity.java), [`AuthorizationService`](./src/main/java/com/BlackDot/Finance/Tracker/Auth/AuthorizationService.java)
- Transaction owner field: [`Transaction`](./src/main/java/com/BlackDot/Finance/Tracker/Transactions/Transaction.java)
- Transaction repository's owner-scoped list query: [`TransactionRepository`](./src/main/java/com/BlackDot/Finance/Tracker/Transactions/TransactionRepository.java)
- Transaction operation flow: [`TransactionService`](./src/main/java/com/BlackDot/Finance/Tracker/Transactions/TransactionService.java)
- Controller delegation: [`TransactionController`](./src/main/java/com/BlackDot/Finance/Tracker/Transactions/TransactionController.java)
- Profile-level check: [`SelfOnly`](./src/main/java/com/BlackDot/Finance/Tracker/Auth/SelfOnly.java), [`UserService`](./src/main/java/com/BlackDot/Finance/Tracker/Users/UserService.java)
