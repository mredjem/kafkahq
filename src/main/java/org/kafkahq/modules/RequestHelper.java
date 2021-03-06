package org.kafkahq.modules;

import com.google.inject.Binder;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Request;
import org.kafkahq.controllers.AbstractController;
import org.kafkahq.controllers.TopicController;
import org.kafkahq.repositories.RecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class RequestHelper implements Jooby.Module {
    private transient static final Logger logger = LoggerFactory.getLogger(TopicController.class);

    public static RecordRepository.Options buildRecordRepositoryOptions(Request request, String cluster, String topic) {
        RecordRepository.Options options = new RecordRepository.Options(cluster, topic);

        request.param("after").toOptional().ifPresent(options::setAfter);
        request.param("partition").toOptional(Integer.class).ifPresent(options::setPartition);
        request.param("sort").toOptional(RecordRepository.Options.Sort.class).ifPresent(options::setSort);
        request.param("timestamp").toOptional(String.class).ifPresent(s -> options.setTimestamp(Instant.parse(s).toEpochMilli()));
        request.param("search").toOptional(String.class).ifPresent(options::setSearch);

        return options;
    }

    public static List<org.kafkahq.models.Config> updatedConfigs(Request request, List<org.kafkahq.models.Config> configs) {
        return configs
            .stream()
            .filter(config -> !config.isReadOnly())
            .filter(config -> !(config.getValue() == null ? "" : config.getValue()).equals(request.param("configs[" + config.getName() + "]").value()))
            .map(config -> config.withValue(request.param("configs[" + config.getName() + "]").value()))
            .collect(Collectors.toList());
    }

    public static AbstractController.Toast runnableToToast(ResultStatusResponseRunnable callable, String successMessage, String failedMessage) {
        AbstractController.Toast.ToastBuilder builder = AbstractController.Toast.builder();

        try {
            callable.run();
            builder
                .message(successMessage)
                .type(AbstractController.Toast.Type.success);
        } catch (Exception exception) {
            String cause = exception.getCause() != null ? exception.getCause().getMessage() : exception.getMessage();

            builder
                .title(failedMessage)
                .message(exception.getCause() != null ? exception.getCause().getMessage() : exception.getMessage())
                .type(AbstractController.Toast.Type.error);

            logger.error(failedMessage != null ? failedMessage : cause, exception);
        }

        return builder.build();
    }

    public interface ResultStatusResponseRunnable {
        void run() throws Exception;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void configure(Env env, Config conf, Binder binder) {
        binder.bind(RequestHelper.class).toInstance(new RequestHelper());
    }
}
