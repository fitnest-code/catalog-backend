-- Əsas zal (home gym) must be opt-in from admin membership policy.
-- V20 defaulted gyms.home_eligible to TRUE, so every active gym appeared as selectable
-- (~163 on prod) even when v4 primaryEnabled was never turned on.

ALTER TABLE gyms
    ALTER COLUMN home_eligible SET DEFAULT FALSE;

-- Turn off the legacy master switch for everyone first.
UPDATE gyms
SET home_eligible = FALSE
WHERE home_eligible IS DISTINCT FROM FALSE;

-- Re-enable only gyms that already have at least one tier with primaryEnabled=true
-- in the v4 membership policy JSON (admin "Əsas zal kimi seçilə bilər").
UPDATE gyms g
SET home_eligible = TRUE
WHERE EXISTS (
    SELECT 1
    FROM gym_membership_policy_v4 p
    WHERE p.gym_id = g.id
      AND EXISTS (
          SELECT 1
          FROM jsonb_array_elements(
                   CASE
                       WHEN p.payload IS NULL OR btrim(p.payload) = '' THEN '[]'::jsonb
                       ELSE (p.payload::jsonb -> 'packages')
                   END
               ) pkg
          WHERE COALESCE((pkg ->> 'primaryEnabled')::boolean, false) = true
      )
);
