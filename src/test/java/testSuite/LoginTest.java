package testSuite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;

public class LoginTest {

    WebDriver driver;
    @BeforeEach
    public void openBrowserProxy(){
        Proxy proxy = new Proxy();
        proxy.setHttpProxy("localhost:9095");
        proxy.setSslProxy("localhost:9095");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--ignore-certificate-errors");
        options.setProxy(proxy);

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();
        driver.get("https://todo.ly/");
    }
//zapv6@zap.com  12345
    @Test
    public void verifyLogin(){
        driver.findElement(By.xpath("//img[@src=\"/Images/design/pagelogin.png\"]")).click();
        driver.findElement(By.id("ctl00_MainContent_LoginControl1_TextBoxEmail")).sendKeys("zapv6@zap.com");
        driver.findElement(By.id("ctl00_MainContent_LoginControl1_TextBoxPassword")).sendKeys("12345");
        driver.findElement(By.id("ctl00_MainContent_LoginControl1_ButtonLogin")).click();
        Assertions.assertTrue(driver.findElement(By.id("ctl00_HeaderTopControl1_LinkButtonLogout")).isDisplayed());
    }


    @AfterEach
    public void closeBrowser(){
        driver.quit();

    }




}
