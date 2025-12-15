package aze.edu.itbrains.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Scraper {

    public static class Product {
        public String name;
        public String price;
        public String url;
        public String image;
        public String description;
        public List<String> colors;
        public List<String> sizes;

        public Product(String name, String price, String url, String image,
                       String description, List<String> colors, List<String> sizes) {
            this.name = name;
            this.price = price;
            this.url = url;
            this.image = image;
            this.description = description;
            this.colors = colors;
            this.sizes = sizes;
        }
    }

    public static List<Product> scrapeProductsFromPage(String url) throws IOException {
        List<Product> products = new ArrayList<>();

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(10000)
                .get();

        Elements items = doc.select("li.product");

        for (Element item : items) {
            String name = item.select("h2").text();
            String price = item.select("span.amount").first().text();
            String productUrl = item.select("a").attr("href");
            String image = item.select("img").attr("src");

            products.add(new Product(name, price, productUrl, image,
                    "", new ArrayList<>(), new ArrayList<>()));
        }
        return products;
    }

    public static List<Product> scrapeAllProducts() throws IOException {
        List<Product> allProducts = new ArrayList<>();
        String baseUrl = "https://www.scrapingcourse.com/ecommerce/page/";
        int totalPages = 12;

        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<Future<List<Product>>> futures = new ArrayList<>();


        for (int i = 1; i <= totalPages; i++) {
            String pageUrl = baseUrl + i + "/";
            futures.add(executor.submit(() -> scrapeProductsFromPage(pageUrl)));
        }

        for (Future<List<Product>> f : futures) {
            try {
                allProducts.addAll(f.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
        return allProducts;
    }

    public static Product scrapeProductFromPage(String url) throws IOException {
        Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10000).get();

        String name = doc.select("h1.product_title").text();
        String price = doc.select("p.price span.amount").first().text();
        String image = doc.select(".woocommerce-product-gallery__image img").attr("src");
        String description = doc.select("div#tab-description").text();

        List<String> sizes = new ArrayList<>();
        for (Element opt : doc.select("select#size option")) {
            if (!opt.text().toLowerCase().contains("choose")) {
                sizes.add(opt.text());
            }
        }

        List<String> colors = new ArrayList<>();
        for (Element opt : doc.select("select#color option")) {
            if (!opt.text().toLowerCase().contains("choose")) {
                colors.add(opt.text());
            }
        }
        return new Product(name, price, url, image, description, colors, sizes);
    }
}
