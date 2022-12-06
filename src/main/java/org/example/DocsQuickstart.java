package org.example;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.DocsScopes;
import com.google.api.services.docs.v1.model.*;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.auth.Credentials;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.common.base.Preconditions;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DocsQuickstart {
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(DocsScopes.DOCUMENTS, DocsScopes.DRIVE, DocsScopes.DRIVE_FILE);
    private static final String CREDENTIALS_PROJECT_FILE_PATH = "/project.json";

    //
    private static Credentials getCredentialsFromServiceAccount() throws IOException {
        InputStream in = DocsQuickstart.class.getResourceAsStream(CREDENTIALS_PROJECT_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_PROJECT_FILE_PATH);
        }
        return ServiceAccountCredentials.fromStream(in).createScoped(SCOPES);
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {
        String domainName = System.getenv("DOMAIN_NAME");
        Preconditions.checkNotNull(domainName, "'DOMAIN_NAME' env is required");

        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(getCredentialsFromServiceAccount());

        Drive driveService = new Drive.Builder(httpTransport, JSON_FACTORY, requestInitializer)
                .setApplicationName("Drive samples")
                .build();

        driveService
                .files()
                .list()
                .execute()
                .getFiles()
                .stream()
//                .map(File::getName)
                .limit(3)
//                .forEach(System.out::println)
        ;

        Docs docsService = new Docs.Builder(httpTransport, JSON_FACTORY, requestInitializer)
                .setApplicationName("Google Docs API Java Quickstart ")
                .build();

        Document documentToBeCreated = new Document()
                .setTitle("___TEST___REMOVE___shared doc " + LocalDateTime.now())
                .setBody(new Body());

        Document newDoc = docsService.documents().create(documentToBeCreated).execute();
        String documentId = newDoc.getDocumentId();

        BatchUpdateDocumentRequest batchUpdateDocumentRequest = new BatchUpdateDocumentRequest()
                .setRequests(Collections.singletonList(new Request()
                        .setInsertText(
                                new InsertTextRequest()
                                        .setEndOfSegmentLocation(new EndOfSegmentLocation())
                                        .setText("from app service account")
                        )));
        BatchUpdateDocumentResponse batchUpdateDocumentResponse = docsService.documents().batchUpdate(documentId, batchUpdateDocumentRequest).execute();

        // share file
        Permission userPermission = new Permission()
                .setType("domain")
                .setRole("writer")
                .setAllowFileDiscovery(true)
                .setDomain(domainName);

        Permission userPermissionExecuted = driveService
                .permissions()
                .create(documentId, userPermission)
                .setFields("id")
                .execute();

        System.out.println(userPermissionExecuted);
        System.out.println("https://docs.google.com/document/d/" + documentId + "/edit");
    }

}
