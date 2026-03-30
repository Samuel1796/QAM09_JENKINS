package org.example.api;

import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests for FakeStore API /users endpoints.
 */
public class UsersApiTest extends BaseApiTest {

    @Test
    public void getAllUsers_returns200WithExpectedFields() {
        given()
            .when()
                .get("/users")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("[0].id", notNullValue())
                .body("[0].email", notNullValue())
                .body("[0].username", notNullValue())
                .body("[0].name.firstname", notNullValue())
                .body("[0].name.lastname", notNullValue())
                .body("[0].address.city", notNullValue())
                .body("[0].phone", notNullValue());
    }

    @Test
    public void getAllUsers_limitResults_returnsCorrectCount() {
        given()
            .queryParam("limit", 3)
            .when()
                .get("/users")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("size()", equalTo(3));
    }

    @Test
    public void getAllUsers_sortDesc_returns200() {
        given()
            .queryParam("sort", "desc")
            .when()
                .get("/users")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("size()", greaterThan(0));
    }

    @Test
    public void getUserById_validId_returns200WithAllFields() {
        given()
            .when()
                .get("/users/1")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("id", equalTo(1))
                .body("email", notNullValue())
                .body("username", notNullValue())
                .body("name.firstname", notNullValue())
                .body("name.lastname", notNullValue())
                .body("address.street", notNullValue())
                .body("address.city", notNullValue())
                .body("address.zipcode", notNullValue())
                .body("address.geolocation.lat", notNullValue())
                .body("address.geolocation.long", notNullValue())
                .body("phone", notNullValue());
    }

    @Test
    public void addUser_returns200WithId() {
        String body = "{\"email\":\"test@example.com\",\"username\":\"testuser\"," +
                      "\"password\":\"pass123\",\"name\":{\"firstname\":\"John\",\"lastname\":\"Doe\"}," +
                      "\"address\":{\"city\":\"Accra\",\"street\":\"Main St\",\"number\":1," +
                      "\"zipcode\":\"00233\",\"geolocation\":{\"lat\":\"5.6\",\"long\":\"-0.2\"}}," +
                      "\"phone\":\"0200000000\"}";
        given()
            .contentType("application/json")
            .body(body)
            .when()
                .post("/users")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("id", notNullValue());
    }

    @Test
    public void updateUser_put_returns200() {
        String body = "{\"email\":\"updated@example.com\",\"username\":\"updateduser\"," +
                      "\"password\":\"newpass\",\"name\":{\"firstname\":\"Jane\",\"lastname\":\"Smith\"}," +
                      "\"address\":{\"city\":\"Kumasi\",\"street\":\"Second St\",\"number\":2," +
                      "\"zipcode\":\"00234\",\"geolocation\":{\"lat\":\"6.7\",\"long\":\"-1.6\"}}," +
                      "\"phone\":\"0201111111\"}";
        given()
            .contentType("application/json")
            .body(body)
            .when()
                .put("/users/1")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("id", notNullValue());
    }

    @Test
    public void updateUser_patch_returns200() {
        String body = "{\"email\":\"patched@example.com\"}";
        given()
            .contentType("application/json")
            .body(body)
            .when()
                .patch("/users/1")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS));
    }

    @Test
    public void deleteUser_returns200() {
        given()
            .when()
                .delete("/users/1")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("id", notNullValue());
    }
}
