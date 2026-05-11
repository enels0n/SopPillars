# SopPillars

`SopPillars` - плагин для Paper с PvP-режимом в стиле Lucky Pillars:
игроки заходят в арену, стартуют в клетках, получают случайный лут по таймеру,
сражаются командами, побеждает последняя живая команда.

Плагин рассчитан на рабочий прод-сервер: удобная админка (wizard + меню),
интеграция с пати, косметика клеток, PlaceholderAPI, снапшоты арены.

## Основные возможности

- Полный цикл арены: лобби ожидания -> обратный отсчет -> клетки -> бой -> празднование победы.
- Пошаговый wizard настройки арены с предметами управления и прогрессом в actionbar.
- Поддержка пати через `SopParty`, при этом соло-игроки без пати заходят корректно.
- Меню настроек конкретной арены (`/pillars settings`) с сохранением на лету.
- Улучшенный UX режима лута:
  - явный переключатель `WHITELIST/BLACKLIST`,
  - инвентарь-редакторы списков для арены,
  - инвентарь-редакторы глобальных списков (`/pillars settings global`).
- Система наборов (kits), включая выбор `No kit` (игра без стартового набора).
- Косметические клетки из `.schem` с выбором по игроку и проверкой permissions.
- Вставка клеток через WorldEdit с поворотом по yaw у точки спавна.
- PlaceholderAPI placeholder'ы для таба, scoreboards и админ-виджетов.
- Снапшоты/бэкапы арены для аккуратного отката после матчей.

## Требования

- Java 8+ (проект собирается с `target 8`).
- Сервер Paper/Spigot (`api-version` в `plugin.yml`: `1.16`).
- Опциональные soft-dependencies:
  - `SopParty` (логика пати),
  - `PlaceholderAPI` (placeholder'ы),
  - `SopLib` (если используется в вашей сборке).
- Для работы `.schem` клеток нужен WorldEdit на сервере.

## Сборка

Из корня репозитория:

```bash
mvn -f "SopPillars/pom.xml" -DskipTests package
```

Готовый JAR:

- `SopPillars/target/SopPillars.jar`

## Установка

1. Скопируйте `SopPillars.jar` в `plugins/`.
2. Один раз запустите сервер (создадутся конфиги/папки).
3. (Опционально) Поставьте `SopParty`, `PlaceholderAPI`, WorldEdit.
4. Перезапустите сервер.

## Быстрый старт

1. Установите глобальный спавн:
   - `/pillars setglobalspawn`
2. Создайте арену:
   - `/pillars create <name> <mode> <teams> <playersPerTeam>`
3. Пройдите wizard настройки (см. ниже).
4. Сохраните арену:
   - `/pillars save`
5. Проверьте вход:
   - `/pillars join <name>`

## Wizard настройки арены

После `/pillars create` или `/pillars edit` wizard запускается автоматически.

Управление в хотбаре:

- Слот 1: установить текущую точку (`Set`)
- Слот 2: шаг назад (`Back`)
- Слот 3: пропустить (`Skip`) - только для опционального `setendspawn`

Порядок шагов:

1. `pos1` (gameplay area)
2. `pos2` (gameplay area)
3. `lobbypos1`
4. `lobbypos2`
5. `setspectator`
6. `setlobbyspawn`
7. `setendspawn` (optional)
8. `setspawn` for every team/player slot in order

Прогресс отображается в actionbar (`Step X/8`), а на `setspawn` еще и как прогресс по слотам.

## Команды

Игрок:

- `/pillars list`
- `/pillars join <arena>`
- `/pillars random [mode,mode,...]`
- `/pillars leave`
- `/pillars kits`
- `/pillars cosmetics`
- `/pillars stats`

Админ:

- `/pillars create <name> <mode> <teams> <playersPerTeam>`
- `/pillars edit <arena>`
- `/pillars save`
- `/pillars cancel <arena>`
- `/pillars delete <arena>`
- `/pillars settings` (edited arena settings)
- `/pillars settings global` (global loot defaults)
- `/pillars setglobalspawn`
- `/pillars tp <arena>`
- `/pillars kitadd <kit>`
- `/pillars kitremove <kit> <slot|all>`
- `/pillars reload`

## Права

- `soppillars.play` - default `true`
- `soppillars.stats` - default `true`
- `soppillars.admin` - default `op`
- `soppillars.cage.<id>` - опциональный доступ к конкретной клетке

## Наборы (Kits)

- Игрок выбирает набор через `/pillars kits`.
- Пункт `No kit` позволяет играть без стартового преимущества.
- Админ может редактировать наборы без правки YAML:
  - предмет в руке -> `/pillars kitadd <kit>`
  - удалить по индексу -> `/pillars kitremove <kit> <slot>`
  - очистить набор -> `/pillars kitremove <kit> all`

Файлы наборов лежат в `plugins/SopPillars/kits/`.

## Система лута

В меню арены (`/pillars settings`):

- интервал выдачи лута,
- явный режим (`WHITELIST` или `BLACKLIST`),
- редакторы списков арены:
  - `Edit arena whitelist`
  - `Edit arena blacklist`

В глобальном меню (`/pillars settings global`):

- режим по умолчанию,
- редактор global whitelist,
- редактор global blacklist.

Как работают редакторы:

- открыли инвентарь редактора,
- положили/убрали предметы,
- закрыли инвентарь -> список сохранен.

## Косметика и клетки

- Поместите `.schem` клетки в `plugins/SopPillars/cages/`.
- `default.schem` автоматически восстанавливается из ресурсов, если удален.
- Игрок выбирает клетку через `/pillars cosmetics` (с учетом permission).
- На старте матча плагин вставляет выбранную схему через WorldEdit.
- Поворот определяется по yaw у точки `setspawn` (ближайшая сторона света).

## PlaceholderAPI

Префикс: `%soppillars_<key>%`

Доступные ключи:

- `in_game`
- `game_status`
- `arena`
- `mode`
- `team`
- `alive`
- `countdown`
- `alive_players`
- `players_total`
- `min_players`
- `min_filled_teams`
- `stats_games`
- `stats_wins`
- `stats_kills`
- `stats_deaths`

Примеры:

- `%soppillars_game_status%`
- `%soppillars_min_players%`
- `%soppillars_stats_wins%`

## Конфигурация

Основной файл: `plugins/SopPillars/config.yml`

Важные параметры:

- `settings.global-spawn` - fallback точка телепорта.
- `settings.default-min-players` и `settings.default-min-filled-teams`.
- `settings.default-loot-blacklist-mode`.
- `settings.default-loot-whitelist` / `settings.default-loot-blacklist`.

Файлы арен находятся в `plugins/SopPillars/arenas/`.

## Частые проблемы

- "No kits are available":
  - проверьте права на кит и YAML-файлы в `kits/`.
- Не ставится кастомная клетка:
  - проверьте наличие `.schem` в `cages/`,
  - убедитесь, что WorldEdit установлен и загрузился.
- Placeholder пустой:
  - проверьте, что PlaceholderAPI установлен и expansion зарегистрировался при старте.
- После сборки нет jar:
  - используйте Maven `package`, а не только `compile`.

## Для разработки

- Документы по требованиям:
  - [SPEC.md](./SPEC.md)
  - [MVP.md](./MVP.md)
- Текущая версия модуля: `0.0.1-SNAPSHOT`.
