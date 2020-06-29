package com.example.uploadingfiles.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Service used for interacting with the storage
 * implements StorageService
 */
@Service
public class FileSystemStorageService implements StorageService {

    private final Path rootLocation;

    /**
     * Define how the Spring dependency injector needs to create the FileSystemStorageService
     * It will inject a StorageProperties instance to the constructor
     *
     * @param properties the StorageProperties that offers access to the application.properties storage values
     */
    @Autowired
    public FileSystemStorageService(StorageProperties properties) {
        this.rootLocation = Path.of(properties.getLocation());
    }

    /**
     * Store a file in the storage.location directory
     *
     * @param file MultipartFile to store
     * @throws StorageException if anythong goes wrong (Empty file, filesystem unable to store...)
     */
    @Override
    public void store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file " + file.getOriginalFilename());
            }
            Files.copy(file.getInputStream(), this.rootLocation.resolve(file.getOriginalFilename()));
        } catch (IOException e) {
            throw new StorageException("Failed to store file " + file.getOriginalFilename(), e);
        }
    }

    /**
     * Get a stream of all the paths in the storage.location directory
     * (except the storage.location itself)
     *
     * @return a Stream<Path> the paths
     * @throws StorageException if the storage.location can't be red
     */
    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.rootLocation, 1)
                    .filter(path -> !path.equals(this.rootLocation))
                    .map(path -> this.rootLocation.relativize(path));
        } catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }

    }

    /**
     * Get the Path for the filename file in the storage.location directory
     *
     * @return Path
     */
    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    /**
     * Get a Resource object for the filename file in the storage.location directory
     *
     * @return Resource the file
     * @throws StorageFileNotFoundException if the file can't be read
     */
    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageFileNotFoundException("Could not read file: " + filename);

            }
        } catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    /**
     * Delete recursively the storage.location directory
     */
    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }

    /**
     * Create the storage.location directory
     *
     * @throws StorageException if the filesystem can't create the directory
     */
    @Override
    public void init() {
        try {
            if (!Files.exists(rootLocation)) {
                Files.createDirectory(rootLocation);
            }
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }
}
