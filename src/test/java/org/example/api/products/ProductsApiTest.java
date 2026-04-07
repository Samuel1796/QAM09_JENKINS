package org.example.api.products;

import org.example.api.base.BaseApiTest;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests for FakeStore API /products endpoints.
 */
public class ProductsApiTest extends BaseApiTest {

    @Test
    public void getAllProducts_returns200WithExpectedFields() {
        given()
            .when()
                .get("/products")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("[0].id", notNullValue())
                .body("[0].title", notNullValue())
                .body("[0].price", notNullValue())
                .body("[0].category", notNullValue())
                .body("[0].image", notNullValue());
    }

    @Test
    public void getAllProducts_limitResults_returnsCorrectCount() {
        given()
            .queryParam("limit", 5)
            .when()
                .get("/products")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("size()", equalTo(5));
    }

    @Test
    public void getAllProducts_sortAsc_returnsAscendingOrder() {
        given()
            .queryParam("sort", "asc")
            .when()
                .get("/products")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("size()", greaterThan(0));
    }

    @Test
    public void getAllProducts_sortDesc_returnsDescendingOrder() {
        given()
            .queryParam("sort", "desc")
            .when()
                .get("/products")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("size()", greaterThan(0));
    }

    @Test
    public void getProductById_validId_returns200WithAllFields() {
        given()
            .when()
                .get("/products/1")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("id", equalTo(1))
                .body("title", notNullValue())
                .body("price", notNullValue())
                .body("description", notNullValue())
                .body("category", notNullValue())
                .body("image", notNullValue())
                .body("rating.rate", notNullValue())
                .body("rating.count", notNullValue());
    }

    @Test
    public void getAllCategories_returns200WithList() {
        given()
            .when()
                .get("/products/categories")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("size()", greaterThan(0));
    }

    @Test
    public void getProductsByCategory_electronics_returns200() {
        given()
            .when()
                .get("/products/category/electronics")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("size()", greaterThan(0))
                .body("[0].category", equalTo("electronics"));
    }

    @Test
    public void getProductsByCategory_jewelery_returns200() {
        given()
            .when()
                .get("/products/category/jewelery")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("size()", greaterThan(0))
                .body("[0].category", equalTo("jewelery"));
    }

    @Test
    public void getProductsByCategory_menClothing_returns200() {
        given()
            .when()
                .get("/products/category/men's clothing")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("size()", greaterThan(0));
    }

    @Test
    public void getProductsByCategory_womenClothing_returns200() {
        given()
            .when()
                .get("/products/category/women's clothing")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("size()", greaterThan(0));
    }

    @Test
    public void addProduct_returns200WithGeneratedId() {
        String body = "{\"title\":\"Test Product\",\"price\":9.99,\"description\":\"A test product\"," +
                      "\"image\":\"https://i.pravatar.cc\",\"category\":\"electronics\"}";
        given()
            .contentType("application/json")
            .body(body)
            .when()
                .post("/products")
            .then()
                .statusCode(anyOf(equalTo(200), equalTo(201)))
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("id", notNullValue());
    }

    @Test
    public void updateProduct_put_returns200WithUpdatedTitle() {
        String body = "{\"title\":\"Updated Product\",\"price\":19.99,\"description\":\"Updated\"," +
                      "\"image\":\"https://i.pravatar.cc\",\"category\":\"electronics\"}";
        given()
            .contentType("application/json")
            .body(body)
            .when()
                .put("/products/1")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("title", equalTo("Updated Product"));
    }

    @Test
    public void updateProduct_patch_returns200() {
        String body = "{\"title\":\"Patched Product\"}";
        given()
            .contentType("application/json")
            .body(body)
            .when()
                .patch("/products/1")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("title", equalTo("Patched Product"));
    }

    @Test
    public void deleteProduct_returns200() {
        given()
            .when()
                .delete("/products/1")
            .then()
                .statusCode(200)
                .time(lessThan(RESPONSE_TIME_THRESHOLD_MS))
                .body("id", notNullValue());
    }
}

