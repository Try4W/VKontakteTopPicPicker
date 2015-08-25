package org.flycraft.vktoppicpicker.donwloader;

import org.flycraft.vktoppicpicker.utils.Dialogs;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VkDownloader extends Thread {

    public enum State {
        LOADING_COMMENTS,
        DOWNLOADING,
        DONE,
        CANCELED
    }

    private final String vkApiVersion = "5.35";

    private final Path workingDirPath = Paths.get("./TopPicData/");
    private Path postDirPath;

    private String wallUrl;
    private String ownerId;
    private String postId;

    private int likesBorder;

    private final HashMap<Path, String> picturesToDownload = new HashMap<>();

    private int downloadedFilesCounter;

    private boolean isWorking = true;

    private VkDownloaderListener listener;

    public VkDownloader(String wallUrl, int likesBorder) {
        this.wallUrl = wallUrl;
        this.likesBorder = likesBorder;
    }

    @Override
    public void run() {
        try {
            System.out.println("VkDownloader started");
            ownerId = wallUrl.substring(wallUrl.indexOf("w=wall") + 6, wallUrl.indexOf("_"));
            postId = wallUrl.substring(wallUrl.indexOf("_") + 1);
            System.out.println("Owner id: " + ownerId);
            System.out.println("Wall id: " + postId);
            if (!Files.exists(workingDirPath)) Files.createDirectory(workingDirPath); // Create work dir if it's needed
            postDirPath = Paths.get(workingDirPath.toString(), postId);
            if(Files.exists(postDirPath)) {
                if(Dialogs.createAskDialog("Внимание", "Вы уже пытались сохранить этот диалог. Удалить старые данные?")) {
                    removePostDir();
                } else {
                    listener.onStateChange(State.CANCELED);
                    return;
                }
            }
            Files.createDirectory(postDirPath);

            listener.onStateChange(State.LOADING_COMMENTS);
            loadComments();

            listener.onDownloadStarted(picturesToDownload.size());
            listener.onStateChange(State.DOWNLOADING);
            for(Map.Entry<Path, String> entry : picturesToDownload.entrySet()) {
                if(!isWorking) return;
                Path localFilePath = entry.getKey();
                String fileAddress = entry.getValue();
                downloadFile(localFilePath, fileAddress);
                downloadedFilesCounter++;
                listener.onNewFileDownloaded(downloadedFilesCounter, localFilePath.getFileName().toString());
            }
            listener.onStateChange(State.DONE);
        } catch (IOException e) {
            e.printStackTrace();
            listener.onException(e);
        }
    }

    public void kill() {
        isWorking = false;
    }

    public void setListener(VkDownloaderListener listener) {
        this.listener = listener;
    }

    public String getWallUrl() {
        return wallUrl;
    }

    private void loadComments() {
        //picturesToDownload.put(Paths.get(postDirPath.toString(), "18-496694-0"), "https://pp.vk.me/c622530/v622530580/34f66/ZPy2m6Iq0lc.jpg");
        //picturesToDownload.put(Paths.get(postDirPath.toString(), "18-496694-1"), "https://pp.vk.me/c622530/v622530413/392e3/R69-NyIlZQo.jpg");
        JSONParser jsonParser = new JSONParser();
        String request = "wall.getComments?owner_id=" + ownerId + "&post_id=" + postId + "&count=" + 100 + "&offset=" + 0 + "&need_likes=" + 1 + "&v=" + vkApiVersion; // initial request
        String jsonResponse = callVkMethod(request);
        System.out.println("First request: " + request);
        System.out.println("First json response: " + jsonResponse);
        try {
            JSONObject response = (JSONObject) ((JSONObject) jsonParser.parse(jsonResponse)).get("response");
            long totalCommentsCount = (long) response.get("count");
            System.out.println("Comments count: " + totalCommentsCount);
            addCommentToMap(response);
            long totalCommentsToRequest = totalCommentsCount - 100; // -100 because first 100 comments were requested in initial request
            System.out.println("Comments to request: " + totalCommentsToRequest);
            long commentsToRequest;
            while(true) { // Request all other comments from post
                if(totalCommentsToRequest > 100) commentsToRequest = 100; else commentsToRequest = totalCommentsToRequest;
                if(totalCommentsToRequest < 0) break;
                System.out.println("Messages for request: " + commentsToRequest);
                jsonResponse = callVkMethod("wall.getComments?owner_id=" + ownerId + "&post_id=" + postId + "&count=" + 100 + "&offset=" + totalCommentsToRequest + "&need_likes=" + 1 + "&v=" + vkApiVersion);
                response = (JSONObject) ((JSONObject) jsonParser.parse(jsonResponse)).get("response");
                addCommentToMap(response);
                totalCommentsToRequest -= 100;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void addCommentToMap(JSONObject getCommentsResponse) {
        JSONArray commentsArray = (JSONArray) getCommentsResponse.get("items");
        for(Object rawComment : commentsArray) {
            JSONObject comment = (JSONObject) rawComment;
            JSONObject likesData = (JSONObject) comment.get("likes");
            JSONArray attachments = (JSONArray) comment.get("attachments");
            long likesCount = (long) likesData.get("count");
            if(attachments != null && likesCount >= likesBorder) { // attachments if comment doesn't have attachments
                long commentId = (long) comment.get("id");
                ArrayList<String> photosUrlArray = new ArrayList<>();
                boolean hasPhotos = false;
                for (Object rawAttachment : attachments) {
                    JSONObject attachment = (JSONObject) rawAttachment;
                    if (attachment.containsKey("photo")) {
                        hasPhotos = true;
                        JSONObject photo = (JSONObject) attachment.get("photo");
                        int bestResolution = 0;
                        for (Object rawPhotoProperty : photo.keySet()) {
                            String photoProperty = (String) rawPhotoProperty;
                            if (photoProperty.contains("photo_")) {
                                int currentResolution = Integer.parseInt(photoProperty.substring(photoProperty.indexOf("_") + 1));
                                if (currentResolution > bestResolution) bestResolution = currentResolution;
                            }
                        }
                        photosUrlArray.add((String) photo.get("photo_" + bestResolution));
                    }
                }
                if(!hasPhotos) continue;
                for (int i = 0; i < photosUrlArray.size(); i++) {
                    String photoUrl = photosUrlArray.get(i);
                    String photoFileName = likesCount + "-" + commentId + "-" + i + ".jpg";
                    picturesToDownload.put(Paths.get(postDirPath.toString(), photoFileName), photoUrl);
                }
            }
        }
    }

    private String callVkMethod(String methodWithArgs) {
        String request = "https://api.vk.com/method/" + methodWithArgs;
        try {
            URL url = new URL(request);
            URLConnection con = url.openConnection();
            BufferedReader inReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            return inReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void downloadFile(Path filePath, String fileAddress) throws IOException {
        Files.createFile(filePath);
        URL fileUrl = new URL(fileAddress);
        ReadableByteChannel readableByteChannel = Channels.newChannel(fileUrl.openStream());
        FileOutputStream fileOutputStream = new FileOutputStream(filePath.toString());
        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        fileOutputStream.close();
    }

    private void removePostDir() throws IOException {
        Files.walkFileTree(postDirPath, new FileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                exc.printStackTrace();
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
