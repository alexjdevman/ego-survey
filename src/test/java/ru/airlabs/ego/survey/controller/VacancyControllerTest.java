package ru.airlabs.ego.survey.controller;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-context.xml", "classpath:/spring/appServlet.xml"})
@WebAppConfiguration
public class VacancyControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Ignore("интеграционный тест")
    @Test
    public void test_Get_Vacancy() {
        given().webAppContextSetup(context).
                when().
                get("/vacancy/5").
                then().
                contentType("application/json").
                body("id", equalTo(5)).
                body("name", equalTo("Java developer")).
                body("url", equalTo("hh.ru/vacancy/999")).
                body("salary", equalTo(200000));
    }

    @Ignore("интеграционный тест")
    @Test
    public void test_Get_Surveys() {
        given().webAppContextSetup(context).
                when().
                get("/vacancy/surveys/0").
                then().
                contentType("application/json").
                body("[0].id", equalTo(1)).
                body("[0].name", equalTo("Соционика"));
    }

    @Ignore("интеграционный тест")
    @Test
    public void test_Get_Feedback_For_Vacancy() {
        given().webAppContextSetup(context).
                when().
                get("/vacancy/user/feedback/0").
                then().
                contentType("application/json").
                body("[0].vacancyId", equalTo(0)).
                body("[0].email", notNullValue()).
                body("[0].name", notNullValue()).
                body("[0].surveyCreate", notNullValue()).
                body("[0].surveyUpdate", notNullValue()).
                body("[0].compareResult", notNullValue()).
                body("[0].progress", equalTo(288));
    }

}
