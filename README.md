# Лаб 8: Mockito-гоор хамаарлыг орлуулж тестлэх

Оюутан: Х.Цогтбаяр, Код: B232270072

## Орчин

```
java --version
openjdk 21.0.13 2026-10-20


mvn --version
Apache Maven 3.9.9
Maven home: /usr/share/maven
Java version: 21.0.13, vendor: Debian, runtime: /usr/lib/jvm/java-21-openjdk-amd64
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "6.12.113+deb13-amd64", arch: "amd64", family: "unix"

```

## Тестийн үр дүн

`results/mvn-test.txt`-ийн сүүлийн үр дүн: **Tests run: 10**, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS.
(7 энгийн тест + A8-ийн 3 мөр. Мөр бүр тусдаа тест болж тоологддог.)

## A8 дахь lenient()

A8-д `lenient().when(...)` ашигласан боловч одоогийн `switch` бүтэц мөр бүрд зөвхөн шаардлагатай stub-уудыг тохируулдаг тул `lenient()` зайлшгүй шаардлагагүй. Харин ашиглагдаагүй stub байгаа үед `lenient()` нь тухайн stub-оос үүдэх `UnnecessaryStubbingException` алдааг дарах зориулалттай.

## Хэсэг B: мини мутаци

Мутант бүрийг `CourseRegistrationService.java` дээр хийж, `mvn test` ажиллуулсны дараа `git checkout`-оор кодыг эх хувилбар руу нь сэргээсэн.

| № | Мутаци | Унасан тестүүд | Үр дүн | Файл |
|---|--------|----------------|--------|------|
| 1 | `if (!student.active()) ...` шалгалтыг комментлосон | A3 `register_inactiveStudent_returnsErrorInactiveAndNoEmailInteraction`, A8 `register_errorScenario_...[2]` (INACTIVE_STUDENT мөр). Хоёулаа `ERROR_INACTIVE_STUDENT` хүлээсэн ч `ERROR_NO_COURSE` ирсэн | Failures: 2, Errors: 0, BUILD FAILURE | `results/mvn-test-mutant-1.txt` |
| 2 | `repository.saveRegistration(...)` дуудалтыг комментлосон | A1 `register_activeStudentWithPrerequisites_returnsOkSavesAndSendsEmail`, A5 `register_emailSenderThrows_stillReturnsOkAndRegistrationSaved`. Хоёулаа `Wanted but not invoked: saveRegistration` | Failures: 2, Errors: 0, BUILD FAILURE | `results/mvn-test-mutant-2.txt` |
| 3 | `\|\| courseId == null` хэсгийг устгасан | A6 `register_nullArguments_throwsIllegalArgumentAndTouchesNoDependency` (`register(STUDENT_ID, null)` дээр exception шидээгүй) | Failures: 1, Errors: 0, BUILD FAILURE | `results/mvn-test-mutant-3.txt` |

Гурван мутант бүгд ядаж нэг тестийг унагасан тул тестүүд эвдрэлийг илрүүлж чадаж байна. UnnecessaryStubbingException-ээр унасан тест байхгүй, бүгд ердийн assertion/verify алдаа байсан.

Мутант 3-ыг A6 засваргүйгээр шууд барьсан, учир нь A6-г эхнээс нь хоёр аргумент тус бүрийн null-ийг тусад нь шалгадаг (`null, COURSE_ID`, `STUDENT_ID, null`, `null, null`) болгож бичсэн. Тиймээс лабд заасан "A6-г засаад дахин ажиллуул" алхам шаардлагагүй болсон.

Мутаци бүрийн дараа кодыг сэргээж, эцэст нь `mvn -B test`-ийг дахин ажиллуулж ногоон байгааг баталсан: `results/mvn-test.txt` (Tests run: 10, BUILD SUCCESS), `src/main` нь эх хувилбараараа репод байгаа.

## Дүгнэлт

Mock нь дуудалтуудыг бүртгэж, урьдчилан тохируулсан хариуг буцаадаг, `verify`-аар хэн хэзээ дуудагдсаныг шалгах боломжтой объект юм. Харин гараар бичсэн fake нь жинхэнэ хөнгөн ажиллагаатай хэрэгжүүлэлт (жишээ нь санах ойн `HashMap`-тай repository) бөгөөд төлөвөөр нь шалгадаг. Fake-ийг олон тестэд дахин ашиглахад хялбар, харин mock тест бүрт өөрийн тохиргоотой байдаг. Өгөгдлийн сан, SMTP, сүлжээ, цаг зэрэг удаан, тогтворгүй, гаднын үйлчилгээг mock хийх нь зүйтэй, ингэснээр нэгжийн тест бүрэн тусгаарлагдана. Харин `Student` гэх мэт энгийн өгөгдлийн record, `List`, `String` шиг стандарт төрлүүд, мөн тестэлж буй классыг өөрийг нь mock хийх ёсгүй, тэгвэл бодит логик биш mock-ийг тестэлсэн болно. `verify`-г хэтрүүлбэл тест дуудалтын дараалал, тоо зэрэг дотоод хэрэгжүүлэлтийн дэлгэрэнгүйд наалдаж, зан төлөв өөрчлөгдөөгүй ч refactor хийхэд тест хэрэггүйгээр унадаг. Харин `verify` дутуу бол тест сул болно, жишээ нь `saveRegistration`-ийг шалгаагүй бол мутант 2-ыг ямар ч тест барихгүй байсан. Мутацийн дасгал харуулсанаар тест ногоон байх нь тест сайн гэсэн үг биш, кодыг зориуд эвдэхэд тест унаж байж л тест үнэхээр ажил хийж байгааг батлана.