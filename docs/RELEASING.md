# Budowanie i publikowanie KinoStream

## Podpisywanie aplikacji

KinoStream został przygotowany w Google AI Studio. Nie dodawaj losowego klucza podpisującego do GitHub Secrets i nie generuj nowego klucza tylko po to, aby przejść CI.

Jeśli publikujesz aplikację przez **Google AI Studio → Ustawienia → Opublikuj → Opublikuj w Sklepie Play**, AI Studio zarządza podpisywaniem APK. Wersje produkcyjne po publikacji zarządza się w Google Play Console. Kontynuuj ten sam sposób publikowania, aby zachować ciągłość podpisu aplikacji.

GitHub Actions w tym repozytorium służy wyłącznie do sprawdzenia kodu: buduje debug APK, uruchamia testy jednostkowe i udostępnia APK jako krótkotrwały artefakt CI. Tego artefaktu nie należy traktować jako produkcyjnej aktualizacji aplikacji.

Jeśli kiedykolwiek zdecydujesz, że GitHub Actions ma samodzielnie budować i publikować podpisane wydania, najpierw trzeba ustalić, jaki klucz jest używany dla istniejącej aplikacji. Nie wolno zastępować go nowym kluczem bez sprawdzenia skutków — Android może odrzucić aktualizację podpisaną innym certyfikatem.

## Testy CI

Każdy push na `main` i gałęzie `fix/**` buduje debug APK, uruchamia `testDebugUnitTest` i udostępnia debug APK jako tymczasowy artefakt Actions. Ten artefakt jest przeznaczony wyłącznie do testów.
