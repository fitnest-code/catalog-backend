# Pre-purchase Home Gym — Mobile Integration Guide (iOS / Android)

This document describes the **read-only** home-gym APIs used **before** purchase (browse / search / gym detail). They tell the app which gyms can become the user’s **əsas zal (home gym)** for each package tier, without changing selection or `PUT /subscriptions/{id}/home-gym`.

Related:

- Post-purchase selection & check-in: [PRIMARY_GYM_V4.md](./PRIMARY_GYM_V4.md)
- Product rules source: *Qayda və hesablanma* (screens 10.x)

All paths below go through the **API gateway**. Prefer the gateway base URL in every environment.

---

## 1. Eligibility rule (must match UI)

```
eligible(gym, package) =
    gym.homeGymEnabled                          // admin master switch
    AND gym offers a package in that hierarchy  // “zal paketə daxildir”
    AND package.tier >= gym.homeMinTier
```

**Package-and-above:** if a gym can be home for SILVER, it can also be home for GOLD and PLATINUM. The backend therefore exposes one value per gym:

| Field | Meaning |
|-------|---------|
| `homeGym.minTier` | Lowest package that can select this gym as home (`BRONZE` / `SILVER` / `GOLD` / `PLATINUM`), or `null` if none / admin off |
| `homeGym.tiers` | All public tiers from `minTier` upward (empty when `minTier` is null) |
| `homeGym.eligibleForRequestedTier` | Present **only** when the client sent `packageId` or `tier`; `true`/`false` for that request |

**Admin master switch off:** `minTier = null`, `tiers = []`, gym disappears from `homeEligible=true` lists and from `HOME_ELIGIBLE` counts.

**Access hours do not affect `minTier`.** Hours matter only after purchase / at check-in.

**Consistency:** every gym shown as home-eligible before purchase for a given package must appear after purchase in:

`GET /api/v1/subscriptions/{subscriptionId}/home-gym/gyms`

(same rule engine).

---

## 2. Endpoint map (what to call for which screen)

| Screen (mock 10.x) | Method | Path | Auth | Purpose |
|--------------------|--------|------|------|---------|
| Gym list / map / category results (10.1–10.3, 10.7–10.8) | `GET` | `/api/v3/gyms` | Bearer (required by catalog) | List items with `homeGym` + optional home filter + `meta.homeEligibleCount` |
| Gym detail packages / hero chips (10.4–10.5) | `GET` | `/api/v4/gyms/{gymId}` | Optional / as today | Detail with `homeGym` + per-package `homeEligible` |
| Search landing categories (10.6) | `GET` | `/api/v2/gyms/count/by-category` | Public | First row `HOME_ELIGIBLE` + real `gymCount`, then normal categories |
| After purchase — selectable homes | `GET` | `/api/v1/subscriptions/{id}/home-gym/gyms` | Bearer | Same eligibility rule for selection UI |

**Do not** expect `homeGym` on older `GET /api/v2/gyms` or `GET /api/v2/gyms/{id}`. Migrate list/detail consumers to **v3 list** and **v4 detail**.

---

## 3. `GET /api/v3/gyms` — gym list with homeGym

### Query parameters

| Param | Type | Required | Notes |
|-------|------|----------|-------|
| `q` | string | no | Search text (name / address), combines with home filter |
| `type` | string | no | `ALL` (default), `NEW`, `CLOSEST`, `SAVED` |
| `category` | long | no | Category id (main or sub) |
| `subscriptionId` | long | no | Existing “gyms in package” filter (catalog package id) |
| `page` | int | no | 1-based, default `1` |
| `page_size` | int | no | Default `10`, max `100` |
| `lat` / `lng` | double | no | For distance / closest sort |
| `sort_dir` | `ASC`/`DESC` | no | Default `DESC` |
| `homeEligible` | bool | no | Default `false`. When `true`, only gyms that can be home (for optional package/tier) |
| `packageId` | long | no | Package id for eligibility + `eligibleForRequestedTier` |
| `tier` | string | no | `BRONZE` / `SILVER` / `GOLD` / `PLATINUM` if you don’t have `packageId` |

**Recommended client behaviour**

- Package picker / “Gold plan” browse: pass `tier=GOLD` (or that plan’s `packageId`).
- User already subscribed: pass **their package** `tier` / `packageId` so chips and filters match post-purchase list (no extra BE change).
- “Only homes” chip (10.7): `homeEligible=true` without tier → all gyms with `minTier != null`.
- “Homes for Gold” (10.8): `homeEligible=true&tier=GOLD`.

### Response shape

```json
{
  "items": [
    {
      "gymId": 101,
      "name": "FitZone",
      "coverImageUrl": "...",
      "stars": 4.5,
      "isNew": false,
      "location": "...",
      "city": "Baku",
      "distanceKm": 1.2,
      "minPackageTier": "BRONZE",
      "homeGym": {
        "minTier": "SILVER",
        "tiers": ["SILVER", "GOLD", "PLATINUM"],
        "eligibleForRequestedTier": true
      }
    },
    {
      "gymId": 102,
      "name": "Pulse Gym",
      "homeGym": {
        "minTier": null,
        "tiers": []
      }
    }
  ],
  "meta": {
    "total": 38,
    "homeEligibleCount": 21,
    "page": 1,
    "pageSize": 10
  },
  "message": null
}
```

Notes:

- `eligibleForRequestedTier` is **omitted** when neither `packageId` nor `tier` was sent.
- `meta.homeEligibleCount` is the count of home-capable gyms under the **same** q/category/subscription/tier filters (not only the current page). Use it for “21 əsas zal” style counters (10.1 / 10.3 / 10.8).
- `meta.total` is the size of the **filtered** list (after `homeEligible` if applied).

### Example calls

```http
GET /api/v3/gyms?page=1&page_size=20
Authorization: Bearer <token>

GET /api/v3/gyms?homeEligible=true&page=1&page_size=20
Authorization: Bearer <token>

GET /api/v3/gyms?homeEligible=true&tier=GOLD&category=5&q=fit&lat=40.4&lng=49.8&type=CLOSEST
Authorization: Bearer <token>
```

---

## 4. `GET /api/v4/gyms/{gymId}` — gym detail with per-package flags

Use this for detail screens that show package cards and home badges (10.4–10.5).

### Response (home-gym related fields)

```json
{
  "gymId": 101,
  "name": "FitZone",
  "homeGym": {
    "minTier": "SILVER",
    "tiers": ["SILVER", "GOLD", "PLATINUM"]
  },
  "packages": [
    { "tier": "BRONZE", "homeEligible": false },
    { "tier": "SILVER", "homeEligible": true },
    { "tier": "GOLD", "homeEligible": true },
    { "tier": "PLATINUM", "homeEligible": true }
  ]
}
```

(Other detail fields from the V4 payload remain as implemented; focus mapping on `homeGym` + `packages[].homeEligible`.)

**UI mapping**

- Hero / chip “from Silver”: use `homeGym.minTier`.
- Package row badge: use `packages[].homeEligible` for that tier (do not recompute on client except for offline display of the same rule).
- If `homeGym.minTier == null`: hide home CTA / show “not available as home”.

```http
GET /api/v4/gyms/101
```

---

## 5. `GET /api/v2/gyms/count/by-category` — search categories (10.6)

Returns categories with **`HOME_ELIGIBLE` first**, then normal category rows.

```json
{
  "categories": [
    {
      "type": "HOME_ELIGIBLE",
      "titleKey": "search.category.homeEligible",
      "subtitleKey": "search.category.homeEligible.sub",
      "gymCount": 64
    },
    {
      "type": "CATEGORY",
      "id": "12",
      "name": "Fitness",
      "iconUrl": "...",
      "gymCount": 100
    }
  ]
}
```

### Localization

Backend stores keys + message bundles. Resolve for the user’s language:

| Key | AZ | EN | RU |
|-----|----|----|----|
| `search.category.homeEligible` | Əsas zal seçilə bilənlər | Eligible for home gym | Подходит для домашнего зала |
| `search.category.homeEligible.sub` | Ayda 30 girişə qədər | Up to 30 visits per month | До 30 посещений в месяц |

If the client already loads `messages` / remote config, prefer those strings. Otherwise hardcode the table above for AZ/EN/RU.

**Tap behaviour for `HOME_ELIGIBLE`:** navigate to gym list with `homeEligible=true`. If the user has an active subscription, also pass their package `tier` / `packageId`.

```http
GET /api/v2/gyms/count/by-category
```

---

## 6. Post-purchase alignment

| Phase | Endpoint | Client tip |
|-------|----------|------------|
| Browse before buy | `GET /api/v3/gyms?tier=…` | Show same gyms the user will later select |
| After buy | `GET /api/v1/subscriptions/{id}/home-gym/gyms` | Selection / change home |
| Change home | `PUT` (existing home-gym selection API — see PRIMARY_GYM_V4 / order docs) | Unchanged by this feature |

Do **not** invent a second eligibility algorithm on mobile. Trust `homeGym` / `homeEligible` flags.

---

## 7. Client checklist

1. Switch list screens to **`/api/v3/gyms`**; keep using existing list item fields plus `homeGym`.
2. Switch detail screens that need home badges to **`/api/v4/gyms/{id}`**.
3. Switch search category strip to **`/api/v2/gyms/count/by-category`**; treat `type=HOME_ELIGIBLE` as the first chip.
4. Pass `tier` or `packageId` whenever the UI is scoped to a package (purchase funnel or active sub).
5. Use `meta.homeEligibleCount` for count labels; do not count only the current page.
6. When `minTier` is null, treat as “cannot be home”.
7. Badge copy from `minTier` / `tiers` (e.g. “Silver+”); do not depend on access-hours for this badge.
8. After purchase, open selection from `…/home-gym/gyms` and expect the same gyms shown pre-purchase for that package.

---

## 8. Error / empty states

- Empty list: `items: []`; optional localized `message` (e.g. gym not found).
- Invalid `tier`: `400` with `INVALID_TIER`.
- Invalid category: `400` with `INVALID_CATEGORY`.
- Missing gym detail: `404`.

---

## 9. Ownership

| Layer | Service |
|-------|---------|
| Routing / JWT edge | gateway-backend |
| Rule engine, list/detail/search payloads, admin switches | catalog-backend (`HomeGymRuleService`) |
| Selection, visits, history after purchase | order-backend |

Admin configuration (master switch, per-package `homeEligible`, cascade to higher/lower tiers) is server-side; mobile only reads the resulting flags.
