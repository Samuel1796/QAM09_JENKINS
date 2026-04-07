package org.example.api.carts;

import org.example.api.base.BaseApiTest;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests for FakeStore API /carts endpoints.
 */
public class CartsApiTest extends BaseApiTest {

    @Test
    public void getAllCarts_returns200WithExpectedFields() {
        given()
            .when()
                .get("/carts")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("[0].id", notNullValue())
                .body("[0].userId", notNullValue())
                .body("[0].date", notNullValue())
                .body("[0].products", notNullValue());
    }

    @Test
    public void getAllCarts_limitResults_returnsCorrectCount() {
        given()
            .queryParam("limit", 3)
            .when()
                .get("/carts")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("size()", equalTo(3));
    }

    @Test
    public void getAllCarts_sortDesc_returns200() {
        given()
            .queryParam("sort", "desc")
            .when()
                .get("/carts")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("size()", greaterThan(0));
    }

    @Test
    public void getCartById_validId_returns200WithProducts() {
        given()
            .when()
                .get("/carts/1")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("id", equalTo(1))
                .body("userId", notNullValue())
                .body("products", notNullValue())
                .body("products[0].productId", notNullValue())
                .body("products[0].quantity", notNullValue());
    }

    @Test
    public void getCartsByUser_returns200() {
        given()
            .when()
                .get("/carts/user/1")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("size()", greaterThan(0));
    }

    @Test
    public void getCartsByDateRange_returns200() {
        given()
            .queryParam("startdate", "2019-01-01")
            .queryParam("enddate", "2020-01-01")
            .when()
                .get("/carts")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS));
    }

    @Test
    public void addCart_returns200WithId() {
        String body = "{\"userId\":1,\"date\":\"2024-01-01\"," +
                      "\"products\":[{\"productId\":1,\"quantity\":2}]}";
        given()
            .contentType("application/json")
            .body(body)
            .when()
                .post("/carts")
            .then()
                .statusCode(anyOf(equalTo(200), equalTo(201)))
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("id", notNullValue());
    }

    @Test
    public void updateCart_put_returns200() {
        String body = "{\"userId\":1,\"date\":\"2024-06-01\"," +
                      "\"products\":[{\"productId\":2,\"quantity\":3}]}";
        given()
            .contentType("application/json")
            .body(body)
            .when()
                .put("/carts/1")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("id", notNullValue());
    }

    @Test
    public void updateCart_patch_returns200() {
        String body = "{\"products\":[{\"productId\":3,\"quantity\":1}]}";
        given()
            .contentType("application/json")
            .body(body)
            .when()
                .patch("/carts/1")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS));
    }

    @Test
    public void deleteCart_returns200() {
        given()
            .when()
                .delete("/carts/1")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("id", notNullValue());
    }
}

