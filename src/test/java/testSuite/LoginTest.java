package testSuite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.UUID;

/**
 * Login en OWASP Juice Shop con Selenium a traves del proxy de OWASP ZAP (localhost:9095).
 * Registra un usuario (Juice Shop no trae uno fijo), inicia sesion y verifica la sesion
 * activa. El trafico queda capturado por ZAP para el escaneo de VulnerabilityTest.
 */
public class LoginTest {

    WebDriver driver;
    WebDriverWait wait;
    static final String TARGET = "http://demo.owasp-juice.shop";

    @BeforeEach
    public void openBrowserProxy() {
        Proxy proxy = new Proxy();
        proxy.setHttpProxy("localhost:9095");
        proxy.setSslProxy("localhost:9095");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--ignore-certificate-errors");
        options.setProxy(proxy);

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @Test
    @DisplayName("Login Juice Shop")
    public void verifyLogin() {
        String email = "zapv6_" + UUID.randomUUID().toString().substring(0, 8) + "@zap.com";
        String password = "Zapv6!2024";

        // abrir y cerrar banners de cookies / bienvenida
        driver.get(TARGET + "/#/");
        clickIfPresent(By.cssSelector("a[aria-label='dismiss cookie message']"));
        clickIfPresent(By.cssSelector("button[aria-label='Close Welcome Banner']"));

        // registrar usuario (Juice Shop no trae credenciales fijas)
        driver.get(TARGET + "/#/register");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("emailControl"))).sendKeys(email);
        driver.findElement(By.id("passwordControl")).sendKeys(password);
        driver.findElement(By.id("repeatPasswordControl")).sendKeys(password);
        wait.until(ExpectedConditions.elementToBeClickable(By.id("securityQuestionControl"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("mat-option"))).click();
        driver.findElement(By.id("securityAnswerControl")).sendKeys("zap");
        driver.findElement(By.id("registerButton")).click();
        wait.until(ExpectedConditions.urlContains("login"));

        // iniciar sesion
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("loginButton")).click();

        // verificar sesion activa: el menu de cuenta muestra el boton de Logout
        wait.until(ExpectedConditions.elementToBeClickable(By.id("navbarAccount"))).click();
        Assertions.assertTrue(
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("navbarLogoutButton"))).isDisplayed());
    }

    private void clickIfPresent(By locator) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
        } catch (Exception ignored) {
            // banner opcional ausente: continuar
        }
    }

    @AfterEach
    public void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }
}
