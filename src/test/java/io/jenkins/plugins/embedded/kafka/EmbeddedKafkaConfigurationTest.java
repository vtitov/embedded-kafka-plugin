package io.jenkins.plugins.embedded.kafka;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class EmbeddedKafkaConfigurationTest {

    @Rule
    public RestartableJenkinsRule rr = new RestartableJenkinsRule();

    /**
     * Tries to exercise enough code paths to catch common mistakes:
     * <ul>
     * <li>missing {@code load}
     * <li>missing {@code save}
     * <li>misnamed or absent getter/setter
     * <li>misnamed {@code textbox}
     * </ul>
     */

    @Ignore
    public void uiAndStorage() {
        // FIXME rewrite assuming EmbeddedKafkaProvider is not a singleton
        //rr.then(r -> {
        //    assertNull("not set initially", EmbeddedKafkaProvider.get().getPath());
        //    HtmlForm config = r.createWebClient().goTo("configure").getFormByName("config");
        //    HtmlTextInput kafkaDirectoryTextbox = config.getInputByName("_.kafkaDirectory");
        //    kafkaDirectoryTextbox.setText("hello");
        //    r.submit(config);
        //    assertEquals("global config page let us edit it", "hello", EmbeddedKafkaProvider.get().getPath());
        //});
        //rr.then(r -> {
        //    assertEquals("still there after restart of Jenkins", "hello", EmbeddedKafkaProvider.get().getPath());
        //});
    }

}
