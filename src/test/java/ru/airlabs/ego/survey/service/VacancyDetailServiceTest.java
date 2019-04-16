package ru.airlabs.ego.survey.service;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.survey.dto.vacancy.VacancyUserDetail;

import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * Тестирование сервиса для получения подробностей по вакансиям
 *
 * @author Aleksey Gorbachev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
@Transactional
public class VacancyDetailServiceTest {

    @Autowired
    private VacancyDetailService vacancyDetailService;

    @Ignore("интеграционный тест")
    @Test
    public void test_Get_Feedback_For_Vacancy() {
        final Long vacancyId = 0L;
        final Long userId = 0L; //admin
        final User user = new User();
        user.setId(userId);

        Page<VacancyUserDetail> detailsPage = vacancyDetailService.getFeedbackForVacancy(vacancyId, user, PageRequest.of(0, 10));
        Optional<VacancyUserDetail> firstDetailOptional = detailsPage.getContent().stream().findFirst();
        if (firstDetailOptional.isPresent()) {
            VacancyUserDetail detail = firstDetailOptional.get();

            assertNotNull(detail);
            assertNotNull(detail.getName());
            assertNotNull(detail.getEmail());
            assertNotNull(detail.getSurveyCreate());
            assertNotNull(detail.getSurveyUpdate());
            assertNotNull(detail.getCompareResult());
            assertEquals(0L, detail.getVacancyId().longValue());
            assertEquals(288, detail.getProgress().intValue());

        }
    }

}
