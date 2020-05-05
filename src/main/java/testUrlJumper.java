import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class testUrlJumper extends moduleDefault {

    protected long startTime;
    protected long totalTime;

    protected List<atwebSite> siteList;
    protected List<atwebUrl> urlList;
    protected String currentStartingAddress;

    @Override
    public void Init(atwebInterface webInterface) {
        this.webInterface = webInterface;

        // get class methods, save them in hash map
        Method[] m_list = testUrlJumper.class.getDeclaredMethods();
        this.methodMap = new HashMap<>();

        System.out.print("Init module class \"" + this.name + "\" with methods");

        for(Method m : m_list) {
            m.setAccessible(true);
            methodMap.put(m.getName(),m);

            System.out.print("\n  " + m.getName() + " :");

            for( Class param : m.getParameterTypes()) {
                System.out.print(" " + param.getName());
            }
        }

        System.out.print("\n\n");
    }


    testUrlJumper() {
        this.name = "urljumper";
        this.currentStartingAddress = "";
        this.siteList = new ArrayList<>();
        this.urlList = new ArrayList<>();
    }


    public boolean Run() {

        // список сайтов
        String[] startingAddressArray = null;
        String startingAddressStr = this.webInterface.GetProp("starting_pages");
        if(!startingAddressStr.equals("")) {
            startingAddressArray = startingAddressStr.split(";");
        } else {
            System.out.println("There is no sites to test (sites = \"" + startingAddressStr + "\"");
            return true;}


        // переход на сайт(ы)
        for( String startingAddress : startingAddressArray ) {
            this.currentStartingAddress = startingAddress;

            // время старта теста
            this.startTime = System.currentTimeMillis();

            System.out.println("Starting with address " + this.currentStartingAddress);

            // создание родительского обьекта если подключение к странице возможно
            atwebUrl startingUrl = new atwebUrl(this.currentStartingAddress);
            this.urlList.add(startingUrl);

            // проход по всем страницам в списке страниц
            for(int i_url = 0; i_url < this.urlList.size(); i_url++) {
                atwebPage page = this.goTo(this.urlList.get(i_url));

                if(page != null) {
                    boolean foundSite = false;
                    for(atwebSite site : this.siteList) {
                        if(site.name.equals(page.site.name)) foundSite = true;
                    }
                    if(!foundSite) this.siteList.add(page.site);
                }
            }

            this.totalTime = System.currentTimeMillis() - this.startTime;
            System.out.println("total time " + this.totalTime);

            this.makeRawReport("raw.report.txt");
        }

        //System.out.println("\n\n\nsites dump:");
        //for(atwebSite site : this.siteList) {
        //    site.dump();
        //}

        // Письмо на почту

        return false;
    }



    protected atwebPage goTo(atwebUrl url) {
        System.out.print("\ngoTo " + url.urlStarting);

        // если коннект не совершался ранее, т.е. не заполнен окончательный адрес, то совершить коннект
        if (url.urlDestination.isEmpty()) {
            url.connect(true, true);

            if (url.numRedirects > 0)
                System.out.print(" response " + url.httpResponseCode + "; destination address is " + url.urlDestination +
                        " with " + url.numRedirects + " redirects; connection time is " + url.serverTimeAll + "ms (" + url.serverTimeDst + "ms); content type = " + url.contentType);
            else
                System.out.print(" response " + url.httpResponseCode + "; connection time is " + url.serverTimeAll + "ms; content type = " + url.contentType);
        }

        atwebPage page = this.pageFindOrCreate(url.urlDestination);
        url.page = page;

        {// поиск урла в списке урлов, ведущих на эту страницу страницы
            atwebUrl search_result = null;
            if (!page.getUrlOnPageList().isEmpty()) for (atwebUrl existingChildUrl : page.getUrlToPageList()) {
                if (existingChildUrl.urlStarting.equals(url.urlStarting)) search_result = existingChildUrl;
            }
            // добавляем на страницу урл, если его там нет
            if (search_result == null) {
                page.addUrlToPage(url);
            }
        }


        // если страница была протестирована ранее, то выход
        // если адрес не находится в пределах стартового, или если по адресу находится не html страница, то выход
        if (    page.isDone() ||
                !atwebUrl.normalizeUrl(url.urlDestination).split("(:\\/\\/)", 2)[1]
                .startsWith(atwebUrl.normalizeUrl(this.currentStartingAddress).split("(:\\/\\/)", 2)[1]) ||
                !url.contentType.startsWith("text/html"))
            return null;


        // если код ответа != 200, то страница не загружается
        if(url.httpResponseCode != 200)
            return null;


        { // загрузка страницы, подсчет времени
            long clientStartLoading = System.currentTimeMillis();

            try {
                this.webInterface.GetDriver().get(url.urlDestination);
            } catch (TimeoutException e) {
                page.setDone(false);
                page.setComment("TimeoutException");
                System.out.println("\nException caught when loading page [" + url.urlDestination + "]; StackTrace:");
                return null;
            } catch (Exception e) {
                page.setDone(false);
                page.setComment("Exception");
                System.out.println("\nException caught when loading page [" + url.urlDestination + "]; StackTrace:");
                e.printStackTrace();
                return null;
            }

            page.clientTime = System.currentTimeMillis() - clientStartLoading;

            System.out.print("; content loaded for " + page.clientTime + "ms");
        }


        // получение элементов страницы
        List<WebElement> ele_list_a;
        try {
            ele_list_a = this.webInterface.GetDriver().findElements(By.tagName("a"));
        } catch (Exception e) {
            page.setDone(false);
            page.setComment("Exception");
            System.out.println("\nException caught when getting elements of page [" + url.urlDestination + "]; StackTrace:");
            e.printStackTrace();
            return null;
        }

        //System.out.println("'a' tag count is " + ele_list_a.size() + "; list:");


        // получение ссылок на текущей странице
        //for(WebElement ele_a : ele_list_a) {
        for(int i_ele = 0; i_ele < ele_list_a.size(); i_ele++) {
            WebElement ele_a = ele_list_a.get(i_ele);

            String ele_href = null;
            try {
                ele_href = ele_a.getAttribute("href");
            } catch (Exception e) {
                if (    e.getMessage().startsWith("chrome not reachable") ||
                        e.getMessage().startsWith("no such window")) {
                    page.setDone(false);
                    page.setComment("Exception");
                    System.out.println("\nException caught when getting href attribute on page [" + url.urlDestination + "]; StackTrace:");
                    e.printStackTrace();
                    return null;
                } else {
                    System.out.println("\nException caught when getting href attribute on page [" + url.urlDestination + "]; StackTrace:");
                    e.printStackTrace();
                }
                continue;
            }

            // отсееваем невалидные урлы типо телефонов или пустых
            if(!atwebUrl.isValidUrl(ele_href)) continue;


            // замена домена (и протокола) из списка заменяемых доменов на домен из стартового адреса
            // тут именно добавление нового адреса, а не замена, что-бы в отчет сохранилась некорректная ссылка
            //TODO впереди неоптимизированный кусок кода!
            String[] domains = atwebMain.currentInterface.GetProp("domains_for_replace").split(";");
            boolean domain_found = false;
            String domain_replaced = "";
            for(String domain : domains) {
                String[] firstSplit = ele_href.split("(:\\/\\/)", 2);
                String[] secondSplit = firstSplit[1].split("\\/", 2);
                if( secondSplit[0].equals(domain) ) {
                    domain_found = true;
                    domain_replaced = page.site.getAddress() + "/" + secondSplit[1];
                }
            }

            //System.out.println(ele_href);

            urlFindOrCreate(page, ele_href);
            if(domain_found) {
                //System.out.println("!!! - found replaceable domain: \n" + ele_href + "\n" + domain_replaced);
                urlFindOrCreate(page, domain_replaced); }
        }

        // страница загружена и ссылки получены, значит done!
        page.setDone(true);

        return page;
    }



    protected atwebUrl urlFindOrCreate(atwebPage page, String url) {
        // поиск урла в глобальном списке урлов
        atwebUrl global_search_result = null;
        if(!this.urlList.isEmpty()) for(atwebUrl existingChildUrl : this.urlList) {
            if(existingChildUrl.urlStarting.equals(url)) global_search_result = existingChildUrl;
        }

        // если урла нет, то создаём объект, добавляем в глобальный список урлов
        if(global_search_result == null) {
            global_search_result = new atwebUrl(url);
            this.urlList.add(global_search_result);
        }

        // поиск урла в списке урлов страницы
        atwebUrl search_result = null;
        if(!page.getUrlOnPageList().isEmpty()) for(atwebUrl existingChildUrl : page.getUrlOnPageList()) {
            if(existingChildUrl.urlStarting.equals(url)) search_result = existingChildUrl;
        }

        // добавляем на страницу урл, если его там нет
        if(search_result == null) {page.addUrlOnPage(global_search_result); }

        return global_search_result;
    }



    protected atwebPage pageFindOrCreate(String url) {

        String normAddress = atwebUrl.normalizeUrl(url);
        String siteAddress = normAddress;

        String [] firstSplit = siteAddress.split("(:\\/\\/)",2); // отделяем протокол
        String [] secondSplit = firstSplit[1].split("\\/",2); // делим по слэшу

        siteAddress = firstSplit[0] + "://" + secondSplit[0];

        atwebSite currentSite = null;
        for(atwebSite site : this.siteList)
            if(site.getAddress().equals(siteAddress))
                currentSite = site;

        if(currentSite == null) {
            currentSite = new atwebSite();
            currentSite.setAddress(siteAddress);
        }

        atwebPage resultPage = currentSite.root;

        if(siteAddress.equals(normAddress))
            currentSite.pageList.add(currentSite.root);

        if(secondSplit.length > 1)
            resultPage = currentSite.pageFindOrCreate(secondSplit[1]);

        return resultPage;
    }



    //TODO Переместить в отдельный модель "report"



    protected boolean createRawDataFile(String fileName) throws Exception {
        File file = new File(fileName);
        boolean result = false;

        if(file.exists()) {
            if (file.isFile()) {
                if(file.delete()) {
                    result = file.createNewFile();
                } else {
                    System.out.print("Can't create new file " + fileName);
                }
            } else {
                System.out.print("Can't create new file " + fileName + ": directory located on this path");
                result = false;
            }
        } else {
            result = file.createNewFile();
        }

        return result;
    }



    protected void putRawDataInFile(String fileName, List<String> text) throws Exception {
        FileWriter writer = new FileWriter(fileName,true);
        for(String txt : text) {
            writer.write(txt);
        }
        writer.close();
    }



    protected void makeRawReport(String fileName) { // отчет в файл

        try {
            this.createRawDataFile(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for(atwebSite site : this.siteList) {
            pageToRawReport(fileName, site.root);
        }

    }



    protected void pageToRawReport(String fileName, atwebPage page) { // отчет в файл

        if(!page.getUrlToPageList().isEmpty()){
            List<String> StrList = new ArrayList<String>();

            StrList.add("p;" + page.getFullAddress() + ";" + page.clientTime + "\n");

            for (atwebUrl pageurl : page.getUrlOnPageList()) {
                StrList.add("u;" + pageurl.urlStarting + ";" + pageurl.urlDestination + ";"
                        + pageurl.numRedirects + ";" + pageurl.httpResponseCode + ";" + pageurl.httpFirstResponseCode + ";"
                        + pageurl.serverTimeAll + ";" + pageurl.serverTimeDst + ";"
                        + pageurl.contentType + "\n");
            }

            try {
                this.putRawDataInFile(fileName, StrList);
            } catch (Exception e) {
                e.printStackTrace();
            }

            StrList.clear();
        }

        if(!page.getChildList().isEmpty()) for(atwebPage pagechild : page.getChildList()) {
            pageToRawReport(fileName, pagechild);
        }
    }
}
