package com.github.squi2rel.arc.runtime;

import arc.Files.FileType;
import arc.files.Fi;
import arc.util.ArcRuntimeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GameFilesTest{
    @TempDir Path gameDirectory;

    @Test
    void localAndCacheUseGameDirectoryInsteadOfWorkingDirectory() throws Exception{
        GameFiles files = new GameFiles(gameDirectory.toFile());
        assertNotEquals(Path.of("").toAbsolutePath(), gameDirectory);
        Fi local = files.local("config/example.txt");
        local.writeString("saved", false);

        assertEquals("saved", Files.readString(gameDirectory.resolve("config/example.txt")));
        assertEquals("config/example.txt", local.path());
        assertEquals(FileType.local, local.type());
        assertEquals(gameDirectory.resolve("config/example.txt").toFile(), local.file());
        assertEquals(local.file(), new java.io.File(local.absolutePath()));
        assertEquals(gameDirectory.toFile(), files.local("").file());
        assertEquals(local, files.local("").child("config").child("example.txt"));
        assertEquals(files.local("config"), local.parent());
        assertEquals(files.local("config/other.txt"), local.sibling("other.txt"));

        files.cache("example.bin").writeBytes(new byte[]{1, 2}, false);
        assertArrayEquals(new byte[]{1, 2}, Files.readAllBytes(gameDirectory.resolve("cache/example.bin")));
        assertEquals(gameDirectory.resolve("cache").toFile(), new java.io.File(files.getCachePath()));
    }

    @Test
    void listingCopyingMappingAndDeletingKeepUsingGameDirectory() throws Exception{
        GameFiles files = new GameFiles(gameDirectory.toFile());
        Fi source = files.local("data/source.txt");
        Fi target = source.sibling("target.txt");
        source.writeString("contents", false);
        target.writeString("old", false);
        source.copyTo(target);

        assertEquals("contents", Files.readString(gameDirectory.resolve("data/target.txt")));
        assertEquals('c', (char)source.map().get());
        assertEquals(2, source.parent().list().length);
        assertEquals(2, source.parent().list((java.io.FileFilter)file -> file.isFile()).length);
        assertEquals(source, source.parent().list((dir, name) -> name.equals("source.txt"))[0]);
        assertEquals("contents", source.parent().list("source.txt")[0].readString());
        target.moveTo(target.sibling("moved.txt"));
        assertFalse(target.exists());
        assertTrue(files.local("data/moved.txt").exists());
        assertTrue(source.parent().deleteDirectory());
        assertFalse(Files.exists(gameDirectory.resolve("data")));
    }

    @Test
    void internalFilesUseGameDirectoryAndRetainClasspathFallback() throws Exception{
        GameFiles files = new GameFiles(gameDirectory.toFile());
        Files.createDirectories(gameDirectory.resolve("assets"));
        Files.writeString(gameDirectory.resolve("assets/example.txt"), "asset");
        Fi internal = files.internal("assets").child("example.txt");

        assertEquals(FileType.internal, internal.type());
        assertTrue(internal.exists());
        assertEquals("asset", internal.readString());
        assertEquals(5, internal.length());
        assertThrows(ArcRuntimeException.class, () -> internal.writeString("overwrite", false));

        String resourcePath = "com/github/squi2rel/arc/script/base.js";
        Fi resource = files.internal("com/github/squi2rel/arc/script").child("base.js");
        assertTrue(resource.exists());
        assertEquals(files.classpath(resourcePath).readString(), resource.readString());
        assertTrue(resource.length() > 0);
        assertEquals(resource.readString(), files.local(resourcePath).readString());
    }

    @Test
    void fileHandlesRetainTheirOriginalInstanceDirectory() throws Exception{
        Path otherDirectory = Files.createDirectory(gameDirectory.resolve("other"));
        Fi first = new GameFiles(gameDirectory.toFile()).local("example.txt");
        Fi second = new GameFiles(otherDirectory.toFile()).local("example.txt");
        first.writeString("first", false);
        second.writeString("second", false);

        assertEquals("first", Files.readString(gameDirectory.resolve("example.txt")));
        assertEquals("second", Files.readString(otherDirectory.resolve("example.txt")));
        assertEquals("first", first.parent().child("example.txt").readString());
    }
}
