# KinoStream: zgłaszanie błędów i Crashlytics

Kod aplikacji ma podłączone SDK Firebase Crashlytics oraz panel zgłoszeń w Ustawieniach. Aby raporty naprawdę trafiały do Twojego projektu, trzeba użyć konfiguracji Androida z tego samego projektu Firebase, w którym znajduje się kolekcja `filmweb_ratings`.

## 1. Konfiguracja Androida w Firebase

1. Otwórz [Firebase Console](https://console.firebase.google.com/) i wybierz istniejący projekt używany przez KinoStream.
2. W ustawieniach projektu dodaj/wybierz aplikację Android o identyfikatorze pakietu `com.aistudio.kinostream.rvpx`.
3. Pobierz z konsoli plik `google-services.json`. Nie wklejaj do niego ręcznie kluczy innego projektu.
4. Lokalnie umieść plik w `app/google-services.json`. Ten plik jest ignorowany przez Git, aby konfiguracja środowiska nie była przypadkowo commitowana.
5. W GitHub otwórz **Settings → Secrets and variables → Actions → New repository secret**, nazwij sekret `GOOGLE_SERVICES_JSON_BASE64` i wklej zawartość pliku zakodowaną jako Base64.

Kodowanie pliku w PowerShell:
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes(".\app\google-services.json")) | Set-Clipboard
```

Workflow sprawdza obecność projektu oraz identyfikator pakietu. Bez tego sekretu CI nadal buduje debug APK do testów, ale blokuje publikację produkcyjnego APK, żeby nie wypuścić wersji bez działającego połączenia z Firebase.

## 2. Włączenie usług Firebase

W tym samym projekcie Firebase:
- **Authentication → Sign-in method → Email/Password**: włącz logowanie e-mail/hasło.
- Utwórz konto administratora dla adresu `janczesko12@gmail.com` w Authentication. Przy pierwszym logowaniu aplikacja sama wyśle wiadomość weryfikacyjną, jeżeli adres nie jest jeszcze potwierdzony; kliknij link w e-mailu i zaloguj się ponownie.
- **Crashlytics**: otwórz usługę Crashlytics dla aplikacji Android. Jeżeli pojawi się kreator konfiguracji, dokończ go.

Panel akceptuje tylko ten adres. To ograniczenie w interfejsie nie zastępuje zabezpieczeń po stronie Firestore — dostęp do raportów jest również sprawdzany przez reguły bazy. Jeżeli konto administratora ma inny adres, zmień go jednocześnie w `MainViewModel.kt` i `firestore.rules`.

## 3. Reguły bezpieczeństwa Firestore

Plik `firestore.rules` w repozytorium pokazuje reguły dla dwóch kolekcji używanych przez tę funkcję:
- `filmweb_ratings`: publiczny odczyt, brak zapisu z aplikacji klienckiej;
- `problem_reports`: użytkownicy mogą tworzyć walidowane zgłoszenia, tylko potwierdzone konto administratora może je odczytać i zmieniać status.

Przed wdrożeniem przejrzyj reguły obecnie działające w Firebase Console. **Nie zastępuj ich w ciemno** — połącz reguły z tego pliku z pozostałymi regułami projektu, jeśli inne strony, funkcje lub kolekcje również korzystają z Firestore. Wklejenie całego pliku jako jedynych reguł blokuje dostęp do wszystkich kolekcji niewymienionych w tym pliku.

## 4. Powiadomienia e-mail o nowych zgłoszeniach

Aplikacja zapisuje każde zgłoszenie z odbiorcą `janczeszko12@gmail.com` oraz wiadomością tekstową. Reguły Firestore ograniczają adres odbiorcy i temat wiadomości, aby klient nie mógł skierować ich na dowolny adres.

Aby Firebase faktycznie wysyłał te wiadomości:
1. W Firebase Console otwórz **Extensions → Explore extensions** i zainstaluj oficjalne rozszerzenie **Trigger Email from Firestore** (`firestore-send-email`).
2. Jako kolekcję dokumentów e-mail ustaw dokładnie `problem_reports`.
3. Podaj działające dane serwera SMTP dostawcy poczty, adres nadawcy oraz ustawienia wymagane przez kreator rozszerzenia. Adres nadawcy musi być dozwolony przez dostawcę poczty; sama nazwa `noreply@kinostream.firebase.com` nie tworzy skrzynki ani nie zapewnia możliwości wysyłki.
4. Sprawdź, czy rozszerzenie ma uprawnienia do zapisu statusu dostarczenia oraz czy Firestore używa reguł z tego repozytorium. Najpierw połącz je z bieżącymi regułami projektu — nie zastępuj innych reguł w ciemno.
5. Wyślij testowe zgłoszenie i sprawdź skrzynkę odbiorczą oraz spam. Dokument zgłoszenia zapisuje się przed wysłaniem; status w panelu oznacza zapisanie zgłoszenia, nie gwarantuje dostarczenia e-maila.

Oficjalna dokumentacja rozszerzenia: https://firebase.google.com/docs/extensions/official/firestore-send-email. Do wysyłki wymagany jest serwer SMTP / dostawca poczty i jego dane uwierzytelniające.

## 5. Alerty Crashlytics na e-mail

Aby otrzymywać e-maile o awariach, zaloguj się do Firebase jako użytkownik projektu mający odpowiednie uprawnienia. Otwórz **Project settings → Alerts** i w karcie Crashlytics włącz alerty dla nowych awarii krytycznych (fatal), nowych błędów niekrytycznych (non-fatal) i ANR. W razie potrzeby ustaw własne preferencje alertów dla konta `janczesko12@gmail.com`. Firebase wysyła domyślnie część alertów, ale nowe awarie i błędy niekrytyczne wymagają włączenia powiadomień.

Dokumentacja: https://firebase.google.com/docs/crashlytics/alerts-default.

## 6. Jak działa raportowanie

- Użytkownik wysyła zgłoszenie przez **Ustawienia → Zgłoś problem**. Po zalogowaniu administratorowi pojawia się osobna karta **Admin** w dolnym menu; lista aktualizuje się w czasie rzeczywistym.
- Można oznaczyć zgłoszenie jako rozwiązane. Historia nie jest usuwana z panelu.
- Crashlytics automatycznie zbiera awarie aplikacji oraz ANR, jeśli Firebase jest poprawnie skonfigurowany. Kod dodaje też wersję aplikacji i informacje o urządzeniu jako klucze raportu, nie zapisuje adresu e-mail użytkownika jako identyfikatora Crashlytics.
- Błędy zgłoszeń i logowania, które da się obsłużyć, są rejestrowane jako błędy niefatalne, gdy Firebase jest dostępny.

## 7. Weryfikacja działania Crashlytics

Po zbudowaniu i zainstalowaniu APK z poprawną konfiguracją otwórz aplikację, zgłoś przykładowy błąd i sprawdź kolekcję `problem_reports` oraz panel w aplikacji. Aby sprawdzić Crashlytics, włącz logowanie debug:
```bash
adb shell setprop log.tag.FirebaseCrashlytics DEBUG
adb logcat -s FirebaseCrashlytics
```
Nie dodawaj przycisku wymuszającego awarię do wersji produkcyjnej. Raporty awarii mogą pojawić się w Firebase z opóźnieniem po ponownym uruchomieniu aplikacji.

Oficjalna instrukcja: [Firebase Crashlytics dla Androida](https://firebase.google.com/docs/crashlytics/android/get-started).
