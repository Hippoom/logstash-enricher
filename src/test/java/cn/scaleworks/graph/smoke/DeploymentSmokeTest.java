package cn.scaleworks.graph.smoke;

import com.jayway.jsonpath.JsonPath;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.restassured.RestAssured.when;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        loader = AnnotationConfigContextLoader.class,
        classes = {
                ApplicationSpec.class
        }
)
public class DeploymentSmokeTest {
    @Autowired
    private ApplicationSpec app;

    @Before
    public void config_rest_assured() {
        RestAssured.baseURI = app.getBaseUri();
        RestAssured.port = app.getPort();
    }

    @Test
    public void itShouldShowDeploymentMeta_whenIVisitTheSiteRoot_givenNewDeploymentIsDone() {


        await().atMost(10, SECONDS).until(() -> {
            Response response = when().get("/meta.json")
                    .then()
                    .log().everything()
                    .extract().response();

            String expectedVersion = app.getVersion();
            String actualVersion = JsonPath.read(response.asString(), "$.version");

            assertThat(actualVersion, equalTo(expectedVersion));
        });
    }
}
