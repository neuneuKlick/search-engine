# Search Engine
## Стэк технологий
+ Java 17
+ Spring Framework
+ Spring Boot
+ Spring Data JPA
+ Spring Web
+ Maven
+ Hibernate
+ MySQL8
## Описание
Локальное приложение, поисковая система, которая находит информацию на интересующих 
нас сайтах. Найденная информация предоставляется в виде списка ссылок с найденным 
текстом. Перейдя по ссылке, попадаем на страницу, где находится нужный текст.
![Использование поисковой системы](src/main/resources/images/img_5.jpg)
## Инструкция по локальному запуску

+ Создайте базу данных в MySQL80 с именем search_engine
+ Установите JDK Java 17
+ В приложении в файле application.yaml укажите параметры подключения

## Инструкцию по работе с программой
### Management
![Management_ADD](src/main/resources/images/img_2.jpg)
ADD/UPDATE - Здесь мы указываем на каком сайте нужно выполнять поиск. Добавляя или 
обновляя страницу (см. ниже):
![Management_START](src/main/resources/images/img_8.jpg)
START INDEXING - Здесь мы можем запустить обработку(индексацию) сайтов, а также 
повторным нажатием прервать данный процесс. 
### Dashboard
![Dashboard](src/main/resources/images/img_1.jpg)
Здесь выводится общая статистика по индексации. А именно, сколько сайтов было 
проиндексировано, страниц и лемм(слов). Ниже описание отдельного сайта, с его статусом.
### Search
![Search](src/main/resources/images/img_3.jpg)
Здесь мы можем выбрать поиск по всем сайтам или одному. Ниже пишем, что нас интересует найти.
Если слово не найдено, оповещаем об этом. (см. ниже):
![Search_not_found_word](src/main/resources/images/img_4.jpg)
Если слово или несколько слов нашлось, то выводится результат(см. ниже):
![Использование поисковой системы](src/main/resources/images/img_5.jpg)
![Search_found_word](src/main/resources/images/img_6.jpg)
Результат поиска на отдельной странице:
![Search_found_word_page](src/main/resources/images/img_7.jpg)
Результат поиска при заданном пустом запросе:
![Search_null](src/main/resources/images/img_9.jpg)
