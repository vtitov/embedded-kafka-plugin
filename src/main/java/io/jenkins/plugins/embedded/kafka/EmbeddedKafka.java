package io.jenkins.plugins.embedded.kafka;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.plugins.embedded.EmbeddableService;
import io.vavr.API;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import io.vavr.control.Try;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import lombok.extern.java.Log;
import lombok.val;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Properties;
import java.util.logging.Level;

import static io.vavr.API.TODO;
import static io.vavr.API.Tuple;

@Log
public class EmbeddedKafka implements EmbeddableService, Serializable {

    private static final long serialVersionUID = -6076892741535318599L;

    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    transient private Option<KafkaServerStartable> kafkaServer = Option.none();
    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    transient private Option<ZooKeeperServer> zooKeeperServer = Option.none();

    private static final Integer ZOOKEEPER_TICK_TIME = 500;
    private static final Integer ZOOKEEPER_MAX_CONNECTIONS = 16;

    private static final String ZOOKEEPER_DATA_DIR = "dataDir";
    private static final String ZOOKEEPER_DATA_LOG_DIR = "dataLogDir";

    private static final String KAFKA_LOG_DIR = "log.dir";

    private static final Map<String,String> KAFKA_SETTINGS = HashMap.ofEntries(
        Tuple(ZOOKEEPER_DATA_DIR, "zookeeper-snapshot"),
        Tuple(ZOOKEEPER_DATA_LOG_DIR, "zookeeper-logs"),
        Tuple(KAFKA_LOG_DIR, "kafkla-log-dirs")
    );

    @Override
    public void close() throws Exception {
        stopKafka();
    }

    @Override
    public EmbeddableService stop() {
        Try.of(()-> {close(); return this;})
            .onFailure(e -> log.log(Level.FINE, "error while stopping kafka", e));
        return this;
    }

    public static EmbeddedKafka start(Integer brokerPort, String kafkaDir){
        return TODO();
    }

    //    @Override
    //    public EmbeddedKafka start(){
    //        return startKafka(
    //                Option.of(this.ZKPort).orElse(()-> Option.of(0)),
    //                Option.of(this.brokerPort).orElse(()-> Option.of(0)),
    //                Option.of(kafkaDir)
    //        );
    //    }

    private EmbeddedKafka stopKafka() {
        kafkaServer.forEach(KafkaServerStartable::shutdown);
        kafkaServer = Option.none();

        zooKeeperServer.forEach(ZooKeeperServer::shutdown); // TODO check if shutdown(true) should be used
        zooKeeperServer = Option.none();
        return this;
    }

    EmbeddedKafka startKafka(Option<Integer> oZkPort, Option<Integer> oBrokerPort, Option<String> oKafkaDir) {
        oBrokerPort.forEach(brokerPort -> {
            log.fine(String.format("starting kafka on port: %d", brokerPort));
            val kafkaDir = oKafkaDir //readKafkaDir()
                .map(kd -> new java.io.File(kd).toPath())
                // TODO add addShutdownHook(s)
                .orElse(() -> Try.of(() -> {
                    log.fine("creating kafka-jenkins temp dir");
                    return Files.createTempDirectory("kafka-jenkins");
                }).toOption());
            kafkaDir.map(baseDir -> Try.of(() ->
                    KAFKA_SETTINGS.map((k, v) -> {
                        val aPath = baseDir.resolve(v).toFile();
                        log.fine(String.format("creating dir: %s", aPath));
                        val rc = aPath.mkdirs();
                        if(!aPath.exists()) {
                            log.fine(String.format("couldn't create dir: %s", aPath));
                        }
                        return Tuple(k, aPath);
                    })
                )
                    .onFailure(e -> log.log(Level.FINE, "couldn't create kafka dirs", e))
                    .onSuccess(dirs ->
                        Try.of(() -> {
                                val zkPort = oZkPort.getOrElse(brokerPort - 1);
                                val tickTime = ZOOKEEPER_TICK_TIME;
                                val maxConnections = ZOOKEEPER_MAX_CONNECTIONS;
                                val snapshotDir = dirs.get(ZOOKEEPER_DATA_DIR).getOrNull();
                                val logDir = dirs.get(ZOOKEEPER_DATA_LOG_DIR).getOrNull();

                                ZooKeeperServer zkServer = new ZooKeeperServer(snapshotDir, logDir, tickTime);
                                val factory = NIOServerCnxnFactory.createFactory();
                                factory.configure(new InetSocketAddress("localhost", zkPort), maxConnections);
                                factory.startup(zkServer);
                                zooKeeperServer = API.Option(zkServer);

                                val zookeeperString = "localhost:" + zkPort;
                                val brokerString = "localhost:" + brokerPort;
                                val zkMaxConnections = maxConnections;
                                val kafkaBrokerConfig = new Properties();
                                kafkaBrokerConfig.setProperty("zookeeper.connect", zookeeperString);
                                kafkaBrokerConfig.setProperty("broker.id", "1");
                                kafkaBrokerConfig.setProperty("offsets.topic.replication.factor", "1");
                                kafkaBrokerConfig.setProperty("host.name", "localhost");
                                kafkaBrokerConfig.setProperty("port", Integer.toString(brokerPort));
                                kafkaBrokerConfig.setProperty("log.dir", logDir.getAbsolutePath());
                                kafkaBrokerConfig.setProperty("log.flush.interval.messages", String.valueOf(1));
                                kafkaBrokerConfig.setProperty("delete.topic.enable", String.valueOf(true));
                                kafkaBrokerConfig.setProperty("auto.create.topics.enable", String.valueOf(true));

                                Thread.currentThread().setContextClassLoader(null);
                                val broker = new KafkaServerStartable(new KafkaConfig(kafkaBrokerConfig));
                                broker.startup();
                                kafkaServer = API.Option(broker);
                                log.fine(String.format("kafka succesfully started at: [%s] [%s]",
                                        zookeeperString, brokerString));
                                return Tuple(zkServer, broker);
                            }
                        ).onFailure(e -> log.log(Level.FINE, "couldn't start kafka", e))
                    )
            );
        });
        return this;
    }

}
