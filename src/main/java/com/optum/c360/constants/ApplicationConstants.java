package com.optum.c360.constants;

public final class ApplicationConstants {

    public static final String SCHEDULER_TIME_INTERVAL = "scheduler.timeInterval";
    public static final String FIRST_RUN_TIME = "first-run-time";
    public static final String FIRST_RUN_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String SOURCE_NAME = "source.name";
    public static final String APPLICATION_ID = "application-id";
    public static final String SUBJECT_AREAS = "elastic.subject-areas";

    public static final String FILE_STORAGE_LOCATION = "file-storage-location";
    public static final String FILE_NAME_PREFIX = "file-name-prefix";
    public static final String KEYSPACE_NAME = "keyspace.name";
    public static final String APPLICATIONS_RUNTIMES_TABLE_NAME = "applications-last-runtimes-table-name";

    public static final String STRING_SUBJECT_AREA = "Subject Area";
    public static final String STRING_DASHBOARD = "DASHBOARD";
    public static final String STRING_DAILY_SUCCESS_COUNT = "Daily Success Count";
    public static final String STRING_DAILY_ERROR_COUNT = "Daily Error Count";
    public static final String STRING_ERROR_MESSAGE = "Error Message";
    public static final String STRING_COUNT = "Count     ";
    public static final String STRING_TOTAL_SUCCESS_COUNT = "Total Success Count";
    public static final String STRING_TOTAL_ERROR_COUNT ="Total Error Count";

    public static final String STRING_FIELD_SOURCE_NAME = "sourceName";
    public static final String STRING_FIELD_SUBJECT_AREA = "subjectArea";
    public static final String STRING_FIELD_TIMESTAMP = "@timestamp";
    public static final String STRING_LINE = "------------------------------------------------------";

    public static final String STRING_STEPS_TO_BE_TAKEN = "Action to be taken";

    public static final String HTML_BREAK = "<br />";
    public static final String HTML_TAB = "&nbsp;&nbsp;&nbsp;&nbsp;";
    public static final String BOLD_START = "<b>";
    public static final String BOLD_END = "</b>";
    public static final String HTML_UL_START = "<ul>";
    public static final String HTML_UL_END = "</ul>";
    public static final String HTML_LI_START = "<li>";
    public static final String HTML_LI_END = "</li>";
    public static final String STRING_ERROR_DICTIONARY = "Error Dictionary";
    public static final String ERROR_DICTIONARY_FILE_PATH = "error-dictionary-file-path";
    public static final Object STRING_DATE = "Date              ";

    private ApplicationConstants() {
    }
}
