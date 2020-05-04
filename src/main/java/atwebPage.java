import java.util.ArrayList;
import java.util.List;

public class atwebPage {
    protected String alias; // сокращённое имя / часть адреса
    protected String comment; // комментарий

    protected long clientTime; // загрузка контента страницы

    protected boolean responseOnly; // эта страница была посещена минимум единожды
    protected boolean done; // все тесты на странице были закончены
    protected boolean getParameter; // это не страница - а гет параметр у родительской страницы
    protected boolean anchor; // это не страница - а якорь. поддержка якорей для страниц, контент которых зависит от якоря

    protected List<atwebForm> formList; // список форм на этой странице
    protected List <atwebUrl> urlOnPageList; // урлы на этой странице, которые ведут на различные ресурсы
    protected List <atwebUrl> urlToPageList; // урлы на других страницах, которые ведут на эту страницу
    protected List <atwebPage> childList; // список дочерних страниц
    protected atwebPage parent; // родитель этой страницы
    protected atwebSite site; // сайт, к которому относится эта страница


    atwebPage() {
        this.alias = "";
        this.comment = "";

        this.clientTime = 0;

        this.responseOnly = true;
        this.done = false;
        this.getParameter = false;
        this.anchor = false;

        this.formList = new ArrayList<>();
        this.urlOnPageList = new ArrayList<>();
        this.urlToPageList = new ArrayList<>();
        this.childList = new ArrayList<>();
        this.parent = null;
        this.site = null;
    }


    String getComment() {
        return this.comment;
    }


    String getAlias() {
        return this.alias;
    }


    String getFullAddress() {
        if(this.parent != null) {
            if(this.parent.getFullAddress().length() > 0) {
                if(this.getParameter)
                    return this.parent.getFullAddress() + "?" + this.alias;
                else if(this.anchor)
                    return this.parent.getFullAddress() + "#" + this.alias;
                else
                    return this.parent.getFullAddress() + "/" + this.alias;
            }
            else return this.alias;
        }
        else return this.alias;
    }


    String getPackedAddress() {
        boolean found = false;
        int index = 0;
        if(this.parent != null) {

            for(atwebPage child : this.parent.childList) {
                if(child.alias.equals(this.alias)) {found = true; break;}
                else index++;
            }

            if(!found) index = -1; // у родителя нет в списке дочерних этой страницы (wut?!)

            if(this.parent.getPackedAddress().length() > 0) {
                if(this.getParameter)
                    return this.parent.getPackedAddress() + "?" + index;
                else if(this.anchor)
                    return this.parent.getPackedAddress() + "#" + index;
                else
                    return this.parent.getPackedAddress() + "/" + index;
            }
            else return Integer.toString(index);

        }
        else return "";
    }


    List <atwebUrl> getUrlOnPageList() {
        return this.urlOnPageList;
    }


    List <atwebUrl> getUrlToPageList() {
        return this.urlToPageList;
    }


    List <atwebForm> getFormList() {
        return this.formList;
    }


    List <atwebPage> getChildList() {
        return this.childList;
    }


    atwebPage getParent() {
        return this.parent;
    }


    atwebSite getSite() {
        return this.site;
    }


    long getTime() {
        return this.clientTime;
    }


    boolean isResponseOnly() {
        return this.responseOnly;
    }


    boolean isDone() {
        return this.done;
    }


    void setAlias(String a) {
        this.alias = a;
    }


    void setComment(String c) {
        this.comment = c;
    }


    void setSite(atwebSite s) {
        this.site = s;
    }


    void setTime(long t) {
        this.clientTime = t;
    }


    void setGetParameter(boolean g) {
        if(g&&this.anchor) this.anchor = false;
        this.getParameter = g;
    }


    void setAnchor(boolean a) {
        if(a&&this.getParameter) this.getParameter = false;
        this.anchor = a;
    }


    void setResponseOnly(boolean r) {
        this.responseOnly = r;
    }


    void setDone(boolean d) {
        this.done = d;
    }


    void addChild(atwebPage p) {
        this.childList.add(p);
        p.parent = this;
        p.site = this.site;
    }


    void addUrlOnPage(atwebUrl u) {
        this.urlOnPageList.add(u);
    }


    void addUrlToPage(atwebUrl u) {
        this.urlToPageList.add(u);
    }


    /*atwebPage childFindOrCreate(String alias) {
        atwebPage subPage = null;

        if(!this.childList.isEmpty()) for(atwebPage child : this.childList) {
            if(child.getAlias().equals(alias)) subPage = child;}

        if(subPage == null) {
            subPage = new atwebPage();
            subPage.setAlias(alias);
            this.addChild(subPage);}

        return subPage;
    }*/


    void dump() {
        System.out.print("\n" + this.getFullAddress());

        if(this.getUrlToPageList().isEmpty())
            System.out.println("  url = none");
        else
            System.out.println("  url = " + this.getUrlToPageList().get(0).urlStarting);

        for( atwebUrl url : this.getUrlOnPageList()) {
            System.out.println(" |- " + url.urlStarting + " (" + url.httpFirstResponseCode + ")");
        }

        for( atwebPage child : this.childList) {
            child.dump();
        }
    }

}
