# Tworzenie stabilnego wydania Android

Workflow GitHub Actions buduje APK debug wyłącznie do testów i nie publikuje go jako aktualizacji aplikacji. Produkcyjne APK jest publikowane dopiero po skonfigurowaniu stałego klucza podpisującego.

## Jednorazowa konfiguracja

1. Utwórz bezpieczny klucz podpisujący z aliasem `upload` (lub użyj już istniejącego klucza, jeśli to on ma podpisywać przyszłe wydania). Zachowaj plik keystore i hasła w bezpiecznym miejscu. Nie dodawaj keystore do repozytorium.
2. Dodaj poniższe sekrety w GitHub: **Settings → Secrets and variables → Actions → New repository secret**:
   - `SIGNING_KEYSTORE_BASE64` — cały plik keystore zakodowany jako Base64.
   - `STORE_PASSWORD` — hasło keystore.
   - `KEY_PASSWORD` — hasło klucza o aliasie `upload`.
3. Uruchom workflow ponownie albo wypchnij commit do gałęzi `main`. Jeżeli sekrety są poprawne, workflow zbuduje `assembleRelease` i opublikuje podpisane APK.

### Kodowanie keystore w PowerShell

Uruchom poniższe polecenie na komputerze, na którym bezpiecznie przechowujesz plik. Skopiowany wynik wklej jako wartość sekretu `SIGNING_KEYSTORE_BASE64`:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("my-upload-key.jks")) | Set-Clipboard
```

Keystore musi zawierać alias `upload`, zgodny z `app/build.gradle.kts`. Nie zapisuj hasła ani zakodowanego klucza w pliku śledzonym przez Git.

## Ważne przy pierwszym podpisanym wydaniu

Wcześniejsze APK z automatycznych buildów debug mogły być podpisane innym, tymczasowym kluczem. Android może odmówić zainstalowania podpisanego wydania jako aktualizacji takiej instalacji. Przed przejściem na podpisane wydanie może być konieczne odinstalowanie starej wersji; lokalne dane aplikacji mogą wtedy zostać usunięte. Po przejściu na stały klucz nie zmieniaj go pomiędzy wydaniami.

## Testy

Każdy push na `main` i gałęzie `fix/**` buduje debug APK, uruchamia `testDebugUnitTest` i udostępnia debug APK jako tymczasowy artefakt Actions. Ten artefakt nie jest podpisanym wydaniem produkcyjnym.
