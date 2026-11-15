package com.sol.office.report.messaging;

import com.sol.office.report.config.RabbitConfig;
import com.sol.office.report.service.ReportExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Worker side of ASYNC reports. Every report-service instance consumes from the same queue, so adding
 * instances adds report capacity.
 */
@Component
@RequiredArgsConstructor
public class ReportExecutionListener {

    private final ReportExecutor executor;

    @RabbitListener(queues = RabbitConfig.EXECUTION_QUEUE, containerFactory = RabbitConfig.EXECUTION_LISTENER_FACTORY)
    public void onExecutionRequested(ExecutionRequestedMessage message) {
        executor.execute(message.executionId());
    }
}
