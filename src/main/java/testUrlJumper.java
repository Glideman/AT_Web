import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class testUrlJumper extends moduleDefault {

    protected long startTime;
    protected long totalTime;

    protected List<atwebSite> siteList;
    protected List<atwebUrl> urlList;
    protected String currentStartingAddress;

    protected int strLimit;
    protected int strCount;

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
        this.strLimit = 128;
        this.strCount = 0;
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
                    // TODO Это нужно вынести в pageFindOrCreate
                    boolean foundSite = false;
                    for(atwebSite site : this.siteList) {
                        if(site.name.equals(page.site.name)) foundSite = true;
                    }
                    if(!foundSite) this.siteList.add(page.site);
                }
            }

            this.totalTime = System.currentTimeMillis() - this.startTime;
            System.out.println("total time " + this.totalTime);


            //this.makeRawReport("raw.report.txt");
            this.reportHTML(this.siteList.get(0).name+".html", this.currentStartingAddress);
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
        url.destinationPage = page;

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
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), StandardCharsets.UTF_8));
        //FileWriter writer = new FileWriter(fileName,true);
        for(String txt : text) {
            //writer.write(txt);
            bufferedWriter.write(txt);
        }
        //writer.close();
        bufferedWriter.close();
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



    protected void reportHTML(String fileName, String siteAddress) { // отчет в файл

        try {
            this.createRawDataFile(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<String> StrList = new ArrayList<String>();

        // Страница - начало
        StrList.add("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "\t<head>\n" +
                "\t\t<meta charset=\"UTF-8\">\n" +
                "\t\t<title>"+siteAddress+"</title>\n" +
                "\n" +
                "\t\t<!-- стили отображения отчета -->\n" +
                "\t\t<style type=\"text/css\">\n" +
                "\t\t\t.filter-block {\n" +
                "\t\t\t\tdisplay:flex;\n" +
                "\t\t\t\tflex-direction:row;\n" +
                "\t\t\t}\n" +
                "\t\t\t.filter-block > div {\n" +
                "\t\t\t//\tfloat:left;\n" +
                "\t\t\t\tmargin-right:16px;\n" +
                "\t\t\t}\n" +
                "\t\t\t//.filter-block > div:last-child {\n" +
                "\t\t\t//\tfloat:none;\n" +
                "\t\t\t//}\n" +
                "\t\t\tselect, input {\n" +
                "\t\t\t\tmargin-bottom:5px;\n" +
                "\t\t\t\t//margin-left:5px;\n" +
                "\t\t\t}\n" +
                "\t\t\tselect {\n" +
                "\t\t\t\tmin-width:125px;\n" +
                "\t\t\t\tmax-width:215px;\n" +
                "\t\t\t}\n" +
                "\t\t\t.hscroll {\n" +
                "\t\t\t\toverflow-x:scroll;\n" +
                "\t\t\t}\n" +
                "\t\t</style>\n" +
                "\t</head>\n" +
                "\n" +
                "\t<body>\n" +
                "\t\t<section>\n" +
                "\t\t\t<h1>Отчет о проверке сайта "+siteAddress+"</h1>\n" +
                "\t\t\t<hr>\n" +
                "\t\t\t<p>Фильтр</p>\n" +
                "\t\t\t<form name=\"frm\">\n" +
                "\t\t\t\t<div class=\"filter-block\">\n" +
                "\t\t\t\t\t<div>\n" +
                "\t\t\t\t\t\tСайты<br>\n" +
                "\t\t\t\t\t\t<span>Скрыть:</span><br>\n" +
                "\t\t\t\t\t\t<select multiple size=\"5\" name=\"filter-sites\" class=\"js-filter\">\n" +
                "\t\t\t\t\t\t</select>\n" +
                "\t\t\t\t\t</div>\n" +
                "\t\t\t\t\t<div>\n" +
                "\t\t\t\t\t\t<label><input type=\"checkbox\" name=\"types-cb\" class=\"js-filter\" checked>Типы данных</label><br>\n" +
                "\t\t\t\t\t\t<span>Скрыть:</span><br>\n" +
                "\t\t\t\t\t\t<select multiple size=\"5\" name=\"filter-types\" class=\"js-filter\">\n" +
                "\t\t\t\t\t\t</select>\n" +
                "\t\t\t\t\t</div>\n" +
                "\t\t\t\t\t<div>\n" +
                "\t\t\t\t\t\t<label><input type=\"checkbox\" name=\"codes-cb\" class=\"js-filter\" checked>Код ответа</label><br>\n" +
                "\t\t\t\t\t\t<span>Скрыть:</span><br>\n" +
                "\t\t\t\t\t\t<select multiple size=\"5\" name=\"filter-codes\" class=\"js-filter\">\n" +
                "\t\t\t\t\t\t</select>\n" +
                "\t\t\t\t\t</div>\n" +
                "\t\t\t\t\t<div>\n" +
                "\t\t\t\t\t\t<label><input type=\"checkbox\" name=\"server-time-cb\" class=\"js-filter\" checked>Время ответа</label><br>\n" +
                "\t\t\t\t\t\t<label>От <input type=\"text\" name=\"server-time-a\" placeholder=\"500\" class=\"js-filter\"></label> <br>\n" +
                "\t\t\t\t\t\t<label>До <input type=\"text\" name=\"server-time-b\" placeholder=\"500\" class=\"js-filter\"></label> <br>\n" +
                "\t\t\t\t\t\t<label><input type=\"checkbox\" name=\"client-time-cb\" class=\"js-filter\" checked>Время загрузки</label><br>\n" +
                "\t\t\t\t\t\t<label>От <input type=\"text\" name=\"client-time-a\" placeholder=\"500\" class=\"js-filter\"></label> <br>\n" +
                "\t\t\t\t\t\t<label>До <input type=\"text\" name=\"client-time-b\" placeholder=\"500\" class=\"js-filter\"></label>\n" +
                "\t\t\t\t\t</div>\n" +
                "\t\t\t\t\t<div>\n" +
                "\t\t\t\t\t\t<label><input type=\"checkbox\" name=\"redirect-cb\" class=\"js-filter\" checked>Показывать редиректы</label><br>\n" +
                "\t\t\t\t\t</div>\n" +
                "\t\t\t\t</div>\n" +
                "\t\t\t\tРезультатов: <span id=\"result-counter\">0</span>\n" +
                "\t\t\t\t<input type=\"button\" value=\"Сбросить\" onclick=\"this.form.reset(); submitFilter();\">\n" +
                "\t\t\t\t<br>\n" +
                "\t\t\t\tСохранить таблицу в формате\n" +
                "\t\t\t\t<select>\n" +
                "\t\t\t\t\t<option>.csv</option>\n" +
                "\t\t\t\t</select>\n" +
                "\t\t\t\t<a href=\"#\" id=\"save-btn\" download=\"report.csv\" onclick=\"makeExportStr();\">Сохранить</a>\n" +
                "\t\t\t\t<br>\n" +
                "\t\t\t\tОграничить кол-во строк\n" +
                "\t\t\t\t<select name=\"max-lines\" class=\"js-filter\">\n" +
                "\t\t\t\t\t<option>32</option>\n" +
                "\t\t\t\t\t<option>128</option>\n" +
                "\t\t\t\t\t<option>512</option>\n" +
                "\t\t\t\t\t<option>2048</option>\n" +
                "\t\t\t\t\t<option>не ограничить</option>\n" +
                "\t\t\t\t</select>\n" +
                "\t\t\t\t(не влияет на экспорт)\n" +
                "\t\t\t</form>\n" +
                "\t\t\t<hr>\n" +
                "\t\t\t<div id=\"result-container\" class=\"hscroll\">\n" +
                "\t\t\t\tResult\n" +
                "\t\t\t</div>\n" +
                "\t\t</section>\n" +
                "\n" +
                "\t\t<!-- скрипты (фильтр и тп). функциональная часть -->\n" +
                "\t\t<script type=\"text/javascript\">\n" +
                "\n" +
                "let dataRaw = [\n");

        this.strCount = 128;

        for(atwebSite site : siteList) {
            addStrToStrListWithLimit(StrList,"[\'" + site.protocol + "\',\'" + site.name + "\',");

            addPageToHTML(site.root, StrList);

            addStrToStrListWithLimit(StrList,"],");
        }

        // Классы JS
        StrList.add("\n];\n" +
                "\n" +
                "\t\t\tclass atwebSite {\n" +
                "\t\t\t\tconstructor() {\n" +
                "\t\t\t\t\tthis.protocol = \"\";\n" +
                "\t\t\t\t\tthis.name = \"\";\n" +
                "\t\t\t\t\tthis.root = new atwebPage();\n" +
                "\t\t\t\t\tthis.root.site = this;\n" +
                "\t\t\t\t\tthis.pageList = new Array();\n" +
                "\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\tgetAddress() {\n" +
                "\t\t\t\t\treturn this.protocol + \"://\" + this.name;\n" +
                "\t\t\t\t}\n" +
                "\t\t\t}\n" +
                "\n" +
                "\n" +
                "\t\t\tclass atwebPage {\n" +
                "\t\t\t\tconstructor() {\n" +
                "\t\t\t\t\tthis.alias = \"\";\n" +
                "\t\t\t\t\tthis.comment = \"\";\n" +
                "\t\t\t\t\tthis.clientTime = 0;\n" +
                "\t\t\t\t\tthis.responseOnly = true;\n" +
                "\t\t\t\t\tthis.done = false;\n" +
                "\t\t\t\t\tthis.getParameter = false;\n" +
                "\t\t\t\t\tthis.anchor = false;\n" +
                "\t\t\t\t\tthis.formList = new Array();\n" +
                "\t\t\t\t\tthis.urlOnPageList = new Array();\n" +
                "\t\t\t\t\tthis.urlToPageList = new Array();\n" +
                "\t\t\t\t\tthis.childList = new Array();\n" +
                "\t\t\t\t\tthis.parent = null;\n" +
                "\t\t\t\t\tthis.site = null;\n" +
                "\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\tgetFullAddress() {\n" +
                "\t\t\t\t\tif(this.parent != null) {\n" +
                "\t\t\t\t\t\tlet parentFullAddress = this.parent.getFullAddress();\n" +
                "\t\t\t\t\t\tif(parentFullAddress.length > 0) {\n" +
                "\t\t\t\t\t\t\tif(this.getParameter)\n" +
                "\t\t\t\t\t\t\t\treturn parentFullAddress + \"?\" + this.alias;\n" +
                "\t\t\t\t\t\t\telse if(this.anchor)\n" +
                "\t\t\t\t\t\t\t\treturn parentFullAddress + \"#\" + this.alias;\n" +
                "\t\t\t\t\t\t\telse\n" +
                "\t\t\t\t\t\t\t\treturn parentFullAddress + \"/\" + this.alias;\n" +
                "\t\t\t\t\t\t}\n" +
                "\t\t\t\t\t\telse return this.alias;\n" +
                "\t\t\t\t\t}\n" +
                "\t\t\t\t\telse return this.alias;}\n" +
                "\t\t\t}\n" +
                "\n" +
                "\n" +
                "\t\t\tclass atwebUrl {\n" +
                "\t\t\t\tconstructor() {\n" +
                "\t\t\t\t\tthis.destinationPage = null;\n" +
                "\t\t\t\t\tthis.page = null;\n" +
                "\t\t\t\t\tthis.urlStarting = \"\";\n" +
                "\t\t\t\t\tthis.urlDestination = \"\";\n" +
                "\t\t\t\t\tthis.numRedirects = 0;\n" +
                "\t\t\t\t\tthis.httpResponseCode = 0;\n" +
                "\t\t\t\t\tthis.httpFirstResponseCode = 0;\n" +
                "\t\t\t\t\tthis.serverTimeAll = 0;\n" +
                "\t\t\t\t\tthis.serverTimeDst = 0;\n" +
                "\t\t\t\t\tthis.connection = null;\n" +
                "\t\t\t\t\tthis.contentType = \"\";\n" +
                "\t\t\t\t\tthis.comment = \"\";\n" +
                "\t\t\t\t}\n" +
                "\t\t\t}\n" +
                "\n" +
                "\t\t\tlet siteList = new Array();\n" +
                "\t\t\tlet pageList = new Array();\n" +
                "\t\t\tlet urlList = new Array();\n" +
                "\t\t\tlet resultData = new Array();\n" +
                "\t\t\tlet exportStr = \"\";\n" +
                "\n" +
                "\t\t\t// ищет в списке сайтов сайт, создаёт новый, если не был найден. возвращает atwebSite\n" +
                "\t\t\tfunction getSiteByAddress(address) {\n" +
                "\t\t\t\tlet result = null;\n" +
                "\n" +
                "\t\t\t\tlet addressSepA = splitString(address,\"://\",2);\n" +
                "\t\t\t\tlet addressSepB = splitString(addressSepA[1],\"/\",2);\n" +
                "\t\t\t\tlet siteProtocol = addressSepA[0];\n" +
                "\t\t\t\tlet siteName = addressSepB[0];\n" +
                "\n" +
                "\t\t\t\tresult = getSiteByName(siteName, siteProtocol);\n" +
                "\n" +
                "\t\t\t\treturn result;}\n" +
                "\n" +
                "\n" +
                "\t\t\t// ищет в списке сайтов сайт, создаёт новый, если не был найден. возвращает atwebSite\n" +
                "\t\t\tfunction getSiteByName(siteName, siteProtocol) {\n" +
                "\t\t\t\tlet result = null;\n" +
                "\n" +
                "\t\t\t\tfor(let site of siteList) {\n" +
                "\t\t\t\t\tif(site.name === siteName) result=site;}\n" +
                "\n" +
                "\t\t\t\tif(result == null) {\n" +
                "\t\t\t\t\tresult = new atwebSite();\n" +
                "\t\t\t\t\tif(typeof siteProtocol != 'undefined') result.protocol = siteProtocol;\n" +
                "\t\t\t\t\telse result.protocol = \"http\";\n" +
                "\t\t\t\t\tresult.name = siteName;\n" +
                "\t\t\t\t\tresult.root.alias = result.protocol + \"://\" + result.name;\n" +
                "\t\t\t\t\tsiteList.push(result);}\n" +
                "\n" +
                "\t\t\t\treturn result;}\n" +
                "\n" +
                "\n" +
                "\t\t\t// ищет в списке страниц страницу, создаёт новую, если не была найдена. возвращает atwebPage\n" +
                "\t\t\tfunction getPage(address) {\n" +
                "\t\t\t\tlet normAddress = normalizeUrl(address);\n" +
                "\t\t\t\tlet site = getSiteByAddress(normAddress);\n" +
                "\n" +
                "\t\t\t\tlet isNewPage = false;\n" +
                "\t\t\t\tlet resultPage = site.root;\n" +
                "\n" +
                "\t\t\t\tlet addressSepA = splitString(normAddress,\"://\",2);\n" +
                "\t\t\t\tlet addressSepB = splitString(addressSepA[1],\"/\",2);\n" +
                "\t\t\t\tlet getParams = \"\";\n" +
                "\n" +
                "\t\t\t\tif(addressSepB.length > 1) {\n" +
                "\t\t\t\t\tlet pageAddress = addressSepB[1];\n" +
                "\n" +
                "\t\t\t\t\t// отделяем гет параметры, если они есть\n" +
                "\t\t\t\t\tif(pageAddress.includes(\"?\")) {\n" +
                "\t\t\t\t\t\tlet addressSplit = splitString(pageAddress,\"?\",2);\n" +
                "\t\t\t\t\t\tpageAddress = addressSplit[0];\n" +
                "\t\t\t\t\t\tgetParams = addressSplit[1];\n" +
                "\t\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\t\t// делим по слэшу\n" +
                "\t\t\t\t\tlet addressSplit = splitString(pageAddress,\"\\/\");\n" +
                "\n" +
                "\t\t\t\t\tfor(let addressSplitPart of addressSplit) {\n" +
                "\t\t\t\t\t\tlet subPage = null;\n" +
                "\n" +
                "\t\t\t\t\t\tif(resultPage.childList.length > 0) for(let child of resultPage.childList) {\n" +
                "\t\t\t\t\t\t\tif(child.alias === addressSplitPart) subPage = child;}\n" +
                "\n" +
                "\t\t\t\t\t\tif(subPage == null) {\n" +
                "\t\t\t\t\t\t\tsubPage = new atwebPage();\n" +
                "\t\t\t\t\t\t\tsubPage.alias = addressSplitPart;\n" +
                "\t\t\t\t\t\t\tsubPage.getParameter = false;\n" +
                "\t\t\t\t\t\t\tsubPage.parent = resultPage;\n" +
                "\t\t\t\t\t\t\tsubPage.site = site;\n" +
                "\t\t\t\t\t\t\tresultPage.childList.push(subPage);\n" +
                "\t\t\t\t\t\t\tisNewPage = true;}\n" +
                "\n" +
                "\t\t\t\t\t\tresultPage = subPage;\n" +
                "\t\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\t\t// если есть гет параметр, то добавляем в список\n" +
                "\t\t\t\t\tif(getParams.length > 0) {\n" +
                "\t\t\t\t\t\tlet subPage = null;\n" +
                "\n" +
                "\t\t\t\t\t\tif(resultPage.childList.length > 0) for(let child of resultPage.childList) {\n" +
                "\t\t\t\t\t\t\tif(child.alias === getParams) subPage = child;}\n" +
                "\n" +
                "\n" +
                "\t\t\t\t\t\tif(subPage == null) {\n" +
                "\t\t\t\t\t\t\tsubPage = new atwebPage();\n" +
                "\t\t\t\t\t\t\tsubPage.alias = getParams;\n" +
                "\t\t\t\t\t\t\tsubPage.getParameter = true;\n" +
                "\t\t\t\t\t\t\tsubPage.parent = resultPage;\n" +
                "\t\t\t\t\t\t\tsubPage.site = site;\n" +
                "\t\t\t\t\t\t\tresultPage.childList.push(subPage);\n" +
                "\t\t\t\t\t\t\tisNewPage = true;}\n" +
                "\n" +
                "\t\t\t\t\t\tresultPage = subPage;\n" +
                "\t\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\t\tif(isNewPage == true) {\n" +
                "\t\t\t\t\t\tsite.pageList.push(resultPage);\n" +
                "\t\t\t\t\t}\n" +
                "\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\treturn resultPage;}\n" +
                "\n" +
                "\t\t\t//\n" +
                "\t\t\tfunction normalizeUrl(url) {\n" +
                "\t\t\t\tlet pageAddress = url;\n" +
                "\t\t\t\tlet getParameters = \"\";\n" +
                "\t\t\t\tlet anchor = \"\";\n" +
                "\n" +
                "\t\t\t\t// убираем якорь\n" +
                "\t\t\t\tif(pageAddress.includes(\"#\")) {\n" +
                "\t\t\t\t\tlet resultSplit = splitString(pageAddress,\"#\",2);\n" +
                "\t\t\t\t\tpageAddress = resultSplit[0];\n" +
                "\t\t\t\t\tanchor = resultSplit[1];}\n" +
                "\n" +
                "\t\t\t\t// убираем геты\n" +
                "\t\t\t\tif(pageAddress.includes(\"?\")) {\n" +
                "\t\t\t\t\tlet resultSplit = splitString(pageAddress,\"?\",2);\n" +
                "\t\t\t\t\tpageAddress = resultSplit[0];\n" +
                "\t\t\t\t\tgetParameters = resultSplit[1];}\n" +
                "\n" +
                "\t\t\t\t// удаляем слэш в конце, если он есть\n" +
                "\t\t\t\tif(pageAddress.endsWith(\"/\")) pageAddress = pageAddress.substring(0, pageAddress.length - 1);\n" +
                "\n" +
                "\t\t\t\tlet result = pageAddress;\n" +
                "\n" +
                "\t\t\t\tif(getParameters !== \"\")\n" +
                "\t\t\t\t\tresult += \"?\" + getParameters;\n" +
                "\n" +
                "\t\t\t\t//if(anchor !== \"\")\n" +
                "\t\t\t\t//\tresult += \"#\" + anchor;\n" +
                "\n" +
                "\t\t\t\treturn result;}\n" +
                "\n" +
                "\n" +
                "\t\t\tfunction splitString(str, separator, limit) {\n" +
                "\t\t\t\tlet strArr = new Array();\n" +
                "\t\t\t\tlet strMod = str;\n" +
                "\n" +
                "\t\t\t\tlet counter = 0;\n" +
                "\t\t\t\twhile (strMod.indexOf(separator) >= 0) {\n" +
                "\t\t\t\t\tcounter++;\n" +
                "\t\t\t\t\tstrArr.push(strMod.substring(0,strMod.indexOf(separator)));\n" +
                "\t\t\t\t\tstrMod = strMod.substring(strMod.indexOf(separator) + separator.length);\n" +
                "\t\t\t\t\tif(typeof limit != 'undefined') if(counter >= limit-1) break;\n" +
                "\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\tstrArr.push(strMod);\n" +
                "\t\t\t\treturn strArr;}\n" +
                "\n" +
                "\n" +
                "\t\t\tfunction processRawData(dataRaw) {\n" +
                "\n" +
                "\t\t\t\tfor( let siteRaw of dataRaw ) {\n" +
                "\t\t\t\t\tlet siteProtocol = siteRaw[0];\n" +
                "\t\t\t\t\tlet siteName = siteRaw[1];\n" +
                "\t\t\t\t\tlet siteRootPg = siteRaw[2];\n" +
                "\n" +
                "\t\t\t\t\tlet site = getSiteByName(siteName, siteProtocol);\n" +
                "\n" +
                "\t\t\t\t\tprocessPage(siteRootPg, site.root);\n" +
                "\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\treturn true;}\n" +
                "\n" +
                "\t\t\tfunction processPage(pageRaw, page) {\n" +
                "\t\t\t\t//page\n" +
                "\t\t\t\tpage.alias = pageRaw[0];\n" +
                "\t\t\t\tpage.comment = pageRaw[1];\n" +
                "\t\t\t\tpage.clientTime = pageRaw[2];\n" +
                "\t\t\t\tpage.getParameter = pageRaw[3];\n" +
                "\t\t\t\tpage.anchor = pageRaw[4];\n" +
                "\n" +
                "\t\t\t\tlet urlRawList = pageRaw[5];\n" +
                "\t\t\t\tlet childRawList = pageRaw[6];\n" +
                "\n" +
                "\t\t\t\tfor(let urlRaw of urlRawList) {\n" +
                "\n" +
                "\t\t\t\t\t// поиск урла в списке урлов страницы\n" +
                "\t\t\t\t\tlet url = null;\n" +
                "\t\t\t\t\tif(page.urlOnPageList.length > 0) for(let existingChildUrl of page.urlOnPageList) {\n" +
                "\t\t\t\t\t\tif(existingChildUrl.urlStarting === urlRaw[0]) url = existingChildUrl;}\n" +
                "\n" +
                "\t\t\t\t\t// добавляем на страницу урл, если его там нет\n" +
                "\t\t\t\t\tif(url == null) {\n" +
                "\t\t\t\t\t\turl = new atwebUrl();\n" +
                "\t\t\t\t\t\turl.urlStarting = urlRaw[0];\n" +
                "\t\t\t\t\t\turl.urlDestination = urlRaw[1];\n" +
                "\t\t\t\t\t\turl.numRedirects = urlRaw[2];\n" +
                "\t\t\t\t\t\turl.httpResponseCode = urlRaw[3];\n" +
                "\t\t\t\t\t\turl.httpFirstResponseCode = urlRaw[4];\n" +
                "\t\t\t\t\t\turl.serverTimeAll = urlRaw[5];\n" +
                "\t\t\t\t\t\turl.serverTimeDst = urlRaw[6];\n" +
                "\t\t\t\t\t\turl.contentType = urlRaw[7];\n" +
                "\t\t\t\t\t\turl.comment = urlRaw[8];\n" +
                "\n" +
                "\t\t\t\t\t\turl.page = page;\n" +
                "\t\t\t\t\t\turl.destinationPage = getPage(url.urlDestination);\n" +
                "\t\t\t\t\t\turl.destinationPage.urlToPageList.push(url);\n" +
                "\n" +
                "\t\t\t\t\t\tpage.urlOnPageList.push(url);\n" +
                "\t\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\t\t// поиск урла в глобальном списке урлов\n" +
                "\t\t\t\t\tlet searchResult = null;\n" +
                "\t\t\t\t\tif(urlList.length > 0) for(let existingChildUrl of urlList) {\n" +
                "\t\t\t\t\t\tif(existingChildUrl.urlStarting === url.urlStarting) searchResult = existingChildUrl;}\n" +
                "\n" +
                "\t\t\t\t\t// если урла нет, то создаём объект, добавляем в глобальный список урлов\n" +
                "\t\t\t\t\tif(searchResult == null) {\n" +
                "\t\t\t\t\t\turlList.push(url);}\n" +
                "\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\tfor(let childRaw of childRawList) {\n" +
                "\t\t\t\t\tlet childAddress = page.getFullAddress();\n" +
                "\t\t\t\t\tif(childRaw[3] === true) childAddress += \"?\";\n" +
                "\t\t\t\t\telse if(childRaw[4] === true) childAddress += \"#\";\n" +
                "\t\t\t\t\telse childAddress += \"/\";\n" +
                "\t\t\t\t\tchildAddress += childRaw[0];\n" +
                "\t\t\t\t\tlet child = getPage(childAddress);\n" +
                "\t\t\t\t\tprocessPage(childRaw, child);\n" +
                "\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\tpageList.push(page);\n" +
                "\t\t\t\treturn true;}\n" +
                "\n" +
                "\n" +
                "\t\t\tfunction getSelectValues(select) {\n" +
                "\t\t\t\tlet result = [];\n" +
                "\n" +
                "\t\t\t\tfor(let opt of select.options) {\n" +
                "\t\t\t\t\tif(opt.selected)\n" +
                "\t\t\t\t\t\tresult.push(opt.value || opt.text);\n" +
                "\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\treturn result;\n" +
                "\t\t\t}\n" +
                "\n" +
                "\n" +
                "\t\t\tfunction updateFilter() {\n" +
                "\t\t\t\tlet typeList = new Array();\n" +
                "\t\t\t\tlet codeList = new Array();\n" +
                "\n" +
                "\t\t\t\tfor(let url of urlList) {\n" +
                "\t\t\t\t\tlet searchResult = null;\n" +
                "\t\t\t\t\tfor(let urlType of typeList) {\n" +
                "\t\t\t\t\t\tif(urlType === url.contentType) searchResult = urlType;}\n" +
                "\t\t\t\t\tif(searchResult == null) typeList.push(url.contentType);\n" +
                "\n" +
                "\t\t\t\t\tsearchResult = null;\n" +
                "\t\t\t\t\tfor(let urlCode of codeList) {\n" +
                "\t\t\t\t\t\tif(urlCode === url.httpResponseCode) searchResult = urlCode;}\n" +
                "\n" +
                "\t\t\t\t\tsearchResult = null;\n" +
                "\t\t\t\t\tfor(let urlCode of codeList) {\n" +
                "\t\t\t\t\t\tif(urlCode === url.httpFirstResponseCode) searchResult = urlCode;}\n" +
                "\t\t\t\t\tif(searchResult == null) codeList.push(url.httpFirstResponseCode);\n" +
                "\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\tlet siteSelect = document.forms['frm']['filter-sites'];\n" +
                "\t\t\t\tlet typeSelect = document.forms['frm']['filter-types'];\n" +
                "\t\t\t\tlet codeSelect = document.forms['frm']['filter-codes'];\n" +
                "\n" +
                "\t\t\t\tfor(let site of siteList) {\n" +
                "\t\t\t\t\tlet optionTag = document.createElement(\"option\");\n" +
                "\t\t\t\t\toptionTag.innerText = site.name;\n" +
                "\t\t\t\t\tsiteSelect.appendChild(optionTag);}\n" +
                "\n" +
                "\t\t\t\tfor(let type of typeList) {\n" +
                "\t\t\t\t\tlet optionTag = document.createElement(\"option\");\n" +
                "\t\t\t\t\toptionTag.innerText = type;\n" +
                "\t\t\t\t\ttypeSelect.appendChild(optionTag);}\n" +
                "\n" +
                "\t\t\t\tfor(let code of codeList) {\n" +
                "\t\t\t\t\tlet optionTag = document.createElement(\"option\");\n" +
                "\t\t\t\t\toptionTag.innerText = code;\n" +
                "\t\t\t\t\tcodeSelect.appendChild(optionTag);}\n" +
                "\n" +
                "\t\t\t\treturn true;}\n" +
                "\n" +
                "\n" +
                "\t\t\tfunction makeExportStr() {\n" +
                "\t\t\t\texportStr = \"data:text/csv;base64,\";\n" +
                "\t\t\t\tlet encStr = \"\";\n" +
                "\t\t\t\tlet saveLink = document.getElementById(\"save-btn\");\n" +
                "\t\t\t\tlet counter = 0;\n" +
                "\n" +
                "\t\t\t\tfor(let resultRow of resultData) {\n" +
                "\t\t\t\t\tif(counter == 0) encStr += \"Num\";\n" +
                "\t\t\t\t\telse encStr += counter;\n" +
                "\n" +
                "\t\t\t\t\tfor(let resultCell of resultRow) {\n" +
                "\t\t\t\t\t\tencStr += \";\\\"\" + resultCell + \"\\\"\";\n" +
                "\t\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\t\tencStr += \"\\n\";\n" +
                "\t\t\t\t\tcounter ++;\n" +
                "\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\texportStr += window.btoa(encStr);\n" +
                "\t\t\t\t//window.location = exportStr;\n" +
                "\n" +
                "\t\t\t\tsaveLink.setAttribute(\"href\",exportStr);\n" +
                "\t\t\t}\n" +
                "\n" +
                "\n" +
                "\t\t\tfunction submitFilter() {\n" +
                "\t\t\t\t// disable\n" +
                "\t\t\t\t//let filterElements = document.getElementsByClassName(\"js-filter\");\n" +
                "\t\t\t\t//for(let filterElement of filterElements) {filterElement.disabled = true;}\n" +
                "\n" +
                "\t\t\t\tlet counter = 0;\n" +
                "\t\t\t\tlet resultCounterEle = document.getElementById(\"result-counter\");\n" +
                "\t\t\t\tlet hideSites = getSelectValues(document.forms['frm']['filter-sites']);\n" +
                "\t\t\t\tlet hideTypes = getSelectValues(document.forms['frm']['filter-types']);\n" +
                "\t\t\t\tlet hideCodes = getSelectValues(document.forms['frm']['filter-codes']);\n" +
                "\t\t\t\tlet timeServerA = document.forms['frm']['server-time-a'];\n" +
                "\t\t\t\tlet timeServerB = document.forms['frm']['server-time-b'];\n" +
                "\t\t\t\tlet timeClientA = document.forms['frm']['client-time-a'];\n" +
                "\t\t\t\tlet timeClientB = document.forms['frm']['client-time-b'];\n" +
                "\t\t\t\tlet showTypes = document.forms['frm']['types-cb'];\n" +
                "\t\t\t\tlet showCodes = document.forms['frm']['codes-cb'];\n" +
                "\t\t\t\tlet showServerTime = document.forms['frm']['server-time-cb'];\n" +
                "\t\t\t\tlet showClientTime = document.forms['frm']['client-time-cb'];\n" +
                "\t\t\t\tlet showRedirect = document.forms['frm']['redirect-cb'];\n" +
                "\n" +
                "\t\t\t\tfor(let resultRow of resultData) {\n" +
                "\t\t\t\t\twhile(resultRow.length > 0) resultRow.pop();}\n" +
                "\t\t\t\twhile(resultData.length > 0) resultData.pop();\n" +
                "\n" +
                "\t\t\t\tfor(let url of urlList) {\n" +
                "\t\t\t\t\tlet blocked = false;\n" +
                "\n" +
                "\t\t\t\t\tfor(let hs of hideSites) if(url.destinationPage !== null && url.destinationPage.site.name === hs) blocked = true;\n" +
                "\t\t\t\t\tfor(let ht of hideTypes) if(url.contentType == ht) blocked = true;\n" +
                "\t\t\t\t\tfor(let hc of hideCodes) if(url.httpResponseCode == hc) blocked = true;\n" +
                "\n" +
                "\t\t\t\t\tif(timeServerA.value != \"\") if(url.serverTimeAll < timeServerA.value*1) blocked = true;\n" +
                "\t\t\t\t\tif(timeServerB.value != \"\") if(url.serverTimeAll > timeServerB.value*1) blocked = true;\n" +
                "\t\t\t\t\tif(timeClientA.value != \"\") if(url.destinationPage.clientTime < timeClientA.value*1) blocked = true;\n" +
                "\t\t\t\t\tif(timeClientB.value != \"\") if(url.destinationPage.clientTime > timeClientB.value*1) blocked = true;\n" +
                "\n" +
                "\t\t\t\t\tif(blocked == true) continue;\n" +
                "\n" +
                "\t\t\t\t\tlet row = [];\n" +
                "\n" +
                "\t\t\t\t\tif(counter == 0) {\n" +
                "\t\t\t\t\t\trow.push(\"Link\");\n" +
                "\t\t\t\t\t\tif(showCodes.checked) row.push(\"Response\");\n" +
                "\t\t\t\t\t\tif(showRedirect.checked) {\n" +
                "\t\t\t\t\t\t\trow.push(\"Redirects\");\n" +
                "\t\t\t\t\t\t\trow.push(\"Destination\");\n" +
                "\t\t\t\t\t\t\tif(showCodes.checked) row.push(\"Response\");\n" +
                "\t\t\t\t\t\t\tif(showServerTime.checked) row.push(\"Response time with redirects\");\n" +
                "\t\t\t\t\t\t}\n" +
                "\t\t\t\t\t\tif(showServerTime.checked) row.push(\"Response time\");\n" +
                "\t\t\t\t\t\tif(showClientTime.checked) row.push(\"Load time\");\n" +
                "\t\t\t\t\t\tif(showTypes.checked) row.push(\"Content type\");\n" +
                "\t\t\t\t\t} else {\n" +
                "\t\t\t\t\t\trow.push(url.urlStarting);\n" +
                "\t\t\t\t\t\tif(showCodes.checked) row.push(url.httpFirstResponseCode);\n" +
                "\t\t\t\t\t\tif(showRedirect.checked) {\n" +
                "\t\t\t\t\t\t\trow.push(url.numRedirects);\n" +
                "\t\t\t\t\t\t\trow.push(url.urlDestination);\n" +
                "\t\t\t\t\t\t\tif(showCodes.checked) row.push(url.httpResponseCode);\n" +
                "\t\t\t\t\t\t\tif(showServerTime.checked) row.push(url.serverTimeAll);\n" +
                "\t\t\t\t\t\t}\n" +
                "\t\t\t\t\t\tif(showServerTime.checked) row.push(url.serverTimeDst);\n" +
                "\t\t\t\t\t\tif(showClientTime.checked) row.push(url.destinationPage.clientTime);\n" +
                "\t\t\t\t\t\tif(showTypes.checked) row.push(url.contentType);\n" +
                "\t\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\t\tresultData.push(row);\n" +
                "// progress calc\n" +
                "\t\t\t\t\tcounter ++;\n" +
                "\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\tresultCounterEle.innerText = resultData.length;\n" +
                "\t\t\t\t//makeExportStr();\n" +
                "\t\t\t\tmakeTable();\n" +
                "\t\t\t\t// enable\n" +
                "\t\t\t\t//filterElements = document.getElementsByClassName(\"js-filter\");\n" +
                "\t\t\t\t//for(let filterElement of filterElements) {filterElement.disabled = false;}\n" +
                "\t\t\t\treturn true;}\n" +
                "\n" +
                "\n" +
                "\t\t\tfunction makeTable() {\n" +
                "\t\t\t\tlet counter = 0;\n" +
                "\n" +
                "\t\t\t\tlet maxLines = getSelectValues(document.forms['frm']['max-lines'])[0];\n" +
                "\t\t\t\tif(maxLines === \"не ограничить\") maxLines = 0;\n" +
                "\n" +
                "\t\t\t\tlet tableElement = document.createElement(\"table\");\n" +
                "\t\t\t\ttableElement.setAttribute(\"border\",\"1px\");\n" +
                "\t\t\t\ttableElement.setAttribute(\"bordercolor\",\"#cacaca\");\n" +
                "\t\t\t\ttableElement.setAttribute(\"frame\",\"border\");\n" +
                "\t\t\t\ttableElement.setAttribute(\"rules\",\"all\");\n" +
                "\t\t\t\ttableElement.setAttribute(\"cellpadding\",\"5px\");\n" +
                "\n" +
                "\t\t\t\tfor(let resultRow of resultData) {\n" +
                "\t\t\t\t\tlet rowEle = document.createElement(\"tr\");\n" +
                "\n" +
                "\t\t\t\t\tlet tagName = \"\";\n" +
                "\t\t\t\t\tif(counter == 0) {tagName = \"th\";}\n" +
                "\t\t\t\t\telse {tagName = \"td\";}\n" +
                "\n" +
                "\t\t\t\t\tlet numEle = document.createElement(tagName);\n" +
                "\t\t\t\t\tif(counter == 0) numEle.innerText = \"Num\";\n" +
                "\t\t\t\t\telse numEle.innerText = counter;\n" +
                "\t\t\t\t\trowEle.appendChild(numEle);\n" +
                "\n" +
                "\t\t\t\t\tfor(let resultCell of resultRow) {\n" +
                "\t\t\t\t\t\tlet cellEle = document.createElement(tagName);\n" +
                "\t\t\t\t\t\tcellEle.innerText = resultCell;\n" +
                "\t\t\t\t\t\trowEle.appendChild(cellEle);\n" +
                "\t\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\t\ttableElement.appendChild(rowEle);\n" +
                "\t\t\t\t\tcounter ++;\n" +
                "\t\t\t\t\tif(maxLines > 0 && counter > maxLines) break;\n" +
                "\t\t\t\t}\n" +
                "\n" +
                "\t\t\t\tlet resultTableContainer = document.getElementById(\"result-container\");\n" +
                "\t\t\t\tresultTableContainer.innerText = \"\";\n" +
                "\t\t\t\tresultTableContainer.appendChild(tableElement);\n" +
                "\t\t\t\treturn true;}\n" +
                "\n" +
                "\n" +
                "\t\t\twindow.onload = function() {\n" +
                "\t\t\t\tprocessRawData(dataRaw);\n" +
                "\n" +
                "\t\t\t\tlet filterElements = document.getElementsByClassName(\"js-filter\");\n" +
                "\t\t\t\tfor(let filterElement of filterElements) {filterElement.onchange = submitFilter;}\n" +
                "\n" +
                "\t\t\t\tupdateFilter();\n" +
                "\t\t\t\tsubmitFilter();\n" +
                "\t\t\t\tmakeTable();\n" +
                "\t\t\t};\n" +
                "\n" +
                "\n" +
                "\t\t</script>\n" +
                "\t</body>\n" +
                "</html>");

        try {
            this.putRawDataInFile(fileName, StrList);
        } catch (Exception e) {
            e.printStackTrace();
        }

        StrList.clear();
    }



    protected void addPageToHTML(atwebPage page, List<String> text) {
        if(page != null) {

            // Данные сайта
            String line = "[\'" +
                    page.alias + "\',\'" +
                    page.comment + "\'," +
                    page.clientTime + "," +
                    page.getParameter + "," +
                    page.anchor + ",[";
            addStrToStrListWithLimit(text,line);

            // список урлов
            for(atwebUrl url : page.getUrlOnPageList()) {
                line = "[\'" +
                        url.urlStarting + "\',\'" +
                        url.urlDestination + "\'," +
                        url.numRedirects + "," +
                        url.httpResponseCode + "," +
                        url.httpFirstResponseCode + "," +
                        url.serverTimeAll + "," +
                        url.serverTimeDst + ",\'" +
                        url.contentType + "\',\'" +
                        url.comment + "\'],";
                addStrToStrListWithLimit(text,line);
            }

            // дети
            addStrToStrListWithLimit(text,"],[");
            for(atwebPage child : page.getChildList()) {
                addPageToHTML(child,text);
            }
            addStrToStrListWithLimit(text,"]],");
        }
    }


    protected void addStrToStrListWithLimit(List<String> text, String line) {
        int eleIndex = text.size()-1;

        if(line.length() + this.strCount < this.strLimit) {
            text.set(eleIndex,text.get(eleIndex) + line);
            this.strCount += line.length();
            System.out.println("old line " + (line.length() + this.strCount));
        } else {
            text.set(eleIndex,text.get(eleIndex) + "\n");
            text.add(line);
            this.strCount = line.length();
            System.out.println("new line " + (line.length() + this.strCount));
        }
    }

}
