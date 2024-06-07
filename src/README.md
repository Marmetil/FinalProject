# Поисковый движок #
## Описание ##
Данное приложение является итоговой работой исполнителя по завершении курса Java-разработчик на обучающей онлайн-платформе skillbox.ru
“Поисковый движок” представляет из себя Spring-приложение, работающее с локально установленной базой данных MySQL, имеющее простой веб-интерфейс и API,  через который им можно управлять и получать результаты поисковой выдачи по запросу.
Исполнителем реализована только backand-составляющая приложения.
Поисковая система «движка» включает в себя четыре основных сервиса:

**Сервис поиска лемм (LemmaCounter)**: отвечает за поиск и сохранение в базу данных встречающихся на страницах сайта лемм и их количества (*Ле́мма (англ. lemma) — начальная, словарная форма слова. В русском языке для существительных и прилагательных это форма именительного падежа единственного числа, для глаголов и глагольных форм - форма инфинитива*).

**Сервис индексирования страниц(IndexingService)**: отвечает за обход страниц сайта, сохранение информации (url, html код, код http-ответа) о них в базу данных. Запускает сервис поиска лемм по каждой найденной странице. Сервис позволяет запустить индексирование как всего списка сайтов, так и отдельной страницы любого из сайтов,  указанных в файле конфигурации. Так же сервис позволяет остановить индексирование и запустить его с начала.

**Сервис поиска(SearchService)**: отвечает за поиск проиндексированных страниц, на которых имеются слова поискового запроса (строка, введенная пользователем) и вывод результатов поиска  с учетом релевантности найденных страниц (*Релевантность — это согласованность запроса пользователя с информацией на странице или сайте в целом*).   Результаты включают в себя информацию о страницах, такую как URL, заголовок, сниппет с выделением искомых слов, а также метрику релевантности.

**Сервис статистики (StatisticService)**: отвечает за сбор и отображение информации об индексируемых сайтах, которая включает в себя количество страниц, количество лемм, статус индексации, возможные ошибки индексации.
## Стек используемых технологий ##
    - Spring Boot для создания веб-сервиса
    - Jsoup для парсинга HTML-страниц
    - Lombok для упрощения создания моделей и сервисов
    - Apache Lucene для  получения лемм слов
    - MySQL для хранения данных
## Запуск приложения ##
1. Установите на свой компьютер JDK и IntelliJ IDEA, если они еще не установлены.
2. Загрузите проект из Git -репозитория https://github.com/Marmetil/FinalProject/tree/main/src/main
3. Установите на свой компьютер Docker, если он  еще не установлен.
4. Установите на свой компьютер MySQL-сервер, если он еще не установлен, и создайте контейнер (с помощью Docker).
5. В конфигурационном файле приложения application.yaml измените параметры подключения к базе данных (username и password) на значения, указанные при создании контейнера MySQL.
6. Запустите приложение через  IntelliJ IDEA и откройте его через браузер по адресу: http://localhost:8080/.
## Как использовать приложение ##
После того, как Вы запустили приложение и открыли его через браузер, войдите на вкладку *MANAGEMENT* и нажмите на кнопку *START INDEXING*. В этом случае запустится индексирование всех сайтов, указанных в конфигурационно файле.
Так же Вы можете ввести адрес любой из страниц сайтов, перечисленных в файле конфигурации и запустить индексацию этой страницы кнопкой *ADD/UPDATE*
![MANAGEMENT.png](..%2F..%2F..%2F..%2FPictures%2FScreenshots%2FMANAGEMENT.png)
Псоле завершения индексации или для того, чтоб остановить индексацию обратитесь к кнопке *STOP INDEXING*
![STOP.png](..%2F..%2F..%2F..%2FPictures%2FScreenshots%2FSTOP.png)
Теперь на вкладке *DASHBOARD* доступна статистика
![Статистика.png](..%2F..%2F..%2F..%2FPictures%2FScreenshots%2F%D1%F2%E0%F2%E8%F1%F2%E8%EA%E0.png)

А на вкладке *SEARCH* доступен выбор проиндексированного сайта и поиск слов на страницах этого сайта
![Статистика.png](..%2F..%2F..%2F..%2FPictures%2FScreenshots%2F%D1%F2%E0%F2%E8%F1%F2%E8%EA%E0.png)