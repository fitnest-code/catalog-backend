#!/usr/bin/env python3
"""Prod audit: V4 primaryEnabled vs packages[].homeEligible for all home-eligible gyms."""
import json
import urllib.request


def get(url: str):
    req = urllib.request.Request(
        url,
        headers={
            "Accept-Language": "az",
            "X-User-Id": "1",
            "Accept": "application/json",
            "X-User-Roles": "ROLE_ADMIN",
        },
    )
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)


def get_opt(url: str):
    try:
        return get(url), None
    except Exception as e:
        return None, str(e)


def list_home_eligible_ids():
    ids = []
    page = 0
    total = None
    while True:
        data = get(
            f"http://catalog-backend:8080/api/v3/gyms?homeEligible=true&page={page}&size=50"
        )
        body = data.get("data", data)
        items = body.get("content") or body.get("gyms") or body.get("items") or []
        # v3 list is unwrapped: {items, meta}; ids are string gymId
        if not isinstance(body, dict):
            break
        if "items" in data:
            body = data
            items = data.get("items") or []
        for g in items:
            gid = g.get("gymId") or g.get("gym_id") or g.get("id")
            if gid is not None:
                ids.append(int(gid))
        meta = body.get("meta") or data.get("meta") or {}
        total = meta.get("total")
        page_size = int(meta.get("pageSize") or 50)
        # API page is 1-based in meta.page; our request page is 0-based
        if not items or (total is not None and len(set(ids)) >= int(total)) or page > 40:
            break
        if total is not None and (page + 1) * page_size >= int(total):
            break
        page += 1
    return sorted(set(ids))


def main():
    for tier in ("PLATINUM", "GOLD", "SILVER", "BRONZE"):
        url = (
            "http://catalog-backend:8080/api/v3/gyms"
            f"?homeEligible=true&tier={tier}&q=qala&page=0&size=20"
        )
        data, err = get_opt(url)
        if err:
            print("SEARCH_ERR", tier, err)
            continue
        body = data.get("data", data)
        items = body.get("content") or body.get("gyms") or body.get("items") or []
        meta = body.get("meta") or {}
        # Response may be wrapped or bare {items, meta}
        if isinstance(data, dict) and "items" in data:
            body = data
            items = data.get("items") or []
            meta = data.get("meta") or {}
        names = [
            (i.get("name"), i.get("gymId") or i.get("gym_id") or i.get("id"))
            for i in items
        ]
        print(
            "SEARCH",
            json.dumps(
                {
                    "tier": tier,
                    "q": "qala",
                    "total": meta.get("total"),
                    "homeEligibleCount": meta.get("homeEligibleCount"),
                    "names": names,
                },
                ensure_ascii=False,
            ),
        )

    ids = list_home_eligible_ids()
    print("home_eligible_list_count", len(ids))

    policy_ok = policy_fail = 0
    conflicts = []
    gold_only = []
    chip_tier_mismatch = []
    no_chip = []

    for gid in ids:
        detail = get(f"http://catalog-backend:8080/api/v4/gyms/{gid}")
        body = detail.get("data", detail)
        name = body.get("name")
        packages = {
            p.get("tier"): bool(p.get("homeEligible")) for p in (body.get("packages") or [])
        }
        home = body.get("homeGym") or {}
        tiers = sorted(home.get("tiers") or [])
        chip_on = sorted([t for t, v in packages.items() if v])
        if chip_on != tiers:
            chip_tier_mismatch.append(
                {"id": gid, "name": name, "chips": chip_on, "tiers": tiers}
            )
        if packages.get("GOLD") and not packages.get("PLATINUM"):
            gold_only.append({"id": gid, "name": name, "packages": packages, "tiers": tiers})
        if not any(packages.values()):
            no_chip.append({"id": gid, "name": name, "homeGym": home})

        pol, err = get_opt(
            f"http://catalog-backend:8080/api/v4/admin/gyms/{gid}/membership-policy"
        )
        if pol is None:
            policy_fail += 1
            continue
        policy_ok += 1
        rules = (pol.get("data", pol).get("packages")) or []
        pev = {}
        for r in rules:
            t = r.get("tier")
            if t:
                pev[t] = bool(r.get("primaryEnabled"))
        for tier in ("BRONZE", "SILVER", "GOLD", "PLATINUM"):
            api = packages.get(tier, False)
            # Bug class: admin primary off but API still shows home chip.
            if pev.get(tier) is False and api is True:
                conflicts.append(
                    {
                        "id": gid,
                        "name": name,
                        "tier": tier,
                        "policyPrimary": False,
                        "apiHome": True,
                    }
                )
            if pev.get(tier) is True and api is False:
                conflicts.append(
                    {
                        "id": gid,
                        "name": name,
                        "tier": tier,
                        "policyPrimary": True,
                        "apiHome": False,
                        "kind": "policy_on_api_off",
                    }
                )

    print("policy_fetched", policy_ok, "policy_fail", policy_fail)
    print("chip_vs_tiers_mismatches", len(chip_tier_mismatch))
    for m in chip_tier_mismatch[:50]:
        print("CHIP_TIER", json.dumps(m, ensure_ascii=False))
    print("policy_vs_api_conflicts", len(conflicts))
    for c in conflicts[:100]:
        print("CONFLICT", json.dumps(c, ensure_ascii=False))
    print("gold_on_plat_off", len(gold_only))
    for x in gold_only:
        print("GOLD_ONLY", json.dumps(x, ensure_ascii=False))
    print("home_list_but_no_tier_chip", len(no_chip))
    for x in no_chip[:30]:
        print("NO_CHIP", json.dumps(x, ensure_ascii=False))


if __name__ == "__main__":
    main()
