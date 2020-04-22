import javax.net.ssl.SSLException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

public class atwebUrl {
    protected atwebPage page; // страницы, доступная по этому урлу

    protected String urlStarting; // изначальный адрес, который был представлен на странице
    protected String urlDestination; // конечный адрес после всех редиректов

    protected int numRedirects; // количество редиректов от изначального до конечного адреса для этого урла
    protected int httpResponseCode; // ответ сервера для конечного адреса
    protected int httpFirstResponseCode; // ответ сервера для начального адреса

    protected long serverTimeAll; // время, которое было потрачено на все редиректы и получение ответа от сервера
    protected long serverTimeDst; // время ответа сервера для конечного адреса

    protected HttpURLConnection connection; // текущее соединение


    atwebUrl(String url) {
        this.page = null;
        this.urlStarting = url;
        this.urlDestination = "";
        this.numRedirects = 0;
        this.httpResponseCode = 0;
        this.httpFirstResponseCode = 0;
        this.serverTimeAll = 0;
        this.serverTimeDst = 0;
        this.connection = null;
    }


    void connect(boolean redirectEnable, boolean disconnect) {
        //this.urlStarting = url;
        //this.urlDestination = this.urlStarting; // назначение устанавливается после коннекта
        String urlCurrent = this.urlStarting;

        long connectionTimeStart = System.currentTimeMillis();

        HttpURLConnection.setFollowRedirects(redirectEnable);
        Integer maxRedirects = Integer.parseInt(atwebMain.currentInterface.GetProp("max_redirects"));
        boolean redirect = true;
        while(redirect) {
            if(this.numRedirects > maxRedirects) {this.httpResponseCode = -6; break;}

            // Коннект
            redirect = false;

            long connectionTimeDstStart = System.currentTimeMillis();
            try {
                this.connection = (HttpURLConnection) new URL(urlCurrent).openConnection();
                this.connection.setRequestMethod("HEAD");
                this.httpResponseCode = this.connection.getResponseCode();
            } catch (UnknownHostException e) {
                this.httpResponseCode = -2;
            } catch (SSLException e) {
                this.httpResponseCode = -3;
            } catch (Exception e) {
                this.httpResponseCode = -5;
                e.printStackTrace();
            }
            this.serverTimeDst = System.currentTimeMillis() - connectionTimeDstStart;

            // Код ответа при переходе по первой ссылке
            if (this.httpFirstResponseCode == 0) this.httpFirstResponseCode = this.httpResponseCode;

            // Редирект?
            if (this.httpResponseCode != HttpURLConnection.HTTP_OK) {
                if (    this.httpResponseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                        this.httpResponseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        this.httpResponseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    redirect = true;
                    this.numRedirects++;
                    urlCurrent = this.connection.getHeaderField("Location");
                    this.connection.disconnect();
                }
            }

            if(!redirectEnable) break;
        }

        this.serverTimeAll = System.currentTimeMillis() - connectionTimeStart;

        if(disconnect) this.connection.disconnect();

        this.urlDestination = urlCurrent;

        return;
    }




    static boolean isValidUrl(String u) {
        if(u == null) return false;
        else if(u.compareTo("") == 0) return false;
        else if(u.contains(":")) {
            if(!u.startsWith("http") || !u.startsWith("https")) return false;
        }

        return true;
    }


    static String normalizeUrl(String u) {
        String pageAddress = u;
        String getParameters = "";
        String anchor = "";

        // убираем якорь
        if(pageAddress.contains("#")) {
            String[] resultSplit = pageAddress.split("#",2);
            pageAddress = resultSplit[0];
            anchor = resultSplit[1];
        }

        // убираем геты
        if(pageAddress.contains("?")) {
            String[] resultSplit = pageAddress.split("\\?",2);
            pageAddress = resultSplit[0];
            getParameters = resultSplit[1];
        }

        // удаляем слэш в конце, если он есть
        if(pageAddress.endsWith("/")) pageAddress = pageAddress.substring(0, pageAddress.length() - 1);

        if(pageAddress.startsWith("https://")) pageAddress = pageAddress.substring(8);
        else if(pageAddress.startsWith("http://")) pageAddress = pageAddress.substring(7);
        String result = "http://" + pageAddress;

        if( atwebMain.currentInterface.GetProp("using_get_param").equals("true") )
            if(!getParameters.isEmpty())
                result += "?" + getParameters;

        if( atwebMain.currentInterface.GetProp("using_anchor").equals("true") )
            if(!anchor.isEmpty())
                result += "#" + anchor;

        return result;
    }


    /*static String deleteLastSlash(String url) {
        // не во всех урлах есть слэш в конце. приводим их к одному виду чтоб проверить на наличие их в списке
        // то-есть убираем слэш
        String Result = url;
        if(Result.contains("?")) {
            String [] SpStr = Result.split("\\?",2);
            if(SpStr[0].endsWith("/")) Result = SpStr[0].substring(0, SpStr[0].length() - 1).concat("?").concat(SpStr[1]);
        }
        else if(Result.endsWith("/")) Result = Result.substring(0, Result.length() - 1);

        return Result;
    }*/
}
