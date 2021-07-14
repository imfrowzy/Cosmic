package tools.mapletools;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author RonanLana
 * <p>
 * This application reports in reactor ids that have drops on the SQL table but are
 * not yet coded.
 */
public class ReactorDropFetcher {
    private static final File OUTPUT_FILE = ToolConstants.getOutputFile("reactor_drop_report.txt");
    private static final String REACTOR_SCRIPT_PATH = ToolConstants.SCRIPTS_PATH + "/reactor";
    private static final Connection con = SimpleDatabaseConnection.getConnection();

    private static PrintWriter printWriter = null;
    private static final Set<Integer> reactors = new HashSet<>();

    private static void printReportFileHeader() {
        printWriter.println(" # Report File autogenerated from the MapleReactorDropFetcher feature by Ronan Lana.");
        printWriter.println(" # Generated data takes into account several data info from the underlying DB and the server-side files.");
        printWriter.println();
    }

    private static int getReactorIdFromFilename(String name) {
        try {
            return Integer.parseInt(name.substring(0, name.indexOf('.')));
        } catch (Exception e) {
            return -1;
        }
    }

    private static void removeScriptedReactorids(String directoryName) {
        File directory = new File(directoryName);

        // get all the files from a directory
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isFile()) {
                reactors.remove(getReactorIdFromFilename(file.getName()));
            }
        }
    }

    private static void loadReactoridsOnDB() throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT DISTINCT reactorid FROM reactordrops;");
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            reactors.add(rs.getInt("reactorid"));
        }

        rs.close();
        ps.close();
    }

    private static List<Integer> getSortedReactorids() {
        List<Integer> sortedReactors = new ArrayList<>(reactors);
        Collections.sort(sortedReactors);

        return sortedReactors;
    }

    private static void fetchMissingReactorDrops() throws SQLException {
        loadReactoridsOnDB();
        removeScriptedReactorids(REACTOR_SCRIPT_PATH);
    }

    private static void reportMissingReactorDrops() throws SQLException {
        if (!reactors.isEmpty()) {
            printWriter.println("MISSING REACTOR DROP SCRIPTS");
            for (Integer reactorid : getSortedReactorids()) {
                printWriter.println("  " + reactorid);
            }
            printWriter.println("\n");
        }
    }

    private static void reportMissingReactors() {
        try {
            System.out.println("Fetching reactors from DB...");
            fetchMissingReactorDrops();

            con.close();
            printWriter = new PrintWriter(OUTPUT_FILE, StandardCharsets.UTF_8);

            // report suspects of missing quest drop data, as well as those drop data that may have incorrect questids.
            System.out.println("Reporting results...");
            printReportFileHeader();
            reportMissingReactorDrops();

            printWriter.close();
            System.out.println("Done!");
        } catch (SQLException e) {
            System.out.println("Warning: Could not establish connection to database to report quest data.");
            System.out.println(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        reportMissingReactors();
    }
}