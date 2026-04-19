package com.metrohub.util;

public final class SmsTemplateBuilder {

    private SmsTemplateBuilder() {
    }

    

    public static String newDocumentSms(String fileName, String department, String priority) {
        String shortName = truncate(fileName, 30);
        return String.format(
                "[MetroHub] New %s-priority document uploaded to %s: \"%s\". Login to acknowledge.",
                priority, truncate(department, 20), shortName);
    }

    

    public static String deadlineApproachingSms(String fileName, long hoursRemaining) {
        return String.format(
                "[MetroHub] REMINDER: Document \"%s\" acknowledgement due in %dh. Please login and take action.",
                truncate(fileName, 35), hoursRemaining);
    }

    

    public static String deadlineOverdueSms(String fileName) {
        return String.format(
                "[MetroHub] OVERDUE: Document \"%s\" acknowledgement deadline has passed! Immediate action required.",
                truncate(fileName, 40));
    }

    

    public static String escalationSms(String fileName, int level) {
        return String.format(
                "[MetroHub] ESCALATION L%d: Document \"%s\" is unacknowledged. Immediate attention required.",
                level, truncate(fileName, 35));
    }

    

    public static String highPrioritySms(String fileName, String department) {
        return String.format(
                "[MetroHub] HIGH PRIORITY: New document \"%s\" uploaded to %s. Urgent acknowledgement needed.",
                truncate(fileName, 30), truncate(department, 20));
    }

    

    public static String acknowledgementSms(String fileName, String acknowledgedBy) {
        return String.format(
                "[MetroHub] Document \"%s\" acknowledged by %s successfully.",
                truncate(fileName, 35), truncate(acknowledgedBy, 20));
    }

    

    public static String violationSms(String fileName) {
        return String.format(
                "[MetroHub] VIOLATION: Compliance violation raised for document \"%s\". Action required.",
                truncate(fileName, 40));
    }

    

    public static String genericSms(String message) {
        return "[MetroHub] " + truncate(message, 140);
    }

    

    public static String buildSms(String alertType, String fileName, String department,
            String priority, long hoursRemaining, int escalationLevel) {
        return switch (alertType) {
            case "NEW_DOCUMENT_UPLOADED" -> newDocumentSms(fileName, department, priority);
            case "HIGH_PRIORITY_UPLOAD" -> highPrioritySms(fileName, department);
            case "DEADLINE_APPROACHING", "DEADLINE_TODAY" -> deadlineApproachingSms(fileName, hoursRemaining);
            case "DEADLINE_OVERDUE" -> deadlineOverdueSms(fileName);
            case "ESCALATION_LEVEL_1" -> escalationSms(fileName, 1);
            case "ESCALATION_LEVEL_2" -> escalationSms(fileName, 2);
            case "ESCALATION_LEVEL_3" -> escalationSms(fileName, 3);
            case "ACKNOWLEDGEMENT_REQUIRED" -> newDocumentSms(fileName, department, priority);
            case "VIOLATION" -> violationSms(fileName);
            default -> genericSms("You have a new notification. Please login to MetroHub.");
        };
    }

    private static String truncate(String text, int maxLen) {
        if (text == null)
            return "";
        return text.length() > maxLen ? text.substring(0, maxLen - 2) + ".." : text;
    }
}
