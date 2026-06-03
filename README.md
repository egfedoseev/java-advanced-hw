# Курс Java Advanced (ITMO University)

Репозиторий содержит решения домашних заданий по продвинутому курсу программирования на Java.

Преподаватель курса: Георгий Корнеев.

## Содержание

### 1. [Домашнее задание 1. Обход файлов (RecursiveWalk)](#homework-1)
### 2. [Домашнее задание 2. Множество на массиве (ArraySet)](#homework-2)
### 3. [Домашнее задание 3. Студенты (StudentDB)](#homework-3)
### 4. [Домашнее задание 4. Реализация потоков (Streams)](#homework-4)
### 5. [Домашнее задание 5. Implementor](#homework-5)
### 6. [Домашнее задание 6. Jar Implementor](#homework-6)
### 7. [Домашнее задание 7. Javadoc](#homework-7)
### 8. [Домашнее задание 8. Итеративный параллелизм (IterativeParallelism)](#homework-8)
### 9. [Домашнее задание 9. Параллельный запуск (ParallelMapper)](#homework-9)
### 10. [Домашнее задание 10. Web Crawler](#homework-10)
### 11. [Домашнее задание 11. HelloUDP](#homework-11)
### 12. [Домашнее задание 12. Физические лица (RMI Bank)](#homework-12)
### 13. [Домашнее задание 13. Физические лица (тесты)](#homework-13) `TBA`
### 14. [Домашнее задание 14. HelloNonblockingUDP](#homework-14) `TBA`
### 15. [Домашнее задание 15. Статистика текста](#homework-15) `TBA`

---

## Описание домашних заданий

### <a name="homework-1"></a>Домашнее задание 1. Обход файлов
* **Решение:** пакет [`info.kgeorgiy.ja.fedoseev.walk`](java-solutions/info/kgeorgiy/ja/fedoseev/walk/)
* **Основной класс:** [`RecursiveWalk.java`](java-solutions/info/kgeorgiy/ja/fedoseev/walk/RecursiveWalk.java)
* **Описание:** Разработка класса для рекурсивного обхода директорий и файлов с целью подсчета их хэш-сумм по алгоритму FNV (32-бит). Программа корректно обрабатывает ошибки ввода-вывода (выводя нули вместо хэша) и эффективно работает с файлами, превышающими объем оперативной памяти.

### <a name="homework-2"></a>Домашнее задание 2. Множество на массиве
* **Решение:** [`ArraySet.java`](java-solutions/info/kgeorgiy/ja/fedoseev/arrayset/ArraySet.java)
* **Описание:** Реализация неизменяемого упорядоченного множества, оптимизированного по памяти и асимптотике, на основе стандартных массивов Java. Реализует интерфейс `NavigableSet`.

### <a name="homework-3"></a>Домашнее задание 3. Студенты
* **Решение:** [`StudentDB.java`](java-solutions/info/kgeorgiy/ja/fedoseev/student/StudentDB.java)
* **Описание:** Класс для фильтрации, поиска и сортировки базы данных студентов. Каждый метод реализован строго с помощью одного оператора (одного длинного выражения) с активным применением Java Stream API и лямбда-выражений.

### <a name="homework-4"></a>Домашнее задание 4. Реализация потоков
* **Решение:** пакет [`info.kgeorgiy.ja.fedoseev.streams`](java-solutions/info/kgeorgiy/ja/fedoseev/streams/)
* **Основной класс:** [`Streams.java`](java-solutions/info/kgeorgiy/ja/fedoseev/streams/Streams.java)
* **Описание:** Сложный вариант (`HardStreams`). Реализация кастомных сплитераторов для двоичных, n-арных и вложенных деревьев, а также специализированных коллекторов (поиск оконных элементов, префиксов/суффиксов строк и уникальных элементов по предикату).

### <a name="homework-5"></a>Домашнее задание 5. Implementor
* **Решение:** [`Implementor.java`](java-solutions/info/kgeorgiy/ja/fedoseev/implementor/Implementor.java)
* **Описание:** Утилита, генерирующая валидный исходный java-код (`*Impl.java`) для переданного интерфейса или класса с помощью рефлексии (Reflection API). Сгенерированные классы компилируются без ошибок, не являются абстрактными и возвращают дефолтные значения.

### <a name="homework-6"></a>Домашнее задание 6. Jar Implementor
* **Решение:** кастомный запуск в [`Implementor.java`](java-solutions/info/kgeorgiy/ja/fedoseev/implementor/Implementor.java) и скрипт сборки [`pack_jar.sh`](scripts/pack_jar.sh)
* **Описание:** Расширение функционала `Implementor`. Добавлена поддержка аргумента `-jar` для автоматической компиляции сгенерированного кода силами JavaCompiler API и его последующей упаковки в исполняемый `.jar`-архив.

### <a name="homework-7"></a>Домашнее задание 7. Javadoc
* **Решение:** скрипт генерации [`generate_javadoc.sh`](scripts/generate_javadoc.sh) и собранные файлы в [`scripts/javadoc/`](scripts/javadoc/)
* **Описание:** Полное документирование пакета `implementor` (включая private-члены) в соответствии со стандартами Javadoc. Весь код компилируется без предупреждений (`warnings`) со ссылками на стандартную библиотеку JDK.

### <a name="homework-8"></a>Домашнее задание 8. Итеративный параллелизм
* **Решение:** [`IterativeParallelism.java`](java-solutions/info/kgeorgiy/ja/fedoseev/iterative/IterativeParallelism.java)
* **Описание:** Класс для многопоточной параллельной обработки списков (фильтрация, маппинг, поиск экстремумов и индексов) на чистых потоках `Thread` без использования высокоуровневых Concurrency Utilities и Parallel Streams.

### <a name="homework-9"></a>Домашнее задание 9. Параллельный запуск
* **Решение:** [`ParallelMapperImpl.java`](java-solutions/info/kgeorgiy/ja/fedoseev/iterative/ParallelMapperImpl.java)
* **Описание:** Реализация собственного интерфейса `ParallelMapper` (кастомный пул потоков), управляющего фиксированным числом рабочих воркеров. Задачи распределяются через внутреннюю блокирующую очередь без использования активного ожидания. Интегрировано с решением `IterativeParallelism`.

### <a name="homework-10"></a>Домашнее задание 10. Web Crawler
* **Решение:** [`WebCrawler.java`](java-solutions/info/kgeorgiy/ja/fedoseev/crawler/WebCrawler.java)
* **Описание:** Многопоточный и потокобезопасный поисковый робот для рекурсивного скачивания веб-страниц и извлечения ссылок на заданную глубину. Написан с использованием `ExecutorService` и потокобезопасных примитивов для строгого соблюдения лимитов на одновременные загрузки (в том числе ограничений на запросы к одному хосту — `perHost`).

### <a name="homework-11"></a>Домашнее задание 11. HelloUDP
* **Решение:** пакет [`info.kgeorgiy.ja.fedoseev.hello`](java-solutions/info/kgeorgiy/ja/fedoseev/hello/)
* **Классы:** [`HelloUDPClient.java`](java-solutions/info/kgeorgiy/ja/fedoseev/hello/HelloUDPClient.java) / [`HelloUDPServer.java`](java-solutions/info/kgeorgiy/ja/fedoseev/hello/HelloUDPServer.java)
* **Описание:** Реализация сетевого взаимодействия по UDP-протоколу. Многопоточный клиент рассылает запросы по заданной схеме и обрабатывает таймауты повторной отправкой, а сервер параллельно принимает пакеты и формирует ответы с расчетом на высокую нагрузку.

### <a name="homework-12"></a>Домашнее задание 12. Физические лица
* **Решение:** пакет [`info.kgeorgiy.ja.fedoseev.bank`](java-solutions/info/kgeorgiy/ja/fedoseev/bank/)
* **Описание:** Распределенное банковское приложение на базе Java RMI (Remote Method Invocation). Добавлена сложная доменная модель физических лиц (`Person`) с поддержкой удаленных сущностей (`RemotePerson`) и сериализуемых локальных слепков данных (`LocalPerson`), сохраняющих состояние счетов на момент создания. Реализован строгий контроль баланса (неотрицательные суммы).

### <a name="homework-13"></a>Домашнее задание 13. Физические лица (тесты)
* `TBA` (В процессе реализации)

### <a name="homework-14"></a>Домашнее задание 14. HelloNonblockingUDP
* `TBA` (В процессе реализации)

### <a name="homework-15"></a>Домашнее задание 15. Статистика текста
* `TBA` (В процессе реализации)