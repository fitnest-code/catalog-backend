package az.fitnest.catalog.service.landing;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LandingMembershipSupportTest {

    @Test
    void membershipFromName_mapsKnownAndLocalizedNames() {
        assertEquals("platinum", LandingMembershipSupport.membershipFromName("Platinum Plus"));
        assertEquals("gold", LandingMembershipSupport.membershipFromName("Qızıl"));
        assertEquals("silver", LandingMembershipSupport.membershipFromName("Gümüş paket"));
        assertEquals("bronze", LandingMembershipSupport.membershipFromName("anything else"));
        assertEquals("bronze", LandingMembershipSupport.membershipFromName(null));
    }

    @Test
    void membershipRank_ordersTiersAscending() {
        assertEquals(1, LandingMembershipSupport.membershipRank("bronze"));
        assertEquals(2, LandingMembershipSupport.membershipRank("silver"));
        assertEquals(3, LandingMembershipSupport.membershipRank("gold"));
        assertEquals(4, LandingMembershipSupport.membershipRank("platinum"));
        assertEquals(1, LandingMembershipSupport.membershipRank("unknown"));
    }

    @Test
    void minEntryMembership_picksLowestTierAndDefaultsToBronze() {
        assertEquals("silver", LandingMembershipSupport.minEntryMembership(List.of("platinum", "silver", "gold")));
        assertEquals("bronze", LandingMembershipSupport.minEntryMembership(List.of()));
        assertEquals("bronze", LandingMembershipSupport.minEntryMembership(null));
    }

    @Test
    void accessMembershipsSorted_dedupesAndSortsAscending() {
        assertEquals(
                List.of("silver", "gold", "platinum"),
                LandingMembershipSupport.accessMembershipsSorted(List.of("Platinum", "Silver", "Gold", "silver")));
        assertEquals(List.of("bronze"), LandingMembershipSupport.accessMembershipsSorted(List.of()));
    }

    @Test
    void normalizeTier_rejectsUnknownValues() {
        assertEquals("gold", LandingMembershipSupport.normalizeTier(" Gold "));
        assertNull(LandingMembershipSupport.normalizeTier("diamond"));
        assertNull(LandingMembershipSupport.normalizeTier(null));
    }
}
