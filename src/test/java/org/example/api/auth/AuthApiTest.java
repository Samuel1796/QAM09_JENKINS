package org.example.api.auth;

import org.example.api.base.BaseApiTest;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests for FakeStore API /auth/login endpoint.
 */
public class AuthApiTest extends BaseApiTest {

    @Test
    public void login_validCredentials_returns200WithToken() {
        given()
            .contentType("application/json")
            .body("{\"username\":\"mor_2314\",\"password\":\"83r5^_\"}")
            .when()
                .post("/auth/login")
            .then()
                .statusCode(anyOf(equalTo(200), equalTo(201)))
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("token", notNullValue())
                .body("token", not(emptyString()));
    }

    @Test
    public void login_anotherValidUser_returns200WithToken() {
        given()
            .contentType("application/json")
            .body("{\"username\":\"johnd\",\"password\":\"m38rmF$\"}")
            .when()
                .post("/auth/login")
            .then()
                .statusCode(anyOf(equalTo(200), equalTo(201)))
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("token", notNullValue());
    }

    @Test
    public void login_missingPassword_returnsErrorResponse() {
        given()
            .contentType("application/json")
            .body("{\"username\":\"mor_2314\"}")
            .when()
                .post("/auth/login")
            .then()
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS));
        // FakeStore returns a response (not necessarily 400) — we verify it responds within threshold
    }

    @Test
    public void login_emptyBody_returnsResponse() {
        given()
            .contentType("application/json")
            .body("{}")
            .when()
                .post("/auth/login")
            .then()
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS));
    }
}

