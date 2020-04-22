import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class atwebFormElement {
    atwebForm form;
    String elementXPath;
    String elementType;
    String defaultData;
    WebElement element;
    Select select;

    atwebFormElement() {
        this.form = null;
        this.elementXPath = "";
        this.elementType = "";
        this.defaultData = "";
        this.element = null;
        this.select = null;
    }
}
