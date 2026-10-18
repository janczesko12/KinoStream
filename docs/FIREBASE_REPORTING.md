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

## 2. Włączenie Crashlytics

W tym samym projekcie Firebase:
- **Crashlytics**: otwórz usługę Crashlytics dla aplikacji Android. Jeżeli pojawi się kreator konfiguracji, dokończ go.
- **Project settings → Alerts**: włącz powiadomienia e-mail o nowych awariach i ANR dla konta, które ma dostęp do projektu Firebase.

Aplikacja nie zawiera już panelu administratora ani logowania Firebase Authentication. E-maile o zgłoszeniach są wysyłane przez Cloudflare Worker i Resend, niezależnie od panelu w aplikacji. Samo usunięcie panelu nie zmienia zasad zapisu zgłoszeń ani działania Crashlytics.

## 3. Reguły bezpieczeństwa Firestore

Plik `firestore.rules` w repozytorium pokazuje reguły dla dwóch kolekcji używanych przez tę funkcję:
- `filmweb_ratings`: publiczny odczyt, brak zapisu z aplikacji klienckiej;
- `problem_reports`: zgłoszenia są zapisywane w Firestore; odczyt i zmiana statusu pozostają ograniczone do potwierdzonego konta administratora. Aplikacja nie ma już wbudowanego panelu administratora.

Przed wdrożeniem przejrzyj reguły obecnie działające w Firebase Console. **Nie zastępuj ich w ciemno** — połącz reguły z tego pliku z pozostałymi regułami projektu, jeśli inne strony, funkcje lub kolekcje również korzystają z Firestore. Wklejenie całego pliku jako jedynych reguł blokuje dostęp do wszystkich kolekcji niewymienionych w tym pliku.

## 4. Powiadomienia e-mail o nowych zgłoszeniach

Firebase Extensions nie są do tego potrzebne. KinoStream zapisuje zgłoszenie w Firestore, a następnie wywołuje osobny Cloudflare Worker. Worker korzysta z Resend API i wysyła e-mail z `powiadomienia@novydev.pl` do `janczesko12@gmail.com`. Nie wymaga to instalowania rozszerzenia Firebase ani zmiany projektu na Blaze.

Instrukcja wdrożenia znajduje się w [cloudflare/kinostream-report-email/README.md](../cloudflare/kinostream-report-email/README.md). W skrócie:
1. Dodaj i zweryfikuj domenę `novydev.pl` w Resend. Skopiuj **dokładne** rekordy DNS podane przez Resend do Cloudflare DNS i poczekaj, aż Resend pokaże status Verified.
2. Utwórz Worker `kinostream-report-email` z katalogu `cloudflare/kinostream-report-email`.
3. Dodaj `RESEND_API_KEY` w Cloudflare jako sekret Worker — nigdy nie umieszczaj go w APK, kodzie repozytorium ani zwykłej zmiennej.
4. Dodaj w ustawieniach GitHub repozytorium zmienną Actions `REPORT_EMAIL_WORKER_URL`, której wartością jest pełny adres endpointu `https://<worker-host>/api/report`. Użyj hosta przypisanego przez Cloudflare, nie przykładowego placeholdera.
5. Uruchom nową kompilację KinoStream. Aplikacja wyśle e-mail dopiero po poprawnym zapisaniu zgłoszenia w Firestore.

Worker ogranicza liczbę żądań do 3 na minutę z jednego adresu IP, waliduje pola i używa stałego nadawcy/odbiorcy. Ponieważ punkt końcowy aplikacji jest publiczny, limit redukuje nadużycia, ale nie zastępuje silnej weryfikacji tożsamości. Przy większej liczbie użytkowników należy dodać weryfikację Firebase App Check lub inne poświadczenie weryfikowane po stronie serwera.

Resend musi potwierdzić domenę `novydev.pl`, zanim nadawca `powiadomienia@novydev.pl` będzie mógł zostać użyty. Nie trzeba tworzyć osobnej skrzynki odbiorczej dla nadawcy, ale domena musi być zweryfikowana i odpowiednio skonfigurowana przez rekordy DNS.

## 5. Alerty Crashlytics na e-mail

Aby otrzymywać e-maile o awariach, zaloguj się do Firebase jako użytkownik projektu mający odpowiednie uprawnienia. Otwórz **Project settings → Alerts** i w karcie Crashlytics włącz alerty dla nowych awarii krytycznych (fatal), nowych błędów niekrytycznych (non-fatal) i ANR. W razie potrzeby ustaw własne preferencje alertów dla konta `janczesko12@gmail.com`. Firebase wysyła domyślnie część alertów, ale nowe awarie i błędy niekrytyczne wymagają włączenia powiadomień.

Dokumentacja: https://firebase.google.com/docs/crashlytics/alerts-default.

## 6. Jak działa raportowanie

- Użytkownik wysyła zgłoszenie przez **Ustawienia → Zgłoś problem**. Zgłoszenie jest zapisywane w Firestore, a Worker wysyła powiadomienie e-mail na `janczesko12@gmail.com`, jeśli Cloudflare i Resend są poprawnie skonfigurowane.
- Wbudowany panel administratora został usunięty z aplikacji; zgłoszenia nadal są przechowywane w Firestore.
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
