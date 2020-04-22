import java.util.ArrayList;
import java.util.List;

public class atwebSite {
    protected String protocol; // протокол подключения
    protected String name; // имя сайта
    protected atwebUrl startingUrl;

    protected atwebPage root; // корень сайта
    List<atwebPage> pageList; // корень сайта
    //protected List <atwebUrl> urlOnSiteList; // урлы на этом сайте (полный список)


    atwebSite() {
        this.protocol = "";
        this.name = "";
        this.startingUrl = null;

        this.root = new atwebPage();
        this.root.site = this;
        this.pageList = new ArrayList<>();
        //this.urlOnSiteList = new ArrayList<>();
    }


    String getAddress() {
        return protocol + "://" + name;
    }


    atwebUrl getStartingUrl() {
        return startingUrl;
    }


    //List <atwebUrl> getUrlOnSiteList() {
    //    return this.urlOnSiteList;
    //}


    void setAddress(String a) {
        String[] addressSep = a.split("(:\\/\\/)",2); //   '://'
        this.protocol = addressSep[0];
        this.name = addressSep[1];
        this.root.setAlias(a);
    }


    void setStartingUrl(atwebUrl u) {
        this.startingUrl = u;
    }


    //void addUrlOnSite(atwebUrl u) {
    //    this.urlOnSiteList.add(u);
    //}


    atwebPage pageFindOrCreate(String url) {
        atwebPage currentPage = this.root;

        String pageAddress = atwebUrl.normalizeUrl(url);
        String getParams = "";
        boolean isNewPage = false;

        // отделяем гет параметры, если они есть
        if(pageAddress.contains("?")) {
            String [] firstSplit = pageAddress.split("\\?",2);
            pageAddress = firstSplit[0];
            getParams = firstSplit[1];
        } else {
            pageAddress = url;
        }


        // TODO добавить сюда поддержку якорей, как отдельных дочерних элементов

        String [] secondSplit = pageAddress.split("\\/"); // делим по слэшу

        for(String secondSplitPart : secondSplit) {
            atwebPage subPage = null;

            if(!currentPage.childList.isEmpty()) for(atwebPage child : currentPage.childList) {
                if(child.getAlias().equals(secondSplitPart)) subPage = child;}

            if(subPage == null) {
                subPage = new atwebPage();
                subPage.setAlias(secondSplitPart);
                subPage.setGetParameter(false);
                subPage.setSite(this);
                currentPage.addChild(subPage);
                isNewPage = true;}

            currentPage = subPage;
        }


        // если есть гет параметр, то добавляем в список
        if(getParams.length() > 0) {
            atwebPage subPage = null;

            if(!currentPage.childList.isEmpty()) for(atwebPage child : currentPage.childList) {
                if(child.getAlias().equals(getParams)) subPage = child;}

            if(subPage == null) {
                subPage = new atwebPage();
                subPage.setAlias(getParams);
                subPage.setGetParameter(true);
                subPage.setSite(this);
                currentPage.addChild(subPage);
                isNewPage = true;}

            currentPage = subPage;
        }

        if(isNewPage) {
            this.pageList.add(currentPage);
            System.out.println("; new page");
        } else {
            System.out.println("; old page");}

        return currentPage;
    }



}
