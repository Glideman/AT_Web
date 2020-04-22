import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;


public class moduleForm extends moduleDefault {
    HashMap<String, atwebForm> forms;
    JSONParser parser;

    @Override
    public void Init(atwebInterface webInterface) {
        this.webInterface = webInterface;

        // get class methods, save them in hash map
        Method[] m_list = moduleForm.class.getDeclaredMethods();
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


    moduleForm() {
        this.name = "form";
        this.parser = new JSONParser();
        this.forms = new HashMap<>();
    }


    boolean GetForm(String json) {
        JSONObject jsonObg;

        try {jsonObg = (JSONObject) this.parser.parse(json);}
        catch (Exception e) {
            e.printStackTrace();
            return true;}

        String formXPath = (String) jsonObg.get("form");
        String formSubmit = (String) jsonObg.get("submit");
        JSONArray formElements = (JSONArray) jsonObg.get("elements");
        Iterator elementsIter = formElements.iterator();

        atwebForm wForm = null;
        wForm = this.forms.getOrDefault(formXPath,null);

        if( wForm == null ) {
            wForm = new atwebForm();
            this.forms.put(formXPath, wForm);
            //System.out.println("new form " + formXPath);
        }
        else {
            wForm.Clear();
            //System.out.println("old form " + formXPath);
        }

        wForm.formXPath = formXPath;
        wForm.formJson = json;
        wForm.page = webInterface.GetCurrentPage();

        try { wForm.form = this.webInterface.GetDriver().findElement(By.xpath(wForm.formXPath));}
        catch (NoSuchElementException e) {
            wForm.formXPath = "";
            return true;}

        while (elementsIter.hasNext()) {
            atwebFormElement fElement = new atwebFormElement();
            wForm.elements.add(fElement);

            JSONObject formElement = (JSONObject) elementsIter.next();
            fElement.elementType = (String) formElement.get("type");
            fElement.elementXPath = (String) formElement.get("element");

            try {fElement.element = wForm.form.findElement(By.xpath(fElement.elementXPath));}
            catch (NoSuchElementException e) {continue;}

            if(fElement.elementType.equals("select")) {fElement.select = new Select(fElement.element);}
        }

        WebElement selElement = null;


        //System.out.println("ad qwedqwedxqw: " + formXPath+formSubmit);

        try { selElement = wForm.form.findElement(By.xpath(formSubmit)); }
        catch (NoSuchElementException e) {System.out.println("Нет сабмита!!1!");return false;}

        //System.out.println("(get) submit " + formSubmit + (selElement == null ? " null" : " not null"));

        atwebFormElement fElement = new atwebFormElement();
        fElement.element = selElement;
        fElement.elementType = "submit";
        fElement.elementXPath = formSubmit;
        wForm.submit = fElement;


        return false;
    }


    /*boolean GetFormById(String formId) {
        webForm wForm = new webForm();
        wForm.formXPath = "//form[@id='" + formId + "']";
        wForm.page = webInterface.GetCurrentPage();

        try { wForm.form = this.webInterface.GetDriver().findElement(By.id(formId));
        } catch (NoSuchElementException e) {
            wForm.formXPath = "";
            return true;}
        System.out.print("found form with xpath " + wForm.formXPath + "\n");
        this.FindElements(wForm);
        forms.put(wForm.formXPath, wForm);
        return false;
    }*/


    /*boolean GetFormByClass(String formClass) {
        webForm wForm = new webForm();
        wForm.formXPath = "//form[contains(@class, '" + formClass + "')]";
        wForm.page = webInterface.GetCurrentPage();

        try { wForm.form = this.webInterface.GetDriver().findElement(By.className(formClass));
        } catch (NoSuchElementException e) {
            wForm.formXPath = "";
            return true;}
        System.out.print("found form with xpath " + wForm.formXPath + "\n");
        this.FindElements(wForm);
        return false;
    }*/


    /*boolean GetFormByXpath(String formXPath) {
        webForm wForm = new webForm();
        wForm.formXPath = formXPath;
        wForm.page = webInterface.GetCurrentPage();

        try { wForm.form = this.webInterface.GetDriver().findElement(By.xpath(formXPath));
        } catch (NoSuchElementException e) {
            wForm.formXPath = "";
            return true;}
        System.out.print("found form with xpath " + wForm.formXPath + "\n");
        this.FindElements(wForm);
        return false;
    }*/


    /*boolean FindElements(webForm form) {

        // сбор полей на форме
        String xPathExpression = "//*[local-name()='input' or local-name()='textarea']";
        List<WebElement> selElementList = null;
        try {selElementList = form.form.findElements(By.xpath(xPathExpression));}
        catch (NoSuchElementException e) {return true;}

        for(WebElement selElement : selElementList) {
            // тип поля
            String fElementTag = selElement.getTagName();
            String fElementType = "";
            if(fElementTag.equals("input"))
                fElementType = selElement.getAttribute("type");
            else
                fElementType = fElementTag;

            // скрытые поля пропускаем
            if(fElementTag.equals("hidden")) continue;
            System.out.print("  " + fElementType);

            formElement fElement = new formElement();
            fElement.element = selElement;
            fElement.elementType = fElementType;

            // путь элемента
            String fElementName = selElement.getAttribute("name");
            System.out.print("  " + fElementName);
            if(fElementName != null)
                if(!fElementName.isEmpty())
                    fElement.elementXPath = "//" + fElementTag + "[@name='" + fElementName + "']";
            System.out.print("  :" + fElement.elementXPath + ":");

            // сабмит?
            if(fElement.elementType.equals("submit"))
                form.submit = fElement;
            else
                form.elements.add(fElement);

            if(form.submit != null) System.out.print("  submit");
            System.out.println();
        }

        // если на форме кнопка отправки реализована не инпатом с типом "submit", то ищем кнопку другими способами
        if(form.submit == null) {
            WebElement selSubmit = null;
            try {
                selSubmit = form.form.findElement(By.xpath("//*[contains(text(), 'Отправить')]"));
                form.submit = new formElement();
                form.submit.element = selSubmit;
                System.out.print("--also found submit button\n");
            } catch (NoSuchElementException e) {

            }
        }

        System.out.println();
        return false;
    }*/


    boolean SendForm(String json) {
        JSONObject jsonObg;

        try {jsonObg = (JSONObject) this.parser.parse(json);}
        catch (Exception e) {
            e.printStackTrace();
            return true;}

        String formXPath = (String) jsonObg.get("form");
        JSONArray formElements = (JSONArray) jsonObg.get("elements");
        Iterator elementsIter = formElements.iterator();


        System.out.print("\nAt page " + this.webInterface.GetCurrentPage().urlDestination + "\n  try to send form " + formXPath);


        atwebForm thisForm = this.forms.getOrDefault(formXPath,null);


        if(thisForm != null) {

            int formElementMaxCounter = 0;
            int formElementSentCounter = 0;

            for(atwebFormElement fElement : thisForm.elements) {
                JSONObject formElement = (JSONObject) elementsIter.next();

                formElementMaxCounter++;
                if(fElement.element == null) continue;
                formElementSentCounter++;

                String elementValue = (String) formElement.get("value");

                //System.out.println("put " + elementValue + " into " + fElement.elementXPath);
                //System.out.println("element " + fElement.elementXPath + (fElement.element == null ? " null" : " not null"));

                switch(fElement.elementType) {
                    case "text":
                    case "textarea":
                    case "number":
                    case "date":
                    case "datetime-local":
                    case "email":
                    case "tel":
                    case "phone":
                    case "password":
                        fElement.element.sendKeys(elementValue);
                        break;
                    case "select":
                        fElement.select.selectByVisibleText(elementValue);
                        break;
                    case "checkbox":
                    case "radio":
                    case "button":
                    case "reset":
                    case "submit":
                        if(elementValue.equals("true") && !fElement.element.isSelected()) fElement.element.click();
                        break;
                    case "color":
                    case "file":
                    case "hidden":
                    case "image":
                    case "month":
                    case "week":
                    case "time":
                    case "range":
                    case "search":
                    case "url":
                        break;
                }
            }

            System.out.print(", and sent " +formElementSentCounter+ " of " +formElementMaxCounter+ " elements");

            boolean formSubmitted = false;
            if(thisForm.submit != null)
                if(thisForm.submit.element != null) {
                    formSubmitted = true;
                    try{thisForm.submit.element.click();} catch (Exception e) {formSubmitted = false;}
                }

            if(formSubmitted) { System.out.println(" with no problems"); }
            else { System.out.println(" without submitting"); }
        } else {
            System.out.println(", but there is no form with that path.");
        }


        return false;
    }




    /*void SendFormA(String url) throws Exception {
        this.GoTo(url);

        this.GetControls();

        this.name_field.sendKeys("TEST bot");

        if(this.contacts) {
            this.contacts_field.sendKeys("test@bot.mail");
        } else {
            this.phone_field.sendKeys("7(987)654-32-10");
            this.mail_field.sendKeys("test@bot.mail");
        }

        this.message_field.sendKeys("message");
        this.submit_btn.click();
    }*/

}
