package com.metrohub.util;

public final class EmailTemplateBuilder {

    private static final String HEADER_COLOR = "#0B3C5D";
    private static final String SUCCESS_COLOR = "#1E7E34";
    private static final String WARNING_COLOR = "#E65100";
    private static final String DANGER_COLOR = "#C62828";

    private EmailTemplateBuilder() {
    }

    

    public static String buildEmail(String recipientName, String subject,
            String bodyHtml, String alertType) {
        String accentColor = switch (alertType) {
            case "HIGH_PRIORITY_UPLOAD", "DEADLINE_OVERDUE" -> DANGER_COLOR;
            case "DEADLINE_APPROACHING", "DEADLINE_TODAY" -> WARNING_COLOR;
            case "ACKNOWLEDGEMENT_REQUIRED" -> WARNING_COLOR;
            default -> HEADER_COLOR;
        };

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0;padding:0;background-color:#F5F7FA;font-family:Arial,Helvetica,sans-serif;">
                    <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#F5F7FA;padding:20px 0;">
                        <tr><td align="center">
                            <table width="600" cellpadding="0" cellspacing="0" style="background-color:#FFFFFF;border:1px solid #D0D7DE;border-radius:4px;overflow:hidden;">

                                <!-- Government Band -->
                                <tr>
                                    <td style="background-color:#082F4A;color:rgba(255,255,255,0.8);font-size:11px;text-align:center;padding:6px 20px;">
                                        भारत सरकार | Government of India | Ministry of Housing &amp; Urban Affairs
                                    </td>
                                </tr>

                                <!-- Header -->
                                <tr>
                                    <td style="background-color:%s;padding:20px 30px;">
                                        <table width="100%%" cellpadding="0" cellspacing="0">
                                            <tr>
                                                <td style="color:#FFFFFF;">
                                                    <h1 style="margin:0;font-size:22px;letter-spacing:0.08em;font-weight:700;">METROHUB</h1>
                                                    <p style="margin:4px 0 0;font-size:12px;color:rgba(255,255,255,0.7);">Document Management &amp; Compliance System</p>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- Accent Line -->
                                <tr>
                                    <td style="background-color:%s;height:3px;"></td>
                                </tr>

                                <!-- Body -->
                                <tr>
                                    <td style="padding:30px;">
                                        <p style="margin:0 0 20px;font-size:14px;color:#333333;">Dear <strong>%s</strong>,</p>
                                        %s
                                        <p style="margin:20px 0 0;font-size:14px;color:#333333;">
                                            Please <a href="http://localhost:3000/login" style="color:%s;text-decoration:none;font-weight:600;">log in to MetroHub</a>
                                            to view and take action.
                                        </p>
                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td style="background-color:#F8FAFB;padding:20px 30px;border-top:1px solid #D0D7DE;">
                                        <p style="margin:0;font-size:11px;color:#999999;">
                                            This is an automated message from MetroHub. Please do not reply to this email.
                                        </p>
                                        <p style="margin:8px 0 0;font-size:11px;color:#999999;">
                                            Metro Rail Authority | IT Support: support@metrohub.in | Helpline: 1800-XXX-XXXX
                                        </p>
                                        <p style="margin:8px 0 0;font-size:10px;color:#CCCCCC;">
                                            Secured under IT Act, 2000 &amp; IT (Amendment) Act, 2008
                                        </p>
                                    </td>
                                </tr>

                            </table>
                        </td></tr>
                    </table>
                </body>
                </html>
                """
                .formatted(HEADER_COLOR, accentColor, recipientName, bodyHtml, HEADER_COLOR);
    }

    /**
     * Build body HTML for new document notification.
     */
    public static String newDocumentBody(String fileName, String department,
            String priority, String docType) {
        return """
                <div style="background-color:#F8FAFB;border:1px solid #D0D7DE;border-radius:4px;padding:16px;margin:10px 0;">
                    <p style="margin:0 0 8px;font-size:13px;color:#666666;">A new document has been uploaded to your department and requires your attention.</p>
                    <table width="100%%" cellpadding="0" cellspacing="0" style="font-size:13px;">
                        <tr><td style="padding:4px 0;color:#999999;width:100px;">Document:</td><td style="padding:4px 0;color:#333333;font-weight:600;">%s</td></tr>
                        <tr><td style="padding:4px 0;color:#999999;">Department:</td><td style="padding:4px 0;color:#333333;">%s</td></tr>
                        <tr><td style="padding:4px 0;color:#999999;">Type:</td><td style="padding:4px 0;color:#333333;">%s</td></tr>
                        <tr><td style="padding:4px 0;color:#999999;">Priority:</td><td style="padding:4px 0;color:%s;font-weight:600;">%s</td></tr>
                    </table>
                </div>
                """
                .formatted(fileName, department, docType,
                        priorityColor(priority), priority);
    }

    /**
     * Build body HTML for deadline notification.
     */
    public static String deadlineBody(String fileName, String department,
            long hoursRemaining) {
        String urgency = hoursRemaining <= 0 ? "OVERDUE"
                : hoursRemaining <= 24 ? "Due Soon" : hoursRemaining + " hours remaining";
        String urgencyColor = hoursRemaining <= 0 ? DANGER_COLOR : hoursRemaining <= 24 ? WARNING_COLOR : HEADER_COLOR;
        return """
                <div style="background-color:#FFF3E0;border:1px solid #FFE0B2;border-radius:4px;padding:16px;margin:10px 0;">
                    <p style="margin:0 0 8px;font-size:13px;color:#333333;">
                        <strong style="color:%s;">%s</strong> — Acknowledgement deadline for the following document:
                    </p>
                    <table width="100%%" cellpadding="0" cellspacing="0" style="font-size:13px;">
                        <tr><td style="padding:4px 0;color:#999999;width:100px;">Document:</td><td style="padding:4px 0;color:#333333;font-weight:600;">%s</td></tr>
                        <tr><td style="padding:4px 0;color:#999999;">Department:</td><td style="padding:4px 0;color:#333333;">%s</td></tr>
                    </table>
                </div>
                """
                .formatted(urgencyColor, urgency, fileName, department);
    }

    /**
     * Build body HTML for escalation notification.
     */
    public static String escalationBody(String fileName, String department,
            int escalationLevel) {
        return """
                <div style="background-color:#FFEBEE;border:1px solid #FFCDD2;border-radius:4px;padding:16px;margin:10px 0;">
                    <p style="margin:0 0 8px;font-size:13px;color:%s;font-weight:600;">
                        Escalation Level %d — Immediate attention required
                    </p>
                    <p style="margin:0 0 8px;font-size:13px;color:#333333;">
                        This document has been escalated due to missed acknowledgement deadline.
                    </p>
                    <table width="100%%" cellpadding="0" cellspacing="0" style="font-size:13px;">
                        <tr><td style="padding:4px 0;color:#999999;width:100px;">Document:</td><td style="padding:4px 0;color:#333333;font-weight:600;">%s</td></tr>
                        <tr><td style="padding:4px 0;color:#999999;">Department:</td><td style="padding:4px 0;color:#333333;">%s</td></tr>
                    </table>
                </div>
                """
                .formatted(DANGER_COLOR, escalationLevel, fileName, department);
    }

    /**
     * Build body HTML for acknowledgement confirmation.
     */
    public static String acknowledgementBody(String fileName, String department,
            String acknowledgedBy) {
        return """
                <div style="background-color:#E8F5E9;border:1px solid #C8E6C9;border-radius:4px;padding:16px;margin:10px 0;">
                    <p style="margin:0 0 8px;font-size:13px;color:%s;font-weight:600;">
                        Document Acknowledged Successfully
                    </p>
                    <table width="100%%" cellpadding="0" cellspacing="0" style="font-size:13px;">
                        <tr><td style="padding:4px 0;color:#999999;width:100px;">Document:</td><td style="padding:4px 0;color:#333333;font-weight:600;">%s</td></tr>
                        <tr><td style="padding:4px 0;color:#999999;">Department:</td><td style="padding:4px 0;color:#333333;">%s</td></tr>
                        <tr><td style="padding:4px 0;color:#999999;">Acknowledged By:</td><td style="padding:4px 0;color:#333333;">%s</td></tr>
                    </table>
                </div>
                """
                .formatted(SUCCESS_COLOR, fileName, department, acknowledgedBy);
    }

    private static String priorityColor(String priority) {
        return switch (priority != null ? priority.toUpperCase() : "") {
            case "CRITICAL" -> DANGER_COLOR;
            case "HIGH" -> WARNING_COLOR;
            case "MEDIUM" -> "#FF9800";
            default -> "#666666";
        };
    }
}
