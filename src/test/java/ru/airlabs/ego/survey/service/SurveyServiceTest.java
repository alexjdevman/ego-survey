package ru.airlabs.ego.survey.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import ru.airlabs.ego.survey.dto.survey.SurveyInfo;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static junit.framework.TestCase.*;

/**
 * Тест сервиса для работы с опросами
 *
 * @author Aleksey Gorbachev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
@Transactional
public class SurveyServiceTest {

    @Autowired
    private SurveyService surveyService;

    @Test
    public void test_Get_Accessible_Survey_List_For_User_AND_RU_Locale() {
        final Long adminUserId = 0L;
        final Locale russianLocale = new Locale.Builder().setLanguage("ru").build();
        List<SurveyInfo> result =  surveyService.getAccessibleSurveyListForUser(adminUserId, Boolean.TRUE, russianLocale);
        Optional<SurveyInfo> surveyResult = result.stream().filter(s -> s.getId() == 1).findFirst();
        if (surveyResult.isPresent()) {
            SurveyInfo info = surveyResult.get();

            assertNotNull(result);
            assertTrue(!result.isEmpty());
            assertEquals(1L, info.getId().longValue());
            assertEquals("Соционика", info.getName());
        }
    }

    @Test
    public void test_Get_Accessible_Survey_List_For_User_AND_ENG_Locale() {
        final Long adminUserId = 0L;
        final Locale englishLocale = new Locale.Builder().setLanguage("en").build();
        List<SurveyInfo> result =  surveyService.getAccessibleSurveyListForUser(adminUserId, Boolean.TRUE, englishLocale);

        Optional<SurveyInfo> surveyResult = result.stream().filter(s -> s.getId() == 1).findFirst();
        if (surveyResult.isPresent()) {
            SurveyInfo info = surveyResult.get();

            assertNotNull(result);
            assertTrue(!result.isEmpty());
            assertEquals(1L, info.getId().longValue());
            assertEquals("Socionics", info.getName());
        }
    }

}
