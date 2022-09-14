package eu.xenit.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

// https://gist.github.com/hitenpratap/14f7168dc65a7a4b46ce2f7aeda81ab9
public class CustomLog4jAppender extends FileAppender {

    private static final int TOP_OF_TROUBLE = -1;
    private static final int TOP_OF_MINUTE = 0;
    private static final int TOP_OF_HOUR = 1;
    private static final int HALF_DAY = 2;
    private static final int TOP_OF_DAY = 3;
    private static final int TOP_OF_WEEK = 4;
    private static final int TOP_OF_MONTH = 5;

    private String datePattern = "'.'yyyy-MM-dd-HH:mm";
    private String compressBackups = "false";
    private String maxNumberOfDays = "7";
    private String scheduledFilename;
    private long nextCheck = System.currentTimeMillis() - 1;
    private Date now = new Date();
    private SimpleDateFormat sdf;
    private RollingCalendar rc = new RollingCalendar();

    private static final TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT");

    public CustomLog4jAppender() {
    }

    public CustomLog4jAppender(Layout layout, String filename, String datePattern) throws IOException {
        super(layout, filename, true);
        this.datePattern = datePattern;
        activateOptions();
    }

    public void setDatePattern(String pattern) {
        datePattern = pattern;
    }

    public String getDatePattern() {
        return datePattern;
    }

    @Override
    public void activateOptions() {
        super.activateOptions();
        if (datePattern != null && fileName != null) {
            now.setTime(System.currentTimeMillis());
            sdf = new SimpleDateFormat(datePattern);
            int type = computeCheckPeriod();
            printPeriodicity(type);
            rc.setType(type);
            File file = new File(fileName);
            scheduledFilename = fileName + sdf.format(new Date(file.lastModified()));
        } else {
            LogLog.error("Either File or DatePattern options are not set for appender [" + name + "].");
        }
    }

    private void printPeriodicity(int type) {
        String appender = "Log4J Appender: ";
        switch (type) {
            case TOP_OF_MINUTE:
                LogLog.debug(appender + name + " to be rolled every minute.");
                break;
            case TOP_OF_HOUR:
                LogLog.debug(appender + name + " to be rolled on top of every hour.");
                break;
            case HALF_DAY:
                LogLog.debug(appender + name + " to be rolled at midday and midnight.");
                break;
            case TOP_OF_DAY:
                LogLog.debug(appender + name + " to be rolled at midnight.");
                break;
            case TOP_OF_WEEK:
                LogLog.debug(appender + name + " to be rolled at start of week.");
                break;
            case TOP_OF_MONTH:
                LogLog.debug(appender + name + " to be rolled at start of every month.");
                break;
            default:
                LogLog.warn("Unknown periodicity for appender [" + name + "].");
        }
    }

    private int computeCheckPeriod() {
        RollingCalendar rollingCalendar = new RollingCalendar(gmtTimeZone, Locale.ENGLISH);
        Date epoch = new Date(0);
        if (datePattern != null) {
            for (int i = TOP_OF_MINUTE; i <= TOP_OF_MONTH; i++) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
                simpleDateFormat.setTimeZone(gmtTimeZone);
                String r0 = simpleDateFormat.format(epoch);
                rollingCalendar.setType(i);
                Date next = new Date(rollingCalendar.getNextCheckMillis(epoch));
                String r1 = simpleDateFormat.format(next);
                if (!r0.equals(r1)) {
                    return i;
                }
            }
        }
        return TOP_OF_TROUBLE;
    }

    private void rollOver() throws IOException {
        if (datePattern == null) {
            errorHandler.error("Missing DatePattern option in rollOver().");
            return;
        }
        String datedFilename = fileName + sdf.format(now);
        if (scheduledFilename.equals(datedFilename)) {
            return;
        }
        this.closeFile();
        File target = new File(scheduledFilename);
        if (target.exists()) {
            Files.delete(target.toPath());
        }
        File file = new File(fileName);
        boolean result = file.renameTo(target);
        if (result) {
            LogLog.debug(fileName + " -> " + scheduledFilename);
        } else {
            LogLog.error("Failed to rename [" + fileName + "] to [" + scheduledFilename + "].");
        }
        try {
            this.setFile(fileName, false, this.bufferedIO, this.bufferSize);
        } catch (IOException e) {
            errorHandler.error("setFile(" + fileName + ", false) call failed.");
        }
        scheduledFilename = datedFilename;
    }

    @Override
    protected void subAppend(LoggingEvent event) {
        long n = System.currentTimeMillis();
        if (n >= nextCheck) {
            now.setTime(n);
            nextCheck = rc.getNextCheckMillis(now);
            try {
                cleanupAndRollOver();
            } catch (IOException ioe) {
                LogLog.error("cleanupAndRollover() failed.", ioe);
            }
        }
        super.subAppend(event);
    }

    public String getCompressBackups() {
        return compressBackups;
    }

    public void setCompressBackups(String compressBackups) {
        this.compressBackups = compressBackups;
    }

    public String getMaxNumberOfDays() {
        return maxNumberOfDays;
    }

    public void setMaxNumberOfDays(String maxNumberOfDays) {
        this.maxNumberOfDays = maxNumberOfDays;
    }

    protected void cleanupAndRollOver() throws IOException {
        File file = new File(fileName);
        Calendar cal = Calendar.getInstance();
        int maxDays = 7;
        try {
            maxDays = Integer.parseInt(getMaxNumberOfDays());
        } catch (Exception e) {
            // just leave it at 7.
        }
        cal.add(Calendar.DATE, -maxDays);
        Date cutoffDate = cal.getTime();
        if (file.getParentFile().exists()) {
            File[] files = file.getParentFile().listFiles(new StartsWithFileFilter(file.getName(), false));
            int nameLength = file.getName().length();
            for (File value : Optional.ofNullable(files).orElse(new File[0])) {
                String datePart;
                try {
                    datePart = value.getName().substring(nameLength);
                    Date date = sdf.parse(datePart);
                    if (date.before(cutoffDate)) {
                        Files.delete(value.toPath());
                    } else if (getCompressBackups().equalsIgnoreCase("YES") || getCompressBackups().equalsIgnoreCase(
                            "TRUE")) {
                        zipAndDelete(value);
                    }
                } catch (Exception pe) {
                    // This isn't a file we should touch (it isn't named correctly)
                }
            }
        }
        rollOver();
    }

    private void zipAndDelete(File file) throws IOException {
        if (!file.getName().endsWith(".zip")) {
            File zipFile = new File(file.getParent(), file.getName() + ".zip");
            try (FileInputStream fis = new FileInputStream(file);
                    FileOutputStream fos = new FileOutputStream(zipFile);
                    ZipOutputStream zos = new ZipOutputStream(fos)) {
                ZipEntry zipEntry = new ZipEntry(file.getName());
                zos.putNextEntry(zipEntry);
                byte[] buffer = new byte[4096];
                while (true) {
                    int bytesRead = fis.read(buffer);
                    if (bytesRead == -1) {
                        break;
                    } else {
                        zos.write(buffer, 0, bytesRead);
                    }
                }
                zos.closeEntry();
            }
            Files.delete(file.toPath());
        }
    }

    class StartsWithFileFilter implements FileFilter {

        private String startsWith;
        private boolean inclDirs;

        StartsWithFileFilter(String startsWith, boolean includeDirectories) {
            super();
            this.startsWith = startsWith.toUpperCase();
            inclDirs = includeDirectories;
        }

        public boolean accept(File pathname) {
            if (!inclDirs && pathname.isDirectory()) {
                return false;
            } else {
                return pathname.getName().toUpperCase().startsWith(startsWith);
            }
        }
    }

    class RollingCalendar extends GregorianCalendar {

        private static final long serialVersionUID = -3560331770601814177L;

        int type = CustomLog4jAppender.TOP_OF_TROUBLE;

        RollingCalendar() {
            super();
        }

        RollingCalendar(TimeZone tz, Locale locale) {
            super(tz, locale);
        }

        void setType(int type) {
            this.type = type;
        }

        long getNextCheckMillis(Date now) {
            return getNextCheckDate(now).getTime();
        }

        Date getNextCheckDate(Date now) {
            this.setTime(now);

            switch (type) {
                case CustomLog4jAppender.TOP_OF_MINUTE:
                    this.set(Calendar.SECOND, 0);
                    this.set(Calendar.MILLISECOND, 0);
                    this.add(Calendar.MINUTE, 1);
                    break;
                case CustomLog4jAppender.TOP_OF_HOUR:
                    this.set(Calendar.MINUTE, 0);
                    this.set(Calendar.SECOND, 0);
                    this.set(Calendar.MILLISECOND, 0);
                    this.add(Calendar.HOUR_OF_DAY, 1);
                    break;
                case CustomLog4jAppender.HALF_DAY:
                    this.set(Calendar.MINUTE, 0);
                    this.set(Calendar.SECOND, 0);
                    this.set(Calendar.MILLISECOND, 0);
                    int hour = get(Calendar.HOUR_OF_DAY);
                    if (hour < 12) {
                        this.set(Calendar.HOUR_OF_DAY, 12);
                    } else {
                        this.set(Calendar.HOUR_OF_DAY, 0);
                        this.add(Calendar.DAY_OF_MONTH, 1);
                    }
                    break;
                case CustomLog4jAppender.TOP_OF_DAY:
                    this.set(Calendar.HOUR_OF_DAY, 0);
                    this.set(Calendar.MINUTE, 0);
                    this.set(Calendar.SECOND, 0);
                    this.set(Calendar.MILLISECOND, 0);
                    this.add(Calendar.DATE, 1);
                    break;
                case CustomLog4jAppender.TOP_OF_WEEK:
                    this.set(Calendar.DAY_OF_WEEK, getFirstDayOfWeek());
                    this.set(Calendar.HOUR_OF_DAY, 0);
                    this.set(Calendar.MINUTE, 0);
                    this.set(Calendar.SECOND, 0);
                    this.set(Calendar.MILLISECOND, 0);
                    this.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case CustomLog4jAppender.TOP_OF_MONTH:
                    this.set(Calendar.DATE, 1);
                    this.set(Calendar.HOUR_OF_DAY, 0);
                    this.set(Calendar.MINUTE, 0);
                    this.set(Calendar.SECOND, 0);
                    this.set(Calendar.MILLISECOND, 0);
                    this.add(Calendar.MONTH, 1);
                    break;
                default:
                    throw new IllegalStateException("Unknown periodicity type.");
            }
            return getTime();
        }
    }
}