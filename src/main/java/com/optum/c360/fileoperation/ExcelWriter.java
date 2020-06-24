package com.optum.c360.fileoperation;

import com.optum.c360.constants.ApplicationConstants;
import com.optum.c360.elastic.ErrorDetails;
import com.optum.c360.elastic.TopicStatistics;
import com.optum.c360.exception.GenericException;
import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ExcelWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelWriter.class);

    @Autowired
    Environment environment;

    @Autowired
    ResourceLoader resourceLoader;

    public File write(List<TopicStatistics> stats) {
        File file = null;
        try {
            if (null != stats) {
                file = saveFile(writeIfNotNull(stats));
            }
        } catch (Exception e) {
            throw new GenericException(e.getMessage(), e);
        }
        return file;
    }

    private Workbook writeIfNotNull(List<TopicStatistics> stats) {
        Workbook wb = new XSSFWorkbook();

        createDashboard(stats, wb);
        int sheetIndex = 1;
        for (TopicStatistics stat : stats) {

            TempFileData tempFileData = new TempFileData();

            tempFileData.topicStatistics = stat;
            tempFileData.sheet = wb.createSheet(stat.getTopic());
            tempFileData.sheetToColor = ((XSSFWorkbook) wb).getSheetAt(sheetIndex);

            createErrorHeadingRow(wb, tempFileData.sheet);

            tempFileData.index = 2;
            tempFileData.row = tempFileData.sheet.createRow((short) 1);
            if (null != stat.getDates() && !stat.getDates().isEmpty()) {
                processSheetData(tempFileData);
            } else {
                Cell noDataFoundCell0 = createCell(tempFileData.row, (short) 0);
                noDataFoundCell0.setCellValue(new XSSFRichTextString("No Data found in elastic"));
                createCell(tempFileData.row, (short) 1);
                createCell(tempFileData.row, (short) 2);
                createCell(tempFileData.row, (short) 3);
                createCell(tempFileData.row, (short) 4);
                tempFileData.sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 4));
                CellUtil.setCellStyleProperty(noDataFoundCell0, CellUtil.VERTICAL_ALIGNMENT, VerticalAlignment.CENTER);
                CellUtil.setCellStyleProperty(noDataFoundCell0, CellUtil.ALIGNMENT, HorizontalAlignment.CENTER);
            }
            sheetIndex++;

            applyColumnWidth(tempFileData.sheet);
            applyTabColor(tempFileData);
        }

        //writeErrorDictionary(wb);
        return wb;
    }

    private void processSheetData(TempFileData tempFileData) {
        TopicStatistics stat = tempFileData.topicStatistics;
        for (String date : stat.getDates()) {
            List<ErrorDetails> errorList = stat.getErrorMap().get(date);

            Cell cellDate = createCell(tempFileData.row, (short) 0);

            cellDate.setCellValue(new XSSFRichTextString(date));
            Cell cellDailySuccessCount = createCell(tempFileData.row, (short) 1);
            Cell cellDailyErrorCount = createCell(tempFileData.row, (short) 2);

            TempSheetData tempSheetData = new TempSheetData();

            if (null != errorList) {
                tempSheetData.totalDistinctErrors = errorList.size();
                for (ErrorDetails errorObj : errorList) {

                    tempSheetData.dailyErrorCount += errorObj.getCount();
                    tempSheetData.errorCountUpdated = true;

                    createCell(tempFileData.row, (short) 0);
                    createCell(tempFileData.row, (short) 1);
                    createCell(tempFileData.row, (short) 2);

                    Cell cellErrorMessage = createCell(tempFileData.row, (short) 3);
                    cellErrorMessage.setCellValue(new XSSFRichTextString(errorObj.getErrorMessage()));

                    Cell cellErrorCount = createCell(tempFileData.row, (short) 4);
                    cellErrorCount.setCellValue(errorObj.getCount());

                    if (tempFileData.index > 1) {
                        tempFileData.row = tempFileData.sheet.createRow((short) tempFileData.index);
                        tempSheetData.rowUpdated =true ;
                    }
                    tempFileData.index++;
                }
            } else {
                createCell(tempFileData.row, (short) 0);
                createCell(tempFileData.row, (short) 1);
                createCell(tempFileData.row, (short) 2);

                Cell cellErrorMessage = createCell(tempFileData.row, (short) 3);
                cellErrorMessage.setCellValue(new XSSFRichTextString("NA"));

                Cell cellErrorCount = createCell(tempFileData.row, (short) 4);
                cellErrorCount.setCellValue(new XSSFRichTextString("NA"));
            }

            updateMergedRegion(tempFileData, tempSheetData);

            cellDate.setCellValue(new XSSFRichTextString(date));

            updateCell(tempFileData, tempSheetData, date, cellDailyErrorCount, cellDailySuccessCount);
        }
    }

    private void updateCell(TempFileData tempFileData, TempSheetData tempSheetData, String date, Cell cellDailyErrorCount, Cell cellDailySuccessCount) {
        if (tempSheetData.errorCountUpdated) {
            tempFileData.applyTabColor = true;
            tempFileData.startRow += tempSheetData.totalDistinctErrors;
            cellDailyErrorCount.setCellValue(tempSheetData.dailyErrorCount);
        } else {
            tempFileData.startRow += 1;
            cellDailyErrorCount.setCellValue(new XSSFRichTextString("NA"));
        }

        setSuccessColumnData(tempFileData.topicStatistics, date, cellDailySuccessCount);

        if (!tempSheetData.rowUpdated) {
            tempFileData.row = tempFileData.sheet.createRow((short) tempFileData.index);
            tempFileData.index++;
        }
    }

    private void updateMergedRegion(TempFileData tempFileData, TempSheetData tempSheetData) {
        if (tempSheetData.totalDistinctErrors > 1) {
            tempFileData.sheet.addMergedRegion(new CellRangeAddress(tempFileData.startRow, tempFileData.startRow + tempSheetData.totalDistinctErrors - 1, tempFileData.startCol, tempFileData.endCol));
            tempFileData.sheet.addMergedRegion(new CellRangeAddress(tempFileData.startRow, tempFileData.startRow + tempSheetData.totalDistinctErrors - 1, tempFileData.startCol + 1, tempFileData.endCol + 1));
            tempFileData.sheet.addMergedRegion(new CellRangeAddress(tempFileData.startRow, tempFileData.startRow + tempSheetData.totalDistinctErrors - 1, tempFileData.startCol + 2, tempFileData.endCol + 2));
        }
    }

    private void setSuccessColumnData(TopicStatistics stat, String date, Cell cellDailySuccessCount) {
        if (null != stat.getSuccessMap() && null != stat.getSuccessMap().get(stat.getTopic())) {
            Map<String, Long> successMap = stat.getSuccessMap().get(stat.getTopic());

            if (null != successMap && null != successMap.get(date)) {
                cellDailySuccessCount.setCellValue(successMap.get(date));
            } else {
                cellDailySuccessCount.setCellValue(new XSSFRichTextString("NA"));
            }
        } else {
            cellDailySuccessCount.setCellValue(new XSSFRichTextString("NA"));
        }
    }

    private void applyColumnWidth(Sheet sheet) {
        try {
            int maxLengthCol0 = getLength(0, ApplicationConstants.STRING_DATE);
            int maxLengthCol1 = getLength(0, ApplicationConstants.STRING_DAILY_SUCCESS_COUNT);
            int maxLengthCol2 = getLength(0, ApplicationConstants.STRING_DAILY_ERROR_COUNT);
            int maxLengthCol4 = getLength(0, ApplicationConstants.STRING_COUNT);

            int width0 = ((int) (maxLengthCol0 * 1.14388)) * 256;
            int width1 = ((int) (maxLengthCol1 * 1.14388)) * 256;
            int width2 = ((int) (maxLengthCol2 * 1.14388)) * 256;
            int width3 = ((int) (50 * 1.14388)) * 256;
            int width4 = ((int) (maxLengthCol4 * 1.14388)) * 256;
            sheet.setColumnWidth(0, width0);
            sheet.setColumnWidth(1, width1);
            sheet.setColumnWidth(2, width2);
            sheet.setColumnWidth(3, width3);
            sheet.setColumnWidth(4, width4);
        } catch (Exception e) {
            LOGGER.info("Exception while getting default width", e);
        }
    }

    private void applyTabColor(TempFileData tempFileData) {
        if (tempFileData.applyTabColor) {
            tempFileData.sheetToColor.setTabColor(new XSSFColor(java.awt.Color.RED, null));
        } else {
            tempFileData.sheetToColor.setTabColor(new XSSFColor(java.awt.Color.GREEN, null));
        }
    }

    private File saveFile(Workbook wb) {
        File file;
        try {
            String dirPath = environment.getProperty("FILE_STORAGE_LOCATION");
            File dir = FileUtils.getFile(dirPath);
            dir.mkdirs();

            String fileName = dir.getAbsolutePath() + File.separator + environment.getProperty(ApplicationConstants.FILE_NAME_PREFIX) + ".xlsx";
            file = FileUtils.getFile(fileName);
            writeFile(wb, file);
        } catch (Exception e) {
            throw new GenericException("Exception while saving file", e);
        }
        return file;
    }

    private void writeFile(Workbook wb, File file) {
        try (OutputStream os = Files.newOutputStream(file.toPath())) {
            wb.write(os);
        } catch (Exception e) {
            throw new GenericException("Exception while writing file", e);
        }
    }

    private void createDashboard(List<TopicStatistics> stats, Workbook wb) {
        Sheet sheet = wb.createSheet(ApplicationConstants.STRING_DASHBOARD);
        createDashboardHeadingRow(wb, sheet);

        int maxLengthCol0 = 0;
        for (int i = 1; i <= stats.size(); i++) {

            TopicStatistics stat = stats.get(i - 1);
            long totalSuccess = stat.getSuccessCount();
            long totalError = stat.getErrorCount();

//            System.out.println("totalSuccess :  "+totalSuccess +"  "+"  totalError:  "+totalError);

            Row row = sheet.createRow((short) i);

            Cell cellSubjectArea = createCell(row, (short) 0);
            cellSubjectArea.setCellValue(new XSSFRichTextString(stat.getTopic()));
            maxLengthCol0 = getLength(maxLengthCol0, cellSubjectArea);
            CellUtil.setCellStyleProperty(cellSubjectArea, CellUtil.ALIGNMENT, HorizontalAlignment.LEFT);

            Cell cellTotalSuccessCount = createCell(row, (short) 1);
            cellTotalSuccessCount.setCellValue(totalSuccess);

            Cell cellErrorCount = createCell(row, (short) 2);
            cellErrorCount.setCellValue(totalError);
        }
        int width0 = ((int) (30 * 1.14388)) * 256;
        int width1 = ((int) (25 * 1.14388)) * 256;
        int width2 = ((int) (25 * 1.14388)) * 256;
        sheet.setColumnWidth(0, width0);
        sheet.setColumnWidth(1, width1);
        sheet.setColumnWidth(2, width2);
    }

    private Cell createCell(Row row, short i) {
        Cell cell = row.createCell(i);
        CellUtil.setCellStyleProperty(cell, CellUtil.VERTICAL_ALIGNMENT, VerticalAlignment.CENTER);

        if (i == 3 || i == 0) {
            CellUtil.setCellStyleProperty(cell, CellUtil.ALIGNMENT, HorizontalAlignment.LEFT);
        } else {
            CellUtil.setCellStyleProperty(cell, CellUtil.ALIGNMENT, HorizontalAlignment.RIGHT);
        }

        CellUtil.setCellStyleProperty(cell, CellUtil.BORDER_BOTTOM, BorderStyle.THIN);
        CellUtil.setCellStyleProperty(cell, CellUtil.BORDER_TOP, BorderStyle.THIN);
        CellUtil.setCellStyleProperty(cell, CellUtil.BORDER_LEFT, BorderStyle.THIN);
        CellUtil.setCellStyleProperty(cell, CellUtil.BORDER_RIGHT, BorderStyle.THIN);

        return cell;
    }

    private void createErrorHeadingRow(Workbook wb, Sheet sheet) {

        Row row = sheet.createRow((short) 0);

        Cell date = row.createCell((short) 0);
        date.setCellValue(new XSSFRichTextString("Date"));

        Cell dailySuccessCount = row.createCell((short) 1);
        dailySuccessCount.setCellValue(new XSSFRichTextString(ApplicationConstants.STRING_DAILY_SUCCESS_COUNT));

        Cell dailyErrorCount = row.createCell((short) 2);
        dailyErrorCount.setCellValue(new XSSFRichTextString(ApplicationConstants.STRING_DAILY_ERROR_COUNT));

        Cell errorMessage = row.createCell((short) 3);
        errorMessage.setCellValue(new XSSFRichTextString(ApplicationConstants.STRING_ERROR_MESSAGE));

        Cell errorCount = row.createCell((short) 4);
        errorCount.setCellValue(new XSSFRichTextString(ApplicationConstants.STRING_COUNT));

        Map<String, Object> properties = getStyleProperties(wb);

        CellUtil.setCellStyleProperties(date, properties);
        CellUtil.setCellStyleProperties(dailySuccessCount, properties);
        CellUtil.setCellStyleProperties(dailyErrorCount, properties);
        CellUtil.setCellStyleProperties(errorMessage, properties);
        CellUtil.setCellStyleProperties(errorCount, properties);

        CellUtil.setCellStyleProperty(date, CellUtil.ALIGNMENT, HorizontalAlignment.CENTER);
        CellUtil.setCellStyleProperty(dailySuccessCount, CellUtil.ALIGNMENT, HorizontalAlignment.CENTER);
        CellUtil.setCellStyleProperty(dailyErrorCount, CellUtil.ALIGNMENT, HorizontalAlignment.CENTER);
        CellUtil.setCellStyleProperty(errorMessage, CellUtil.ALIGNMENT, HorizontalAlignment.CENTER);
        CellUtil.setCellStyleProperty(errorCount, CellUtil.ALIGNMENT, HorizontalAlignment.CENTER);
    }

    private void createDashboardHeadingRow(Workbook wb, Sheet sheet) {

        Row row = sheet.createRow((short) 0);

        Cell subjectArea = row.createCell((short) 0);
        subjectArea.setCellValue(new XSSFRichTextString(ApplicationConstants.STRING_SUBJECT_AREA));

        Cell totalSuccessCount = row.createCell((short) 1);
        totalSuccessCount.setCellValue(new XSSFRichTextString(ApplicationConstants.STRING_TOTAL_SUCCESS_COUNT));

        Cell totalErrorCount = row.createCell((short) 2);
        totalErrorCount.setCellValue(new XSSFRichTextString(ApplicationConstants.STRING_TOTAL_ERROR_COUNT));

        Map<String, Object> properties = getStyleProperties(wb);

        CellUtil.setCellStyleProperties(subjectArea, properties);
        CellUtil.setCellStyleProperties(totalSuccessCount, properties);
        CellUtil.setCellStyleProperties(totalErrorCount, properties);

        CellUtil.setCellStyleProperty(subjectArea, CellUtil.ALIGNMENT, HorizontalAlignment.CENTER);
        CellUtil.setCellStyleProperty(totalSuccessCount, CellUtil.ALIGNMENT, HorizontalAlignment.CENTER);
        CellUtil.setCellStyleProperty(totalErrorCount, CellUtil.ALIGNMENT, HorizontalAlignment.CENTER);

    }

    private Map<String, Object> getStyleProperties(Workbook wb) {
        Font defaultFontBlack = wb.createFont();
        defaultFontBlack.setFontName("Calibri");
        defaultFontBlack.setFontHeightInPoints((short) 12);
        defaultFontBlack.setBold(true);
        defaultFontBlack.setColor(IndexedColors.BLACK.getIndex());

        Map<String, Object> properties = new HashMap<>();
        properties.put(CellUtil.FILL_PATTERN, FillPatternType.SOLID_FOREGROUND);
        //properties.put(CellUtil.FILL_FOREGROUND_COLOR, IndexedColors.GREY_50_PERCENT.getIndex()); //do using only IndexedColors for fills
        properties.put(CellUtil.FONT, defaultFontBlack.getIndexAsInt()); //since apache poi 4.0.0
        properties.put(CellUtil.FILL_FOREGROUND_COLOR, IndexedColors.LIGHT_ORANGE.getIndex());

        properties.put(CellUtil.BORDER_BOTTOM, BorderStyle.MEDIUM);
        properties.put(CellUtil.BORDER_TOP, BorderStyle.MEDIUM);
        properties.put(CellUtil.BORDER_LEFT, BorderStyle.MEDIUM);
        properties.put(CellUtil.BORDER_RIGHT, BorderStyle.MEDIUM);

        return properties;
    }

    private void writeErrorDictionary(Workbook wb) {
        Sheet sheet = wb.createSheet(ApplicationConstants.STRING_ERROR_DICTIONARY);
        createErrorDictionaryHeading(sheet);
        Resource errorDictionary = resourceLoader.getResource(environment.getProperty(ApplicationConstants.ERROR_DICTIONARY_FILE_PATH));
        try (InputStream inputStream = errorDictionary.getInputStream()){

            List<String> doc =
                    new BufferedReader(new InputStreamReader(inputStream,
                            StandardCharsets.UTF_8)).lines().collect(Collectors.toList());

            for(int i = 1; i <= doc.size(); i++) {
                String line = doc.get((i - 1));
                writeErrors(sheet, line, i);
            }

        } catch (IOException e) {
            LOGGER.info("Error writing error-dictionary in Excel", e);
        }

        int width0 = ((int) (90 * 1.14388)) * 256;
        int width1 = ((int) (80 * 1.14388)) * 256;
        sheet.setColumnWidth(0, width0);
        sheet.setColumnWidth(1, width1);

    }

    private void writeErrors(Sheet sheet, String line, int count) {
        try {
            String[] values = line.split("=");
            Row row = sheet.createRow((short) count);

            Cell errorMessage = createCell(row, (short) 0);
            errorMessage.setCellValue(new XSSFRichTextString(values[0]));
            CellUtil.setCellStyleProperty(errorMessage, CellUtil.ALIGNMENT, HorizontalAlignment.LEFT);

            Cell stepToBeTaken = createCell(row, (short) 1);
            stepToBeTaken.setCellValue(new XSSFRichTextString(values[1]));
            CellUtil.setCellStyleProperty(stepToBeTaken, CellUtil.ALIGNMENT, HorizontalAlignment.LEFT);

            int width0 = ((int) (30 * 1.14388)) * 256;
            int width1 = ((int) (25 * 1.14388)) * 256;
            sheet.setColumnWidth(0, width0);
            sheet.setColumnWidth(1, width1);
        } catch (Exception e) {
            LOGGER.info("Exception while writing error line {}", line, e);
        }
    }

    private void createErrorDictionaryHeading(Sheet sheet) {
        Row row = sheet.createRow((short) 0);

        Cell errorMessageCell = row.createCell((short) 0);
        errorMessageCell.setCellValue(new XSSFRichTextString(ApplicationConstants.STRING_ERROR_MESSAGE));

        Cell stepToBeTaken = row.createCell((short) 1);
        stepToBeTaken.setCellValue(new XSSFRichTextString(ApplicationConstants.STRING_STEPS_TO_BE_TAKEN));

        Map<String, Object> properties = getStyleProperties(sheet.getWorkbook());

        CellUtil.setCellStyleProperties(errorMessageCell, properties);
        CellUtil.setCellStyleProperties(stepToBeTaken, properties);

        CellUtil.setCellStyleProperty(errorMessageCell, CellUtil.ALIGNMENT, HorizontalAlignment.CENTER);
        CellUtil.setCellStyleProperty(stepToBeTaken, CellUtil.ALIGNMENT, HorizontalAlignment.CENTER);
    }

    private int getLength(int oldMax, Object obj) {
        if (oldMax < obj.toString().length()) {
            oldMax = 20;
        }
        return oldMax;
    }

    static class TempSheetData {
        int totalDistinctErrors = 0;
        long dailyErrorCount = 0;
        boolean rowUpdated = false;
        boolean errorCountUpdated = false;
    }

    static class TempFileData {
        TopicStatistics topicStatistics;
        Sheet sheet;
        XSSFSheet sheetToColor;
        int index;
        Row row;
        boolean applyTabColor = false;
        int startRow = 1;
        int startCol = 0;
        int endCol = 0;
    }
}
