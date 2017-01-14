import network.RequestTemplate;
import org.junit.Assert;
import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;

/**
 * @author KPentaris - 13/1/2017.
 */
@RunWith(value = JUnit4ClassRunner.class)
public class IntegrationTests {

    @Test
    public void conversionServiceResponseTest() throws Exception {
        RequestTemplate template = new RequestTemplate("http://www.w3schools.com/xml/tempconvert.asmx/CelsiusToFahrenheit", "POST");
        template.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        template.addRequestParam("Celsius", "40");

        String response = template.performRequest();
        int endOfOpeningXMLTag = response.indexOf(">", response.indexOf(">") + 1);
        int startOfEndingXMLTag = response.indexOf("<", endOfOpeningXMLTag);
        response = response.substring(endOfOpeningXMLTag + 1, startOfEndingXMLTag);

        Assert.assertEquals(response, "104");
    }
}
