import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

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

        }

        System.out.println("\n\n\nsites dump:");
        for(atwebSite site : this.siteList) {
            site.dump();
        }

        // Письмо на почту

        return false;
    }



    protected atwebPage goTo(atwebUrl url) {
        System.out.print("\ngoTo " + url.urlStarting);

        // если коннект не совершался ранее, т.е. не заполнен окончательный адрес, то совершить коннект
        if (url.urlDestination.isEmpty()) {
            url.connect(true, true);

            if (url.numRedirects > 0)
                System.out.println(" response " + url.httpResponseCode + "; destination address is " + url.urlDestination +
                        " with " + url.numRedirects + " redirects;\n  connection time is " + url.serverTimeAll + "ms (" + url.serverTimeDst + "ms); content type = " + url.contentType);
            else
                System.out.println(" response " + url.httpResponseCode + "; connection time is " + url.serverTimeAll + "ms; content type = " + url.contentType);
        }

        atwebPage page = this.pageFindOrCreate(url.urlDestination);

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


        { // загрузка страницы, подсчет времени
            long clientStartLoading = System.currentTimeMillis();

            try {
                this.webInterface.GetDriver().get(url.urlDestination);
            } catch (TimeoutException e) {
                page.setComment("time out");
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            page.clientTime = System.currentTimeMillis() - clientStartLoading;

            System.out.println("content loaded for " + page.clientTime + "ms");
        }


        // получение элементов страницы
        List<WebElement> ele_list_a;
        try {
            ele_list_a = this.webInterface.GetDriver().findElements(By.tagName("a"));
        } catch (Exception e) {
            e.printStackTrace();
            page.setDone(false);
            return null;
        }

        //System.out.println("'a' tag count is " + ele_list_a.size() + "; list:");


        // получение ссылок на текущей странице
        for(WebElement ele_a : ele_list_a) {
            String ele_href = null;
            try {
                ele_href = ele_a.getAttribute("href");
            } catch (Exception e) {
                e.printStackTrace();
                if (e.getMessage().startsWith("chrome not reachable") ||
                        e.getMessage().startsWith("no such window")) {
                    page.setDone(false);
                    return null;
                }
                continue;
            }

            // отсееваем невалидные урлы типо телефонов или пустых
            if(!atwebUrl.isValidUrl(ele_href)) continue;


            //System.out.println(ele_href);


            // поиск урла в глобальном списке урлов
            atwebUrl global_search_result = null;
            if(!this.urlList.isEmpty()) for(atwebUrl existingChildUrl : this.urlList) {
                if(existingChildUrl.urlStarting.equals(ele_href)) global_search_result = existingChildUrl;
            }

            // если урла нет, то создаём объект, добавляем в глобальный список урлов
            if(global_search_result == null) {
                global_search_result = new atwebUrl(ele_href);
                this.urlList.add(global_search_result);
            }

            // поиск урла в списке урлов страницы
            atwebUrl search_result = null;
            if(!page.getUrlOnPageList().isEmpty()) for(atwebUrl existingChildUrl : page.getUrlOnPageList()) {
                if(existingChildUrl.urlStarting.equals(ele_href)) search_result = existingChildUrl;
            }

            // добавляем на страницу урл, если его там нет
            if(search_result == null) {page.addUrlOnPage(global_search_result); }
        }

        // страница загружена и ссылки получены, значит done!
        page.setDone(true);

        return page;
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

}
