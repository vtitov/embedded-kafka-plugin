package io.jenkins.plugins.embedded.kafka;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.jenkins.plugins.embedded.EmbeddedProvider;
import io.vavr.control.Option;
import jenkins.model.Jenkins;
import lombok.extern.java.Log;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.io.Serializable;

/**
 * Example of Jenkins global configuration.
 */
@Log
//@Extension
public class EmbeddedKafkaProvider extends EmbeddedProvider implements Serializable {

    //static final long serialVersionUID = 42L;
    private static final long serialVersionUID = 1905162041950251407L;

//    private static final int MAX_UNSIGNED_16_BIT_INT = 0xFFFF; // port max

//    /** @return the singleton instance */
//    public static EmbeddedHdfsProvider get() {
//        return GlobalConfiguration.all().get(EmbeddedHdfsProvider.class);
//    }
//
//    private String label;

    private String kafkaPort;
    private String zkPort;
    private String path; // kafka directory

    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    transient private Option<EmbeddedKafka> embeddedKafka = Option.none();

    @DataBoundConstructor
    public EmbeddedKafkaProvider(String kafkaPortString, String zkPort, String path) {
        this.kafkaPort = kafkaPortString;
        this.zkPort = zkPort;
        this.path = path;
    }
    public EmbeddedKafkaProvider() {
        // When Jenkins is restarted, load any saved configuration from disk.
        //load();
    }

//    /** @return the currently configured label, if any */
//    public String getLabel() {
//        return label;
//    }

//    /**
//     * Together with {@link #getLabel}, binds to entry in {@code config.jelly}.
//     * @param label the new value of this field
//     */
//    @DataBoundSetter
//    public void setLabel(String label) {
//        this.label = label;
//        save();
//    }

    public String getKafkaPort() {
        return kafkaPort;
    }

    @DataBoundSetter
    public void setKafkaPort(String kafkaPort) {
        this.kafkaPort = kafkaPort;
        //save();
    }

    public String getZkPort() {
        return zkPort;
    }

    @DataBoundSetter
    public void setZkPort(String zkPort) {
        this.zkPort = zkPort;
        //save();
    }

    /** @return the currently configured kafka directory, if any */
    public String getPath() {
        return path;
    }

    /**
     * Together with {@link #getPath}, binds to entry in {@code config.jelly}.
     * @param path the new value of this field
     */
    @DataBoundSetter
    public void setPath(String path) {
        this.path = path;
        //save();
    }

    public FormValidation doCheckLabel(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.warning("Please specify a label.");
        }
        return FormValidation.ok();
    }

//    public FormValidation doCheckKafkaDirectory(@QueryParameter String value) {
//        return xDoCheckDirectory(value);
//    }

    public FormValidation doCheckKafkaPort(@QueryParameter String value) {
        return xDoCheckPort(value);
    }

    public FormValidation doCheckZkPort(@QueryParameter String value) {
        return xDoCheckPort(value);
    }

//    protected FormValidation xDoCheckPort(@QueryParameter String value) {
//        return parsePort(value)
//                .filter(iPort -> iPort < 0 || iPort > MAX_UNSIGNED_16_BIT_INT)
//                .fold(
//                        ()->FormValidation.warning("Please specify an integer in range 0..65536"),
//                        iPort -> FormValidation.ok()
//                );
//    }

//    @Override
//    public boolean configure(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
//        log.info("StaplerRequest url: " + req.getRequestURIWithQueryString() + ", " + req.getRequestURI());
//        log.info("StaplerRequest uri" + req.getRequestURLWithQueryString() + ", " + req.getRequestURL());
//        log.info("JSONObject: " + json.toString());
//        stop();
//        req.bindJSON(this,json);
//        //save();
//        start();
//        return true;
//    }

    @Override
    public void start() {
        stop();
        embeddedKafka = Option.of(new EmbeddedKafka().startKafka(
                parsePort(zkPort), parsePort(kafkaPort), parseDir(path)
        ));
    }
    @Override
    public void stop() {
        if(embeddedKafka==null) { embeddedKafka=Option.none(); } // TODO find out why embeddedKafka can be null
        embeddedKafka.forEach(EmbeddedKafka::stop);
        embeddedKafka = Option.none();
    }

//    private static Option<String> parseDir(String directory) {
//        return StringUtils.isEmpty(directory)
//                ? Option.none()
//                : Option.of(directory);
//    }
//
//    private static Option<Integer> parsePort(String value) {
//        return Try.of(()-> Integer.parseInt(value))
//                .onFailure(e -> log.log(Level.FINE, "no port specified", e))
//                .toOption();
//    }

    @Override
    public Descriptor<EmbeddedProvider> getDescriptor() {
        return Option.of(Jenkins.getInstanceOrNull())
                .map(j -> j.getDescriptorByType(DescriptorImpl.class))
                .getOrNull();
    }
    @Extension
    public static class DescriptorImpl extends EmbeddedProviderDescriptor {
        @Override
        public String getDisplayName() {
            return "Kafka Messaging";
        }
    }
}
