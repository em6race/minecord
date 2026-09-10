package com.example.minecord.utils;

import java.io.InputStream;
import java.util.Properties;

public class GitVersion {

    private static String commitHash = "unknown";
    private static String commitMessage = "development build";
    private static String buildTime = "unknown";
    private static String diffFiles = "0";
    private static String diffInsertions = "0";
    private static String diffDeletions = "0";
    private static boolean loaded = false;

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;

        try (InputStream is = GitVersion.class.getResourceAsStream("/git.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);

                String hash = props.getProperty("git.commit.hash");
                String msg = props.getProperty("git.commit.message");
                String time = props.getProperty("git.build.time");
                String files = props.getProperty("git.diff.files");
                String ins = props.getProperty("git.diff.insertions");
                String del = props.getProperty("git.diff.deletions");

                if (hash != null && !hash.startsWith("${")) commitHash = hash.trim();
                if (msg != null && !msg.startsWith("${")) commitMessage = msg.trim();
                if (time != null && !time.startsWith("${")) buildTime = time.trim();
                if (files != null && !files.startsWith("${")) diffFiles = files.trim();
                if (ins != null && !ins.startsWith("${")) diffInsertions = ins.trim();
                if (del != null && !del.startsWith("${")) diffDeletions = del.trim();
            }
        } catch (Throwable ignored) {}
    }

    public static String getCommitHash() {
        load();
        return commitHash;
    }

    public static String getCommitMessage() {
        load();
        return commitMessage;
    }

    public static String getBuildTime() {
        load();
        return buildTime;
    }

    public static String getDiffFiles() {
        load();
        return diffFiles;
    }

    public static String getDiffInsertions() {
        load();
        return diffInsertions;
    }

    public static String getDiffDeletions() {
        load();
        return diffDeletions;
    }
}
