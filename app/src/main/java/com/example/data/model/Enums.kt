package com.example.data.model

enum class PlatformType(val label: String) {
    INSTAGRAM("Instagram"),
    YOUTUBE("YouTube"),
    THREADS("Threads"),
    FACEBOOK("Facebook"),
    OTHER("Other");

    companion object {
        fun fromName(name: String?): PlatformType =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}

enum class ClientType(val label: String) {
    NEW("New"),
    OLD("Old"),
    REGULAR("Regular");

    companion object {
        fun fromName(name: String?): ClientType =
            entries.firstOrNull { it.name == name } ?: NEW
    }
}

enum class CalendarStatus(val label: String) {
    DONE("Editing Done"),
    MISSED("Editing Missed"),
    NOT_MARKED("Not Marked");

    companion object {
        fun fromName(name: String?): CalendarStatus =
            entries.firstOrNull { it.name == name } ?: NOT_MARKED
    }
}

enum class VideoCategory(val label: String) {
    CLIENT_VIDEO("Client Video"),
    PERSONAL_VIDEO("Personal Video"),
    PORTFOLIO_VIDEO("Portfolio Video"),
    PRACTICE_VIDEO("Practice Video");

    companion object {
        fun fromName(name: String?): VideoCategory =
            entries.firstOrNull { it.name == name } ?: CLIENT_VIDEO
    }
}

enum class ProjectType(val label: String) {
    MOTION_GRAPHICS("Motion Graphics"),
    SAAS_ANIMATION("SaaS Animation"),
    GAMING("Gaming"),
    REELS_SHORTS("Reels / Shorts"),
    VLOGS("Vlogs"),
    OTHER("Other");

    companion object {
        fun fromName(name: String?): ProjectType =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}

enum class ProjectPriority(val label: String, val sortWeight: Int) {
    LOW("Low", 1),
    NORMAL("Normal", 2),
    HIGH("High", 3),
    URGENT("Urgent", 4);

    companion object {
        fun fromName(name: String?): ProjectPriority =
            entries.firstOrNull { it.name == name } ?: NORMAL
    }
}

enum class PaymentStatus(val label: String) {
    PAID("Paid"),
    UNPAID("Unpaid"),
    PARTIAL("Partial");

    companion object {
        fun fromName(name: String?): PaymentStatus =
            entries.firstOrNull { it.name == name } ?: UNPAID
    }
}

enum class ProjectStatus(val label: String) {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    companion object {
        fun fromName(name: String?): ProjectStatus =
            entries.firstOrNull { it.name == name } ?: PENDING
    }
}

enum class PaymentMethod(val label: String) {
    BANK_TRANSFER("Bank Transfer"),
    UPI("UPI"),
    CASH("Cash"),
    OTHER("Other");

    companion object {
        fun fromName(name: String?): PaymentMethod =
            entries.firstOrNull { it.name == name } ?: UPI
    }
}

enum class ExpenseCategory(val label: String) {
    SOFTWARE("Software"),
    SUBSCRIPTIONS("Subscriptions"),
    EDITING_ASSETS("Editing Assets"),
    INTERNET("Internet"),
    HARDWARE("Hardware"),
    OTHER("Other");

    companion object {
        fun fromName(name: String?): ExpenseCategory =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}

enum class ThemeMode(val label: String) {
    LIGHT("Light Mode"),
    DARK("Dark Mode"),
    SYSTEM("System Default");

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: LIGHT
    }
}

enum class DateFormatOption(val label: String, val example: String) {
    DD_MMM_YYYY("DD MMM YYYY", "06 Jul 2026"),
    DD_MM_YYYY("DD/MM/YYYY", "06/07/2026"),
    YYYY_MM_DD("YYYY-MM-DD", "2026-07-06");

    companion object {
        fun fromName(name: String?): DateFormatOption =
            entries.firstOrNull { it.name == name } ?: DD_MMM_YYYY
    }
}

enum class FinancialDateFilter(val label: String) {
    THIS_MONTH("This Month"),
    PREVIOUS_MONTH("Previous Month"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time"),
    CUSTOM("Custom Range")
}

enum class ClientSortOption(val label: String) {
    NAME("Name (A-Z)"),
    RECENT_ACTIVITY("Recent Activity")
}

enum class ProjectSortOption(val label: String) {
    DATE_DESC("Newest First"),
    DEADLINE_ASC("Deadline Soonest"),
    AMOUNT_DESC("Highest Amount"),
    PRIORITY_DESC("Highest Priority")
}

enum class NoteCategory(val label: String) {
    GENERAL("General"),
    CLIENT("Client Note"),
    PROJECT("Project Brief"),
    SCRIPT("Script / Hook"),
    IDEA("Creative Idea");

    companion object {
        fun fromName(name: String?): NoteCategory =
            entries.firstOrNull { it.name == name } ?: GENERAL
    }
}

enum class GoalMetric(val label: String) {
    PROJECTS("Completed Videos"),
    INCOME("Total Earnings"),
    CLIENTS("Total Clients"),
    EDITING_DAYS("Editing Days"),
    CUSTOM("Custom Milestone");

    companion object {
        fun fromName(name: String?): GoalMetric =
            entries.firstOrNull { it.name == name } ?: CUSTOM
    }
}

enum class GoalStatus(val label: String) {
    ACTIVE("Active"),
    COMPLETED("Completed"),
    PAUSED("Paused");

    companion object {
        fun fromName(name: String?): GoalStatus =
            entries.firstOrNull { it.name == name } ?: ACTIVE
    }
}

enum class RestoreMode(val label: String, val description: String) {
    MERGE_SKIP_DUPLICATES(
        "Merge & Skip Duplicates",
        "Keeps all existing records, imports new entries, and safely skips duplicates."
    ),
    REPLACE_ALL(
        "Replace All Data",
        "Deletes all current records and replaces them with the backup contents."
    )
}
