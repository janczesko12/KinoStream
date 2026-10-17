# Podpisywanie i udostępnianie KinoStream jako APK

KinoStream możesz udostępniać na własnej stronie bez publikowania aplikacji w Google Play. GitHub Actions może sprawdzać kod i budować APK. Po skonfigurowaniu stałego klucza podpisującego tworzy też produkcyjny APK do pobrania; artefakt trzeba pobrać z GitHub Actions i przesłać na własny hosting.

## 1. Utwórz własny klucz podpisujący

Na komputerze z JDK uruchom PowerShell:

```powershell
keytool -genkeypair -v `
  -keystore "$env:USERPROFILE\kinostream-upload.jks" `
  -storetype JKS `
  -alias upload `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000 `
  -dname "CN=KinoStream,O=KinoStream,C=PL"
```

Ustaw hasło magazynu i zapamiętaj je. Zrób zaszyfrowaną kopię pliku `kinostream-upload.jks` w bezpiecznym miejscu. Nigdy nie dodawaj tego pliku ani haseł do repozytorium. Ten klucz jest potrzebny do podpisywania kolejnych wersji APK; jego utrata może uniemożliwić instalację aktualizacji nad poprzednią wersją.

## 2. Dodaj sekrety GitHub

Otwórz **Settings → Secrets and variables → Actions → New repository secret** w repozytorium i dodaj:

| Nazwa | Wartość |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | Zawartość pliku keystore zakodowana jako Base64 |
| `STORE_PASSWORD` | Hasło magazynu kluczy |
| `KEY_PASSWORD` | Hasło klucza o aliasie `upload` |

Aby skopiować Base64 do schowka w PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\kinostream-upload.jks")) | Set-Clipboard
```

Wklej wynik jako wartość `SIGNING_KEYSTORE_BASE64`. Pozostałe dwa sekrety wypełnij swoimi hasłami. Nie używaj klucza API — to nie są klucze API.

## 3. Pobierz APK i wgraj na stronę

Po udanym przebiegu workflow wejdź w **GitHub → Actions → najnowszy przebieg → Artifacts** i pobierz `kinostream-release-apk`. W archiwum ZIP będzie `app-release.apk`. Po sprawdzeniu pliku możesz przesłać go na własny hosting i podlinkować na stronie.

Jeśli sekrety nie są ustawione, workflow nadal buduje i testuje debug APK, ale nie tworzy produkcyjnego artefaktu podpisanego stałym kluczem.

## Ważne przy pierwszej wersji

Jeśli wcześniejszy APK został zainstalowany z AI Studio lub był podpisany innym kluczem, nowy APK może nie zainstalować się jako aktualizacja. Wtedy może być konieczne odinstalowanie starej wersji — lokalne dane aplikacji zostaną usunięte. Jeśli użytkownicy już mają zainstalowany APK, najpierw ustal, jakim certyfikatem jest on podpisany; nie zmieniaj klucza w ciemno.

## Testy CI

Workflow buduje debug APK i uruchamia `testDebugUnitTest`. Z osobnym, trwałym kluczem może również zbudować `app-release.apk` i udostępnić go jako artefakt GitHub Actions na 14 dni. Nie publikuje automatycznie pliku w Google Play ani nie wdraża go na Twoją stronę.
