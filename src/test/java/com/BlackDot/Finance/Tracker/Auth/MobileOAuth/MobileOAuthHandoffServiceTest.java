package com.BlackDot.Finance.Tracker.Auth.MobileOAuth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MobileOAuthHandoffServiceTest {
    @Test
    void createsTheRfc7636Sha256Challenge() {
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

        assertEquals(
                "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
                MobileOAuthHandoffService.createCodeChallenge(verifier));
    }
}
