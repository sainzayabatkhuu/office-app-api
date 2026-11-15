package com.sol.office.common.event;

/**
 * RabbitMQ topology shared between producers and consumers.
 */
public final class MessagingConstants {

    /** Topic exchange for user-facing notifications; any service may publish. */
    public static final String NOTIFICATION_EXCHANGE = "office.notification";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.user";

    /** Topic exchange for report-service internal work distribution. */
    public static final String REPORT_EXCHANGE = "office.report";
    public static final String REPORT_EXECUTION_ROUTING_KEY = "report.execution.requested";

    public static final String DEAD_LETTER_EXCHANGE = "office.dlx";

    private MessagingConstants() {
    }
}
