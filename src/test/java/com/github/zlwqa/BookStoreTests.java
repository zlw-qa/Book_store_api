package com.github.zlwqa;

import annotations.JiraIssue;
import annotations.JiraIssues;
import annotations.Layer;
import annotations.Microservice;
import com.codeborne.selenide.logevents.SelenideLogger;
import com.github.zlwqa.lombok.UserResponseData;
import io.qameta.allure.*;
import io.qameta.allure.selenide.AllureSelenide;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;

import static com.github.zlwqa.config.App.*;
import static io.qameta.allure.Allure.step;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@Layer("REST")
@Owner("vshalunov")
@Tag("API")
@JiraIssues({@JiraIssue("HOMEWORK-326")})
@DisplayName("Тестирование веб-приложения Book Store")
public class BookStoreTests {

    @BeforeAll
    static void setup() {
        SelenideLogger.addListener("AllureSelenide", new AllureSelenide());
        RestAssured.baseURI = API_CONFIG.apiUrl();

        step("Получение токена авторизации и userId", () -> {
            UserResponseData userResponseData = given()
                    .contentType(JSON)
                    .body(setUserLoginData())
                    .when()
                    .post("/Account/v1/Login")
                    .then()
                    .extract().as(UserResponseData.class);

            USER_RESPONSE_DATA.setUserId(userResponseData.getUserId());
            USER_RESPONSE_DATA.setToken(userResponseData.getToken());
        });

    }

    @Test
    @DisplayName("Успешная генерация токена")
    @Tags({@Tag("Critical"), @Tag("Highest")})
    @Microservice("Generation Token")
    @Feature("Генерация токена")
    @Story("Метод /Account/v1/GenerateToken")
    @Severity(SeverityLevel.CRITICAL)
    @Link(name = "Book Store", url = "https://demoqa.com/books")
    void tokenGenerationTest() {

        given()
                .contentType(JSON)
                .body(setUserLoginData())
                .when()
                .post("/Account/v1/GenerateToken")
                .then().log().all()
                .statusCode(200)
                .body("token", notNullValue(),
                        "status", is("Success"),
                        "result", is("User authorized successfully."));
    }

    @Test
    @DisplayName("Отображение списка всех книг")
    @Tags({@Tag("Major"), @Tag("Medium")})
    @Microservice("BookStore")
    @Feature("Список книг")
    @Story("Метод GET /BookStore/v1/Books")
    @Severity(SeverityLevel.NORMAL)
    void displayAListOfAllBooksTest() {
        given()
                .contentType(JSON)
                .when()
                .get("/BookStore/v1/Books")
                .then().log().all()
                .statusCode(200)
                .body("books", notNullValue(),
                        "books[0].isbn", is("9781449325862"),
                        "books[0].title", is("Git Pocket Guide"));
    }

    @Test
    @DisplayName("Отображение определенной книги по ISBN в списке всех книг")
    @Tags({@Tag("Major"), @Tag("Medium")})
    @Microservice("BookStore")
    @Feature("Список книг")
    @Story("Метод GET /BookStore/v1/Book?ISBN=")
    @Severity(SeverityLevel.NORMAL)
    void displayABookByISBNInTheListOfAllBooksTest() {
        given()
                .contentType(JSON)
                .formParam("ISBN", "9781449325862")
                .when()
                .get("/BookStore/v1/Book")
                .then().log().all()
                .statusCode(200)
                .body(notNullValue())
                .body("isbn", is("9781449325862"),
                        "title", is("Git Pocket Guide"));
    }

    @Test
    @DisplayName("Добавление книги в профиль пользователя")
    @Tags({@Tag("Blocker"), @Tag("High")})
    @Microservice("BookStore")
    @Feature("Список добавленных книг в профиле пользователя")
    @Story("Метод POST /BookStore/v1/Books")
    @Severity(SeverityLevel.BLOCKER)
    static void addingABookToAUserProfileTest() {
        String data = "{\"userId\": \"" + USER_RESPONSE_DATA.getUserId() + "\"," +
                "\"collectionOfIsbns\" : [{\"isbn\":\"9781449325862\"}]}";

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer " + USER_RESPONSE_DATA.getToken())
                .body(data)
                .when()
                .post("/BookStore/v1/Books")
                .then().log().all()
                .statusCode(201)
                .body("books[0].isbn", is("9781449325862"));
    }

    @Test
    @DisplayName("Удаление добавленной книги из профиля пользователя")
    @Tags({@Tag("Blocker"), @Tag("High")})
    @Microservice("BookStore")
    @Feature("Список добавленных книг в профиле пользователя")
    @Story("Метод DELETE /BookStore/v1/Book")
    @Severity(SeverityLevel.BLOCKER)
    static void removingAnAddedBookFromAUserProfileTest() {

        String data = "{\"isbn\":\"9781449325862\"," +
                "userId\": \"" + USER_RESPONSE_DATA.getUserId() + "\"}";

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer " + USER_RESPONSE_DATA.getToken())
                .baseUri("https://demoqa.com")
                .body(data)
                .when()
                .delete("/BookStore/v1/Book")
                .then().log().all()
                .statusCode(204)
                .body(is(""));
    }
}

