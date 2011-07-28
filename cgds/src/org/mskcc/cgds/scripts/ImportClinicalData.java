package org.mskcc.cgds.scripts;

import org.mskcc.cgds.dao.DaoClinicalData;
import org.mskcc.cgds.dao.DaoException;
import org.mskcc.cgds.util.ConsoleUtil;
import org.mskcc.cgds.util.FileUtil;
import org.mskcc.cgds.util.ProgressMonitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

/**
 * Imports Clinical Data.
 *
 * @author Ethan Cerami.
 */
public class ImportClinicalData {
    private File clinicalDataFile;
    private ProgressMonitor pMonitor;
    private int caseIdCol = -1;
    private int osMonthsCol = -1;
    private int osStatusCol = -1;
    private int dfsMonthCol = -1;
    private int dfsStatusCol = -1;
    private int ageCol = -1;

    /**
     * Constructor.
     *
     * @param clinicalDataFile File
     * @param pMonitor         ProgressMonitor
     */
    public ImportClinicalData(File clinicalDataFile, ProgressMonitor pMonitor) {
        this.pMonitor = pMonitor;
        this.clinicalDataFile = clinicalDataFile;
    }

    /**
     * Method to import data.
     *
     * @throws java.io.IOException
     * @throws org.mskcc.cgds.dao.DaoException
     *
     * @throws IllegalArgumentException
     */
    public void importData() throws IOException, DaoException, IllegalArgumentException {
        // create reader
        FileReader reader = new FileReader(clinicalDataFile);
        BufferedReader buf = new BufferedReader(reader);

        DaoClinicalData daoClinical = new DaoClinicalData();

        // skip column headings
        String colHeadingLine = buf.readLine();

        //  Figure Out Which Column Number Contains the Data we Want
        extractHeaders(colHeadingLine);
        String[] parts;

        //  At a minimum, we must have Case ID, OS Survival and OS Status
        if (caseIdCol > -1 && osStatusCol > -1 && osMonthsCol > -1) {
            // start reading data
            String line = buf.readLine();
            while (line != null) {
                if (pMonitor != null) {
                    pMonitor.incrementCurValue();
                    ConsoleUtil.showProgress(pMonitor);
                }
                if (!line.startsWith("#") && line.trim().length() > 0) {
                    parts = line.split("\t");
                    String caseId = parts[caseIdCol];
                    Double osMonths = getDouble(parts, osMonthsCol);
                    String osStatus = getString(parts, osStatusCol);
                    Double dfsMonths = getDouble(parts, dfsMonthCol);
                    String dfsStatus = getString(parts, dfsStatusCol);
                    Double ageAtDiagnosis = getDouble(parts, ageCol);
                    daoClinical.addCase(caseId, osMonths, osStatus, dfsMonths, dfsStatus,
                            ageAtDiagnosis);
                }
                line = buf.readLine();
            }
        } else {
            pMonitor.logWarning("Could not parse clinical file.");
            pMonitor.logWarning("Appropriate columns could not be identified.  " +
                    "Check the file and try again");
            throw new IOException ("Could not parse clinical file.");
        }
    }

    private void extractHeaders(String colHeadingLine) {
        HashSet<String> caseIdNames = new HashSet<String>();
        caseIdNames.add("BCRPATIENTBARCODE");
        caseIdNames.add("CASE_ID");

        HashSet<String> osStatusNames = new HashSet<String>();
        osStatusNames.add("VITALSTATUS");
        osStatusNames.add("OS_STATUS");

        HashSet<String> osMonthsNames = new HashSet<String>();
        osMonthsNames.add("OverallSurvival(mos)");
        osMonthsNames.add("OS_MONTHS");

        HashSet<String> dfsMonthsNames = new HashSet<String>();
        dfsMonthsNames.add("ProgressionFreeSurvival (mos)#");
        dfsMonthsNames.add("DFS_MONTHS");

        HashSet<String> dfsStatusNames = new HashSet<String>();
        dfsStatusNames.add("ProgressionFreeStatus");
        dfsStatusNames.add("DFS_STATUS");

        HashSet<String> ageNames = new HashSet<String>();
        ageNames.add("AgeAtDiagnosis (yrs)");

        String[] parts = colHeadingLine.split("\t");
        for (int i = 0; i < parts.length; i++) {
            String header = parts[i];
            if (caseIdNames.contains(header)) {
                caseIdCol = i;
            }
            if (osStatusNames.contains(header)) {
                osStatusCol = i;
            }
            if (osMonthsNames.contains(header)) {
                osMonthsCol = i;
            }
            if (dfsMonthsNames.contains(header)) {
                dfsMonthCol = i;
            }
            if (dfsStatusNames.contains(header)) {
                dfsStatusCol = i;
            }
            if (ageNames.contains(header)) {
                ageCol = i;
            }
        }
    }

    private Double getDouble(String parts[], int index) {
        if (index < 0) {
            return null;
        } else {
            String value = parts[index];
            if (value == null) {
                return null;
            } else if (value.trim().length() == 0) {
                return null;
            } else if (value.equalsIgnoreCase("NA")) {
                return null;
            } else {
                try {
                    Double dValue = Double.parseDouble(value);
                    return dValue;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
    }

    private String getString(String parts[], int index) {
        if (index < 0) {
            return null;
        } else {
            String value = parts[index];
            if (value == null) {
                return null;
            } else if (value.trim().length() == 0) {
                return null;
            } else if (value.equalsIgnoreCase("NA")) {
                return null;
            } else if (value.equalsIgnoreCase("missing")) {
                return null;
            } else {
                return value;
            }
        }
    }

    /**
     * The big deal main.
     */
    public static void main(String[] args) {

        // check args
        if (args.length < 1) {
            System.out.println("command line usage:  importSurvivalData.pl <data_file.txt>");
            System.exit(1);
        }

        File dataFile = new File(args[0]);
        ProgressMonitor pMonitor = new ProgressMonitor();

        try {
            System.err.println("Reading data from:  " + dataFile.getAbsolutePath());
            int numLines = FileUtil.getNumLines(dataFile);
            System.err.println(" --> total number of lines:  " + numLines);
            pMonitor.setMaxValue(numLines);

            ImportClinicalData importClinicalData = new ImportClinicalData(dataFile, pMonitor);
            importClinicalData.importData();
            System.err.println("Success!");
        } catch (IOException e) {
            System.err.println ("Error:  " + e.getMessage());
        } catch (DaoException e) {
            System.err.println ("Error:  " + e.getMessage());
        } finally {
            ConsoleUtil.showWarnings(pMonitor);
        }
    }
}