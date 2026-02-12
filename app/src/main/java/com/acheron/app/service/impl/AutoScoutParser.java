package com.acheron.app.service.impl;

import com.acheron.app.dto.request.ScrapingRequest;
import com.acheron.app.entity.Car;
import com.acheron.app.service.CarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class  AutoScoutParser {

    private final CarService carService;
    private static final int PAGE_SIZE = 100; // Autoscout зазвичай показує 20 авто на сторінку

    // Метод запускається асинхронно, щоб не блокувати відповідь контролера
    @Async
    public void startScraping(ScrapingRequest request) {
        String baseUrl = request.url();
        baseUrl = baseUrl+"&"+PAGE_SIZE;
        log.info("Starting scraping for URL: {}", baseUrl);

        try {
            // 1. Отримуємо першу сторінку, щоб дізнатися загальну кількість авто
            Document firstPageDoc = Jsoup.connect(baseUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get();

            // 2. Парсимо загальну кількість (з вашого скріншоту)
            int totalListings = extractTotalCount(firstPageDoc);
            int totalPages = (int) Math.ceil((double) totalListings / PAGE_SIZE);

            log.info("Found {} listings, approx {} pages to scrape.", totalListings, totalPages);

            // 3. Проходимо по сторінках
            // Autoscout використовує параметр &page=1, &page=2 і т.д.
            for (int page = 1; page <= totalPages; page++) {
                String pageUrl = baseUrl + "?page=" + page;

                log.info("Scraping page {} of {}: {}", page, totalPages, pageUrl);

                try {
                    scrapeAndSavePage(pageUrl);
                } catch (Exception e) {
                    log.error("Error scraping page {}: {}", page, e.getMessage());
                }

                // 4. Пауза 20 секунд між запитами (Anti-bot protection)
                if (page < totalPages) {
                    log.info("Sleeping for 20 seconds...");
                    Thread.sleep(20000);
                }
            }

            log.info("Scraping finished for {}", baseUrl);

        } catch (Exception e) {
            log.error("Fatal error during scraping initialization", e);
        }
    }

    private void scrapeAndSavePage(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get();

        Set<Car> carsFound = parseCarsFromHtml(doc);

        if (!carsFound.isEmpty()) {
            carService.saveAll(carsFound);
        }
    }

    // Логіка витягування цифри з заголовка (Ваш скріншот)
    private int extractTotalCount(Document doc) {
        // Селектор з вашого скріншоту: h1 data-testid="list-header-title"
        String headerText = doc.select("h1[data-testid='list-header-title']").text();
        // Приклад тексту: "4,287 Offers for BMW 3 Series (all)"

        // Видаляємо все, крім цифр
        String numberOnly = headerText.replaceAll("[^0-9]", "");

        // Якщо в рядку є інші цифри (наприклад "BMW 3"), треба брати першу групу.
        // Більш надійний regex для початку рядка:
        Pattern pattern = Pattern.compile("^([0-9,.]+)");
        Matcher matcher = pattern.matcher(headerText);
        if (matcher.find()) {
            String countStr = matcher.group(1).replaceAll("[,.]", ""); // Видаляємо коми тисяч
            return Integer.parseInt(countStr);
        }

        // Fallback, якщо regex не спрацював, пробуємо просто всі цифри (може бути помилково якщо в назві моделі є цифри)
        return numberOnly.isEmpty() ? 0 : Integer.parseInt(numberOnly);
    }

    // Тут треба реалізувати парсинг конкретних карток авто
    private Set<Car> parseCarsFromHtml(Document doc) {
        Set<Car> cars = new HashSet<>();

        // 1. Селектор на основі вашого HTML.
        // Там використовується data-testid="decluttered-list-item"
        Elements carElements = doc.select("article[data-testid='decluttered-list-item']");

        for (Element el : carElements) {
            try {
                Car car = new Car();

                // --- 1. Основні дані з атрибутів (найнадійніший спосіб) ---

                // ID (Guid) - "f6e05e5f-..."
                String externalId = el.attr("data-guid");
                car.setExternalId(externalId);

                // Ціна - "61499" (вже без валюти)
                String priceStr = el.attr("data-price");
                if (isValidNumber(priceStr)) {
                    car.setPrice(Integer.parseInt(priceStr));
                }

                // Пробіг - "54000"
                String mileageStr = el.attr("data-mileage");
                if (isValidNumber(mileageStr)) {
                    car.setMileage(Integer.parseInt(mileageStr));
                }

                // Марка та модель
                car.setMake(el.attr("data-make")); // "bmw"
                car.setModel(el.attr("data-model")); // "840"

                // Рік (First Registration) - "12-2021"
                String regDate = el.attr("data-first-registration");
                if (regDate != null && regDate.length() >= 4) {
                    // Беремо останні 4 символи як рік
                    String yearStr = regDate.substring(regDate.length() - 4);
                    if (isValidNumber(yearStr)) {
                        car.setFirstRegistration(yearStr);
                    }
                }

                // --- 2. Додаткові дані з тексту (для опису VectorStore) ---

                // Посилання
                Element linkEl = el.selectFirst("a.ListItemTitle_anchor__4TrfR"); // Клас з вашого HTML
                if (linkEl != null) {
                    String relativeUrl = linkEl.attr("href");
                    car.setUrl("https://www.autoscout24.com" + relativeUrl);
                }

                // Опис / Версія (напр. "iAS M-Sport Cabrio /Laser/...")
                String version = "";
                Element titleEl = el.selectFirst("span.ListItemTitle_subtitle__V_ao6");
                if (titleEl != null) {
                    version = titleEl.text().trim();
                }
                // Зберігаємо повну назву для семантичного пошуку
                car.setDescription(car.getMake() + " " + car.getModel() + " " + version);

                // --- 3. Фото (Опціонально) ---
                Element imgEl = el.selectFirst("img[data-testid='decluttered-list-item-image']");
//                if (imgEl != null) {
//                    car.set(imgEl.attr("src"));
//                }

                cars.add(car);

            } catch (Exception e) {
                log.warn("Failed to parse individual car element: {}", e.getMessage());
                // Не зупиняємо весь процес через одну помилку
            }
        }

        log.info("Parsed {} cars from HTML", cars.size());
        return cars;
    }

    // Допоміжний метод для перевірки, чи рядок є числом
    private boolean isValidNumber(String str) {
        return str != null && !str.isEmpty() && str.matches("\\d+");
    }
}