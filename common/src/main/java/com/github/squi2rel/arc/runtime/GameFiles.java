package com.github.squi2rel.arc.runtime;

import arc.Files;
import arc.files.Fi;
import arc.util.ArcRuntimeException;
import arc.util.OS;

import java.io.File;
import java.io.InputStream;

/** Arc file access rooted at the Minecraft instance directory. */
public final class GameFiles implements Files{
    private final File gameDirectory;

    public GameFiles(File gameDirectory){
        this.gameDirectory = gameDirectory.toPath().toAbsolutePath().normalize().toFile();
    }

    @Override
    public Fi get(String path, FileType type){
        return type == FileType.local || type == FileType.internal
            ? new GameFi(new File(path), type) : new Fi(path, type);
    }

    @Override
    public String getLocalStoragePath(){
        return gameDirectory.getPath() + File.separator;
    }

    @Override
    public boolean isLocalStorageAvailable(){
        return true;
    }

    @Override
    public String getExternalStoragePath(){
        return OS.userHome + File.separator;
    }

    @Override
    public boolean isExternalStorageAvailable(){
        return true;
    }

    /** Keeps the physical file rooted while retaining Arc's relative paths and file types. */
    private final class GameFi extends Fi{
        private final File relative;

        GameFi(File relative, FileType type){
            super(new File(gameDirectory, relative.getPath()), type);
            this.relative = relative;
        }

        @Override
        public String path(){
            return relative.getPath().replace('\\', '/');
        }

        @Override
        public String name(){
            return relative.getName();
        }

        @Override
        public String pathWithoutExtension(){
            String path = path();
            int dot = path.lastIndexOf('.');
            return dot == -1 ? path : path.substring(0, dot);
        }

        @Override
        public InputStream read(){
            return file.exists() ? super.read() : new Fi(relative.getPath(), FileType.classpath).read();
        }

        @Override
        public boolean exists(){
            return file.exists() || (type == FileType.internal && new Fi(relative.getPath(), FileType.classpath).exists());
        }

        @Override
        public Fi child(String name){
            return new GameFi(relative.getPath().isEmpty() ? new File(name) : new File(relative, name), type);
        }

        @Override
        public Fi sibling(String name){
            if(relative.getPath().isEmpty()) throw new ArcRuntimeException("Cannot get the sibling of the root.");
            return new GameFi(new File(relative.getParent(), name), type);
        }

        @Override
        public Fi parent(){
            File parent = relative.getParentFile();
            return new GameFi(parent == null ? new File("") : parent, type);
        }

        @Override
        public String toString(){
            return path();
        }
    }
}
