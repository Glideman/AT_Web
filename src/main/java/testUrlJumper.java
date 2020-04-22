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
            // время старта теста
            this.startTime = System.currentTimeMillis();

            System.out.println("Starting with address " + startingAddress);

            // создание родительского обьекта если подключение к странице возможно
            atwebUrl startingUrl = new atwebUrl(startingAddress);
            this.urlList.add(startingUrl);

            // проход по всем страницам в списке страниц
            for(int i_url = 0; i_url < this.urlList.size(); i_url++) {
                this.goTo(this.urlList.get(i_url));
            }

            this.totalTime = System.currentTimeMillis() - this.startTime;
        }

        // Письмо на почту

        return false;
    }



    protected void goTo(atwebUrl url) {
        System.out.print("goTo " + url.urlStarting);

        // если коннект не совершался ранее, т.е. не заполнен окончательный адрес, то совершить коннект
        if(url.urlDestination.isEmpty()) {
            url.connect(true, true);

            if(url.numRedirects > 0)
                System.out.println(" response " + url.httpResponseCode + "; destination address is " + url.urlDestination +
                        " with " + url.numRedirects + " redirects;\n  connection time is " + url.serverTimeAll + "ms (" + url.serverTimeDst + "ms)");
            else
                System.out.println(" response " + url.httpResponseCode + "; connection time is " + url.serverTimeAll + "ms");
        }

        atwebPage page = this.pageFindOrCreate(url.urlDestination);

        if(page.isDone()) { return; }


        //TODO Тут добавить проверку на то, находится ли страница в пределах допустимого для тестирования


        //if(page.isResponseOnly()) { return; }


        //TODO Тут добавить проверку на то, файл ли загружается по ссылке или веб документ

        long clientStartLoading = System.currentTimeMillis();

        // загрузка страницы, подсчет времени
        try {
            this.webInterface.GetDriver().get(url.urlDestination);
        } catch (TimeoutException e) {
            page.setComment("time out");
            return;
        } catch (Exception e) {
            e.printStackTrace();return;
        }

        page.clientTime = System.currentTimeMillis() - clientStartLoading;

        System.out.println("content loaded for " + page.clientTime + "ms");


        // получение элементов страницы
        List<WebElement> ele_list_a;
        try {
            ele_list_a = this.webInterface.GetDriver().findElements(By.tagName("a"));
        } catch (Exception e) {
            e.printStackTrace();
            page.setDone(false);
            return;
        }


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
                    return;
                }
                continue;
            }

            // отсееваем невалидные урлы типо телефонов или пустых
            if(!atwebUrl.isValidUrl(ele_href)) continue;


            // поиск урла в глобальном списке урлов
            atwebUrl global_search_result = null;
            if(!this.urlList.isEmpty()) for(atwebUrl existingChildUrl : this.urlList) {
                if(existingChildUrl.urlStarting.equals(ele_href)) global_search_result = existingChildUrl;
            }


            // если урла нет, то создаём объект, добавляем в глобальный список урлов
            if(global_search_result == null) {
                atwebUrl newChildUrl = new atwebUrl(ele_href);
                this.urlList.add(newChildUrl);

                // поиск урла в списке урлов страницы
                atwebUrl search_result = null;
                if(!page.getUrlOnPageList().isEmpty()) for(atwebUrl existingChildUrl : page.getUrlOnPageList()) {
                    if(existingChildUrl.urlStarting.equals(ele_href)) search_result = existingChildUrl;
                }

                // добавляем на страницу урл, если его там нет
                if(search_result == null) {page.addUrlOnPage(newChildUrl); }
            }

        }

        // страница загружена и ссылки получены, значит done!
        page.setDone(true);

    }



    protected atwebPage pageFindOrCreate(String url) {

        String siteAddress = atwebUrl.normalizeUrl(url);

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

            if(atwebUrl.normalizeUrl(siteAddress).equals(atwebUrl.normalizeUrl(url)))
                currentSite.pageList.add(currentSite.root);
        }

        atwebPage resultPage = currentSite.root;

        if(secondSplit.length > 1)
            resultPage = currentSite.pageFindOrCreate(secondSplit[1]);

        return resultPage;
    }

}
