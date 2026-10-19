package testSuite;

import io.qameta.allure.*;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
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

import static io.restassured.RestAssured.given;

/**
 * Caso de prueba: vulnerabilidad del FLUJO DE NEGOCIO sobre OWASP Juice Shop.
 *
 * Flujo cubierto (registro -> login -> agregar al carrito -> checkout) ejecutado
 * con Selenium a traves del proxy de OWASP ZAP (localhost:9095). Todo el trafico
 * del flujo queda capturado por ZAP; luego se dispara un active scan sobre el
 * host objetivo y, al finalizar, se adjunta el reporte de alertas de ZAP al
 * reporte de Allure.
 *
 * Requisitos para ejecucion satisfactoria en el IDE:
 *   - OWASP ZAP corriendo con la API en el puerto 9095 (daemon o GUI).
 *   - Chrome + chromedriver compatibles.
 *
 * Reporte Allure:
 *   gradle test                 -> genera build/allure-results
 *   allure serve build/allure-results
 */
public class JuiceShopBusinessFlowTest {

    WebDriver driver;
    WebDriverWait wait;
    Response response;

    // App objetivo del flujo de negocio
    static final String TARGET = "http://demo.owasp-juice.shop";
    // Proxy / API de OWASP ZAP
    static final String ZAP_PROXY = "localhost:9095";
    static final String ZAP_API = "http://localhost:9095";

    @BeforeEach
    public void openBrowserProxy() {
        Proxy proxy = new Proxy();
        proxy.setHttpProxy(ZAP_PROXY);
        proxy.setSslProxy(ZAP_PROXY);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--ignore-certificate-errors");
        options.setProxy(proxy);

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @Test
    @DisplayName("Flujo de negocio Juice Shop: registro -> login -> carrito -> checkout + active scan ZAP")
    @Severity(SeverityLevel.CRITICAL)
    @Owner("UCB")
    @Epic("Prueba de vulnerabilidad de flujo de negocio")
    @Feature("OWASP Juice Shop")
    @Story("Registro, autenticacion y compra con escaneo ZAP")
    @Link(name = "OWASP Juice Shop", url = "http://demo.owasp-juice.shop/")
    public void businessFlowVulnerabilityScan() throws InterruptedException {
        String email = "zapv6_" + UUID.randomUUID().toString().substring(0, 8) + "@zap.com";
        String password = "Zapv6!2024";

        // 1) Recorrer el flujo de negocio con Selenium (trafico capturado por ZAP)
        navigateHome();
        registerUser(email, password);
        login(email, password);
        addFirstProductToBasket();
        goToCheckout();

        // 2) Lanzar el active scan de ZAP sobre el flujo ya capturado
        String scanId = startActiveScan();
        waitScanProgress(scanId);
    }

    @Step("Abrir Juice Shop y cerrar banners de bienvenida/cookies")
    private void navigateHome() {
        driver.get(TARGET + "/#/");
        clickIfPresent(By.cssSelector("a[aria-label='dismiss cookie message']"));
        clickIfPresent(By.cssSelector("button[aria-label='Close Welcome Banner']"));
    }

    @Step("Registrar nuevo usuario: {0}")
    private void registerUser(String email, String password) {
        driver.get(TARGET + "/#/register");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("emailControl"))).sendKeys(email);
        driver.findElement(By.id("passwordControl")).sendKeys(password);
        driver.findElement(By.id("repeatPasswordControl")).sendKeys(password);
        driver.findElement(By.cssSelector("mat-select[name='securityQuestion']")).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("mat-option"))).click();
        driver.findElement(By.id("securityAnswerControl")).sendKeys("zap");
        driver.findElement(By.id("registerButton")).click();
        wait.until(ExpectedConditions.urlContains("login"));
    }

    @Step("Autenticar usuario: {0}")
    private void login(String email, String password) {
        driver.get(TARGET + "/#/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email"))).sendKeys(email);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("loginButton")).click();
        wait.until(ExpectedConditions.urlContains("search"));
    }

    @Step("Agregar primer producto al carrito")
    private void addFirstProductToBasket() {
        driver.get(TARGET + "/#/search");
        wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button[aria-label='Add to Basket']"))).click();
    }

    @Step("Ir al carrito y pasar a checkout")
    private void goToCheckout() {
        clickIfPresent(By.cssSelector("button[aria-label='Show the shopping cart']"));
        clickIfPresent(By.id("checkoutButton"));
    }

    @Step("Lanzar active scan de ZAP sobre {TARGET}")
    private String startActiveScan() {
        response = given()
                .queryParam("url", TARGET)
                .log().all()
                .when()
                .get(ZAP_API + "/JSON/ascan/action/scan/");
        response.then().log().all();
        String scanId = response.then().extract().path("scan");
        System.out.println("Scan ID: " + scanId);
        return scanId;
    }

    @Step("Esperar progreso del active scan hasta 100%")
    private void waitScanProgress(String scanId) throws InterruptedException {
        String state = "0";
        do {
            Thread.sleep(5000);
            response = given()
                    .queryParam("scanId", scanId)
                    .log().all()
                    .when()
                    .get(ZAP_API + "/JSON/ascan/view/status/");
            response.then().log().all();
            state = response.then().extract().path("status");
            System.out.println("Scan Status: " + state);
        } while (!state.equals("100"));
        System.out.println("Scan Completed *******: " + state);
    }

    /** Click tolerante a elementos opcionales (banners/botones que pueden no aparecer). */
    private void clickIfPresent(By locator) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
        } catch (Exception ignored) {
            // elemento opcional ausente: continuar el flujo
        }
    }

    @AfterEach
    public void tearDown() {
        try {
            // Reporte HTML de alertas de ZAP -> adjunto en Allure
            response = given()
                    .when()
                    .get(ZAP_API + "/OTHER/core/other/htmlreport/");
            response.then().log().all();
            attachHtmlReport("OWASP ZAP - Reporte de Alertas", response.getBody().asString());
        } catch (Exception e) {
            System.out.println("No se pudo obtener el reporte de ZAP: " + e.getMessage());
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    @Attachment(value = "{0}", type = "text/html")
    public String attachHtmlReport(String name, String content) {
        return content;
    }
}
