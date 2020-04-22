import org.openqa.selenium.WebElement;

import java.util.ArrayList;

public class atwebForm {
    atwebUrl page; // TODO this should be a page, not url
    String formXPath;
    String formJson;
    WebElement form;
    ArrayList<atwebFormElement> elements;
    atwebFormElement submit;

    atwebForm() {
        this.page = null;
        this.formXPath = "";
        this.formJson = "";
        this.form = null;
        this.elements = new ArrayList<>();
        this.submit = null;
    }

    void Clear() {
        this.page = null;
        this.formXPath = "";
        this.formJson = "";
        this.form = null;
        this.elements.clear();
        this.submit = null;
    }
}
