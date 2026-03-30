package org.example.api;

import io.restassured.RestAssured;
import org.testng.annotations.BeforeSuite;

public class BaseApiTest {

    /** Maximum acceptable response time in milliseconds (Requirement 1.6). */
    protected static final long RESPONSE_TIME_THRESHOLD_MS = 5000L;

    @BeforeSuite
    public void configureRestAssured() {
        RestAssured.baseURI = "https://fakestoreapi.com";
    }
}
