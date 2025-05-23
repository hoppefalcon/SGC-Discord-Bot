package sgc.discord.infographics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Update;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchClearValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import sgc.bungie.api.processor.activity.SGC_Member;
import sgc.types.SGC_Clan;

/* Class to demonstrate use-case of drive's download file. */
public class GoogleDriveUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleDriveUtil.class);
    private static final String APPLICATION_NAME = "sgc-discord-bot";
    private static final List<String> SCOPES = Arrays
            .asList(new String[] { SheetsScopes.SPREADSHEETS, DriveScopes.DRIVE_FILE, DriveScopes.DRIVE });
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /**
     * Download a Document file in PDF format.
     *
     * @param realFileId file ID of any workspace document format file.
     * @return byte array stream if successful, {@code null} otherwise.
     * @throws IOException if service account credentials file not found.
     */
    public static ByteArrayOutputStream downloadFile(String realFileId) throws IOException {
        /*
         * Load pre-authorized user credentials from the environment.
         * TODO(developer) - See https://developers.google.com/identity for
         * guides on implementing OAuth2 for your application.
         */
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(Arrays.asList(DriveScopes.DRIVE_FILE));
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(
                credentials);

        // Build a new authorized API client service.
        Drive service = new Drive.Builder(new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer)
                .setApplicationName("Drive samples")
                .build();

        try {
            OutputStream outputStream = new ByteArrayOutputStream();

            service.files().get(realFileId)
                    .executeMediaAndDownloadTo(outputStream);
            return (ByteArrayOutputStream) outputStream;
        } catch (GoogleJsonResponseException e) {
            // TODO(developer) - handle error appropriately
            System.err.println("Unable to move file: " + e.getDetails());
            throw e;
        }
    }

    /**
     * Initiates the authentication for Google Sheets API.
     */
    public static void initiateGoogleSheetsAuth() {
        try {
            LOGGER.info("Initiating Google Sheet Auth");
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                    getGoogleSheetsHttpRequestInitializer())
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            final String spreadsheetId = "1R0RkQYKVWcy6DA71xNkoDU-XIc14UjXuD7M05H2VHPk";
            service.spreadsheets().values().get(spreadsheetId, "SOL!A2:F").execute();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Returns the HTTP request initializer for Google Sheets API authentication.
     *
     * @return The HTTP request initializer.
     * @throws IOException If there is an error reading the credentials file.
     */
    private static HttpRequestInitializer getGoogleSheetsHttpRequestInitializer() throws IOException {
        // Load service account secrets.

        final ServiceAccountCredentials serviceAccountCredentials = ServiceAccountCredentials.newBuilder()
                .setProjectId(System.getenv("GOOGLE_API_PROJECT_ID"))
                .setPrivateKeyId(System.getenv("GOOGLE_API_PRIVATE_KEY_ID"))
                .setPrivateKeyString(new String(Base64.getDecoder().decode(System.getenv("GOOGLE_API_PRIVATE_KEY"))))
                .setClientEmail(System.getenv("GOOGLE_API_CLIENT_EMAIL"))
                .setClientId(System.getenv("GOOGLE_API_CLIENT_ID"))
                .setTokenServerUri(URI.create(System.getenv("GOOGLE_API_TOKEN_URI")))
                .setUniverseDomain(System.getenv("GOOGLE_API_UNIVERSE_DOMAIN"))
                .build();

        GoogleCredentials credentials = serviceAccountCredentials.createScoped(SCOPES);

        return new HttpCredentialsAdapter(credentials);
    }

    /**
     * Writes the activity data to the Google Sheets.
     *
     * @param members A HashMap containing clans as keys and their members as
     *                values.
     */
    public static void writeActivityToGoogleSheet(HashMap<SGC_Clan, ArrayList<SGC_Member>> members) {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            final String spreadsheetId = "1R0RkQYKVWcy6DA71xNkoDU-XIc14UjXuD7M05H2VHPk";
            Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getGoogleSheetsHttpRequestInitializer())
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            BatchClearValuesRequest batchClearValuesRequest = new BatchClearValuesRequest();
            List<String> ranges = new ArrayList<>();
            List<Update> updates = new ArrayList<>();

            for (SGC_Clan clan : members.keySet()) {
                final String range = String.format("%s!A2:F", clan);
                ranges.add(range);

                ValueRange valueRange = new ValueRange();
                valueRange.setRange(range);

                List<List<Object>> values = new ArrayList<>();
                for (SGC_Member member : members.get(clan)) {
                    List<Object> row = new ArrayList<>();
                    row.add(member.getDiscordDisplayName());
                    row.add(member.getBungieDisplayName());
                    row.add(member.isDiscordActivity());
                    row.add(member.isGameActivity());
                    row.add(member.getDiscordMessageCounts().get("TOTAL"));
                    row.add(member.getDiscordUserName());
                    values.add(row);
                }

                valueRange.setValues(values);
                Update update = service.spreadsheets().values().update(spreadsheetId, range, valueRange);
                update.setValueInputOption("USER_ENTERED");
                updates.add(update);
            }

            batchClearValuesRequest.setRanges(ranges);
            service.spreadsheets().values().batchClear(spreadsheetId, batchClearValuesRequest).execute();

            for (Update update : updates) {
                update.execute();
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static Map<String, Integer> getPOTWWeights() {
        HashMap<String, Integer> weights = new HashMap<>();
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            final String spreadsheetId = "1EeIFBxfwDHjTrsZUFHKtNUglH7whdiLBTfupcOsqEF8";
            final String range = "Weighting Scale!A2:B";
            Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getGoogleSheetsHttpRequestInitializer())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            ValueRange response = service.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();
            for (List<Object> row : values) {
                weights.put(((String) row.get(0)).replace(" [POTW]", ""),
                        Integer.parseInt((String) row.get(1)));
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return weights;
    }

    public static byte[] getInfographic(String folderID) throws GeneralSecurityException, IOException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getGoogleSheetsHttpRequestInitializer())
                .setApplicationName(APPLICATION_NAME)
                .build();
        // Print the names and IDs for up to 10 files.

        FileList result = service.files().list()
                .setQ("'" + folderID + "' in parents and trashed=false")
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name)")
                .execute();
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            return null;
        } else {
            return downloadFileContent(service, files.get(0));
        }
    }

    private static byte[] downloadFileContent(Drive service, File file)
            throws IOException {
        try {
            OutputStream outputStream = new ByteArrayOutputStream();

            service.files().get(file.getId())
                    .executeMediaAndDownloadTo(outputStream);

            return ((ByteArrayOutputStream) outputStream).toByteArray();
        } catch (GoogleJsonResponseException e) {
            // TODO(developer) - handle error appropriately
            System.err.println("Unable to move file: " + e.getDetails());
            throw e;
        }

    }

    public static byte[] getSGCLogo() throws GeneralSecurityException, IOException {
        return getInfographic("1I1tX2JMMNzGMFormF7aQCogXaqVducL7");
    }

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        getPOTWWeights();
    }

    public static Map<String, String> getClanRoleIDs() {
        HashMap<String, String> clans = new HashMap<>();
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            final String spreadsheetId = "1bcW1yT-j_RxlQLGvHWOqisW-xFDPvRaOjR2cByIQHu8";
            final String range = "Clans!A2:B";
            Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getGoogleSheetsHttpRequestInitializer())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            ValueRange response = service.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();
            for (List<Object> row : values) {
                clans.put((String) row.get(0),
                        (String) row.get(1));
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return clans;
    }

    public static String getNotRegisteredRoleID() {
        String roleID = "";
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            final String spreadsheetId = "1bcW1yT-j_RxlQLGvHWOqisW-xFDPvRaOjR2cByIQHu8";
            final String range = "Misc!A2:B";
            Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getGoogleSheetsHttpRequestInitializer())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            ValueRange response = service.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();
            for (List<Object> row : values) {
                if (((String) row.get(0)).equals("Not Registered")) {
                    roleID = (String) row.get(1);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return roleID;
    }

    public static Map<String, Integer> getCrucubleMapWeights() {
        HashMap<String, Integer> weights = new HashMap<>();
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            final String spreadsheetId = "1vGH6LXObJBMigCbXAT86hpBF5hiZj-fJDzoXdBEGljg";
            final String range = "Maps!A2:B";
            Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getGoogleSheetsHttpRequestInitializer())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            ValueRange response = service.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();
            for (List<Object> row : values) {
                weights.put((String) row.get(0),
                        Integer.parseInt((String) row.get(1)));
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return weights;
    }

    public static Map<String, Integer> getCrucubleModeWeights() {
        HashMap<String, Integer> weights = new HashMap<>();
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            final String spreadsheetId = "1vGH6LXObJBMigCbXAT86hpBF5hiZj-fJDzoXdBEGljg";
            final String range = "Modes!A2:B";
            Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getGoogleSheetsHttpRequestInitializer())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            ValueRange response = service.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();
            for (List<Object> row : values) {
                weights.put((String) row.get(0),
                        Integer.parseInt((String) row.get(1)));
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return weights;
    }

    public static Map<String, Integer> getGambitMapWeights() {
        HashMap<String, Integer> weights = new HashMap<>();
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            final String spreadsheetId = "1WEKNB0kY5DmkuP4PDzQC3t3D17t9iPjn5tr1W_fD8v0";
            final String range = "Maps!A2:C";
            Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getGoogleSheetsHttpRequestInitializer())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            ValueRange response = service.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();
            for (List<Object> row : values) {
                weights.put((String) row.get(0),
                        Integer.parseInt((String) row.get(2)));
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return weights;
    }

    public static Map<String, Integer> getGambitMapCombatantsWithWeights(String map) {
        HashMap<String, Integer> weights = new HashMap<>();
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            final String spreadsheetId = "1WEKNB0kY5DmkuP4PDzQC3t3D17t9iPjn5tr1W_fD8v0";
            final String range = "Maps!A2:C";
            Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getGoogleSheetsHttpRequestInitializer())
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            ValueRange response = service.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();
            List<String> combatantUIDs = new ArrayList<>();
            for (List<Object> row : values) {
                if (map.equals((String) row.get(0))) {
                    // combatantUIDs = Arrays.asList(((String) row.get(1)).split(","));
                    for (String str : ((String) row.get(1)).split(",")) {
                        combatantUIDs.add(str.trim());
                    }
                }
            }
            if (combatantUIDs != null) {
                final String range2 = "Combatants!A2:C";
                response = service.spreadsheets().values()
                        .get(spreadsheetId, range2)
                        .execute();
                values = response.getValues();

                for (List<Object> row : values) {
                    if (combatantUIDs.contains((String) row.get(0))) {
                        weights.put((String) row.get(1),
                                Integer.parseInt((String) row.get(2)));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return weights;
    }
}