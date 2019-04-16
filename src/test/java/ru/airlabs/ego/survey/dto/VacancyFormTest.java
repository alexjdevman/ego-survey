package ru.airlabs.ego.survey.dto;

import org.junit.Before;
import org.junit.Test;
import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.core.entity.Vacancy;
import ru.airlabs.ego.survey.dto.vacancy.VacancyForm;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * Тест модели для представления данных по вакансии на UI
 *
 * @author Aleksey Gorbachev
 */
public class VacancyFormTest {

    private User user;

    @Before
    public void setUp() {
        user = new User();
        user.setId(0L);
        user.setName("EgoProfil Admin");
        user.setEmail("admin@egoprofil.com");
        user.setActive(Boolean.TRUE);
    }

    @Test
    public void test_Build_Vacancy() {
        VacancyForm form = new VacancyForm();
        form.setName("Java developer");
        form.setUrl("hh.ru/vacancy/9999");
        form.setSalary(120000L);
        form.setSurveyId(1L);

        Vacancy vacancy = form.buildVacancy(user);
        assertNotNull(vacancy);
        assertEquals("Java developer", vacancy.getName());
        assertEquals("hh.ru/vacancy/9999", vacancy.getUrl());
        assertEquals(120000L, vacancy.getSalary().longValue());
        assertEquals(1L, vacancy.getSurveyId().longValue());
        assertEquals(0L, vacancy.getManagerId().longValue());
        assertNotNull(vacancy.getDateCreate());
    }

    @Test
    public void test_Copy_Data_From_Vacancy() {
        Vacancy vacancy = new Vacancy();
        vacancy.setId(1L);
        vacancy.setSurveyId(1L);
        vacancy.setName("Scala developer");
        vacancy.setUrl("hh.ru/vacancy/6666");
        vacancy.setSalary(150000L);

        VacancyForm form = new VacancyForm();
        form.copyDataFromVacancy(vacancy);
        assertEquals("Scala developer", form.getName());
        assertEquals("hh.ru/vacancy/6666", form.getUrl());
        assertEquals(150000L, form.getSalary().longValue());
        assertEquals(1L, form.getSurveyId().longValue());
        assertEquals(1L, form.getId().longValue());

    }

}
