package mn.edu.must.sqat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    private static final String STUDENT_ID = "S001";
    private static final String COURSE_ID = "CS201";
    private static final String EMAIL = "s001@must.edu.mn";

    @Mock StudentRepository repository;
    @Mock EmailSender emailSender;
    CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    private static Student activeStudent(List<String> taken) {
        return new Student(STUDENT_ID, EMAIL, true, taken);
    }

    /** Хичээл байгаа, өмнөх холбоо хичээл нь CS101 гэсэн энгийн нөхцөл бэлдэнэ. */
    private void stubCourseWithPrerequisiteCs101() {
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS101"));
    }

    // A1
    @Test
    void register_activeStudentWithPrerequisites_returnsOkSavesAndSendsEmail() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent(List.of("CS101")));
        stubCourseWithPrerequisiteCs101();

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(EMAIL), anyString(), anyString());
    }

    // A2
    @Test
    void register_unknownStudent_returnsErrorNoStudentAndNothingSavedOrSent() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(null);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // A3
    @Test
    void register_inactiveStudent_returnsErrorInactiveAndNoEmailInteraction() {
        when(repository.findStudent(STUDENT_ID))
                .thenReturn(new Student(STUDENT_ID, EMAIL, false, List.of("CS101")));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verifyNoInteractions(emailSender);
        verify(repository, never()).saveRegistration(anyString(), anyString());
    }

    // A4
    @Test
    void register_missingPrerequisite_returnsErrorPrerequisitesAndNotSaved() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent(List.of()));
        stubCourseWithPrerequisiteCs101();

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // A5
    @Test
    void register_emailSenderThrows_stillReturnsOkAndRegistrationSaved() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent(List.of("CS101")));
        stubCourseWithPrerequisiteCs101();
        doThrow(new RuntimeException("SMTP server unavailable"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
    }

    // A6 — аргумент бүрийн null-ийг тусад нь шалгана
    @Test
    void register_nullArguments_throwsIllegalArgumentAndTouchesNoDependency() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null, COURSE_ID));
        assertThrows(IllegalArgumentException.class, () -> service.register(STUDENT_ID, null));
        assertThrows(IllegalArgumentException.class, () -> service.register(null, null));

        verifyNoInteractions(repository, emailSender);
    }

    // A7
    @Test
    void register_success_emailBodyContainsCourseCode() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent(List.of("CS101")));
        stubCourseWithPrerequisiteCs101();
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);

        service.register(STUDENT_ID, COURSE_ID);

        verify(emailSender).send(eq(EMAIL), anyString(), body.capture());
        assertTrue(body.getValue().contains(COURSE_ID),
                "и-мэйлийн бие хичээлийн кодыг агуулах ёстой, гэтэл: " + body.getValue());
    }

    // A8
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "NO_STUDENT,       ERROR_NO_STUDENT",
            "INACTIVE_STUDENT, ERROR_INACTIVE_STUDENT",
            "PREREQUISITES,    ERROR_PREREQUISITES"
    })
    void register_errorScenario_returnsExpectedErrorCodeAndNeverSaves(String scenario, String expected) {
        // Мөр бүр өөр stub ашигладаг тул lenient(): өөр өөр мөрөнд хэрэглэгддэггүй stub-д
        // strict stubs UnnecessaryStubbingException өгөхгүй.
        switch (scenario) {
            case "NO_STUDENT" ->
                    lenient().when(repository.findStudent(STUDENT_ID)).thenReturn(null);
            case "INACTIVE_STUDENT" ->
                    lenient().when(repository.findStudent(STUDENT_ID))
                            .thenReturn(new Student(STUDENT_ID, EMAIL, false, List.of("CS101")));
            case "PREREQUISITES" -> {
                lenient().when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent(List.of()));
                lenient().when(repository.courseExists(COURSE_ID)).thenReturn(true);
                lenient().when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CS101"));
            }
            default -> throw new IllegalArgumentException("unknown scenario " + scenario);
        }

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals(expected, result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }
}
